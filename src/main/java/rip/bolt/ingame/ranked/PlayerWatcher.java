package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.Punishment;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.result.TieVictoryCondition;

public class PlayerWatcher implements Listener {

  public static final Duration ABSENT_MAX = Duration.ofSeconds(AppData.absentSecondsLimit());

  private final RankedManager rankedManager;
  private final ForfeitManager forfeitManager;
  final Map<UUID, MatchParticipation> players = new HashMap<>();

  public PlayerWatcher(RankedManager rankedManager) {
    this.rankedManager = rankedManager;
    this.forfeitManager = new ForfeitManager(this);
  }

  public ForfeitManager getForfeitManager() {
    return forfeitManager;
  }

  public void addPlayers(List<UUID> uuids) {
    players.clear();
    forfeitManager.clearPolls();
    uuids.forEach(uuid -> players.put(uuid, new MatchParticipation(uuid)));
  }

  public MatchParticipation getParticipation(UUID uuid) {
    return players.get(uuid);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerLogin(final PlayerLoginEvent event) {
    if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL
        || event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
      // allow if player is on a team
      if (isPlaying(event.getPlayer().getUniqueId())) {
        event.allow();
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    if (!isPlaying(player.getId()) || !event.getMatch().isRunning()) {
      return;
    }

    MatchParticipation participation = players.get(player.getId());
    participation.playerJoined();
    forfeitManager.updateCountdown(event.getNewParty());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLeave(PlayerPartyChangeEvent event) {
    // player changing to null party is leaving
    if (event.getNewParty() != null) return;
    if (!(event.getOldParty() instanceof Competitor)) return;

    MatchPlayer player = event.getPlayer();
    if (!isPlaying(player.getId()) || !event.getMatch().isRunning()) {
      return;
    }

    MatchParticipation participation = players.get(player.getId());
    participation.playerLeft();
    forfeitManager.updateCountdown(event.getOldParty());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (rankedManager.isManuallyCanceled()) return;

    if (event.getMatch().getDuration().compareTo(ABSENT_MAX) > 0) {
      List<UUID> abandonedPlayers =
          players.entrySet().stream()
              .filter(player -> player.getValue().hasAbandoned())
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      if (!playersAbandoned(abandonedPlayers)) return;

      event
          .getMatch()
          .sendMessage(
              text(
                  "Player(s) temporarily banned due to lack of participation.",
                  NamedTextColor.GRAY));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEndMonitor(MatchFinishEvent event) {
    players.clear();
    forfeitManager.clearPolls();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMatchStart(MatchStartEvent event) {
    if (!AppData.fullTeamsRequired()) return;
    if (!playersAbandoned(getMissingPlayers(event.getMatch()))) return;

    // the order of these two lines should not be changed
    rankedManager.postMatchStatus(event.getMatch(), MatchStatus.CANCELLED);
    event.getMatch().addVictoryCondition(new TieVictoryCondition());
    event.getMatch().finish();

    event
        .getMatch()
        .sendMessage(
            text("Match could not be started due to lack of players.", NamedTextColor.RED));
    event
        .getMatch()
        .sendMessage(
            text("The offending players have received a temporary ban.", NamedTextColor.GRAY));
  }

  public List<UUID> getMissingPlayers(Match match) {
    return players.keySet().stream()
        .filter(absence -> match.getPlayer(absence) == null)
        .collect(Collectors.toList());
  }

  private boolean playersAbandoned(List<UUID> players) {
    if (players.size() <= 5) {
      players.forEach(this::playerAbandoned);
    }

    return players.size() > 0;
  }

  private void playerAbandoned(UUID player) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(),
            () -> Ingame.get().getApiManager().postPlayerPunishment(new Punishment(player)));
  }

  public boolean isPlaying(UUID uuid) {
    return players.containsKey(uuid);
  }

  public static class MatchParticipation {

    final UUID uuid;
    long absentLength = 0;
    Long playerLeftAt = null;

    public MatchParticipation(UUID uuid) {
      this.uuid = uuid;
    }

    public void playerJoined() {
      if (playerLeftAt != null) absentLength += System.currentTimeMillis() - playerLeftAt;
      playerLeftAt = null;
    }

    public void playerLeft() {
      playerLeftAt = System.currentTimeMillis();
    }

    public boolean canStartCountdown() {
      return playerLeftAt != null;
    }

    public boolean hasAbandoned() {
      return absentDuration().compareTo(ABSENT_MAX) > 0;
    }

    public Duration absentDuration() {
      return Duration.ofMillis(
          absentLength + (playerLeftAt == null ? 0 : System.currentTimeMillis() - playerLeftAt));
    }
  }
}
