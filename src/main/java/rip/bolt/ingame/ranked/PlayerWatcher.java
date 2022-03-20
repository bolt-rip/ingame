package rip.bolt.ingame.ranked;

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
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.result.TieVictoryCondition;

public class PlayerWatcher implements Listener {

  public static final Duration ABSENT_MAX = Duration.ofSeconds(AppData.absentSecondsLimit());

  private final RankedManager rankedManager;
  private final ForfeitManager forfeitManager;
  private final CancelManager cancelManager;

  private final Map<UUID, MatchParticipation> players = new HashMap<>();
  private boolean automaticallyCancelled;

  public PlayerWatcher(RankedManager rankedManager) {
    this.rankedManager = rankedManager;
    this.forfeitManager = new ForfeitManager(this);
    this.cancelManager = new CancelManager(this);
  }

  public ForfeitManager getForfeitManager() {
    return forfeitManager;
  }

  public void addPlayers(List<UUID> uuids) {
    automaticallyCancelled = false;
    players.clear();
    forfeitManager.clearPolls();
    cancelManager.clearCountdown();
    uuids.forEach(uuid -> players.put(uuid, new MatchParticipation(uuid)));
  }

  public MatchParticipation getParticipation(UUID uuid) {
    return players.get(uuid);
  }

  public Map<UUID, MatchParticipation> getParticipations() {
    return players;
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
    if (!isPlaying(player.getId())) return;

    MatchParticipation participation = players.get(player.getId());
    participation.playerJoined();

    if (!event.getMatch().isRunning()) return;

    forfeitManager.updateCountdown(event.getNewParty());
    cancelManager.playerJoined(event.getPlayer());
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
    cancelManager.playerLeft(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (rankedManager.isManuallyCanceled()) return;

    // If a regular match end and duration less than max absent period no bans to check
    if (!automaticallyCancelled && event.getMatch().getDuration().compareTo(ABSENT_MAX) > 0) return;

    Duration maxAbsenceDuration =
        (automaticallyCancelled) ? CancelManager.CANCEL_ABSENCE_LENGTH : ABSENT_MAX;

    List<UUID> abandonedPlayers =
        players.values().stream()
            .filter(
                participation -> participation.absentDuration().compareTo(maxAbsenceDuration) > 0)
            .map(MatchParticipation::getUUID)
            .collect(Collectors.toList());

    if (playersAbandoned(abandonedPlayers)) {
      event.getMatch().sendMessage(Messages.participationBan());
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

    // No players are currently missing
    if (getOfflinePlayers(event.getMatch()).isEmpty()) return;

    // If a player never joined mark as abandoned
    if (playersAbandoned(getNonJoinedPlayers())) {
      cancelMatch(event.getMatch());
      event.getMatch().sendMessage(Messages.matchStartCancelled());
    }

    // Set everyone who is not online as "absent"
    players.forEach(
        (uuid, matchParticipation) -> {
          if (event.getMatch().getPlayer(uuid) == null) matchParticipation.playerLeft();
        });

    cancelManager.startCountdownIfRequired(event.getMatch());
  }

  public void cancelMatch(Match match) {
    automaticallyCancelled = true;
    // the order of these two lines should not be changed
    rankedManager.postMatchStatus(match, MatchStatus.CANCELLED);
    match.addVictoryCondition(new TieVictoryCondition());
    match.finish();
  }

  public List<UUID> getNonJoinedPlayers() {
    return players.values().stream()
        .filter(participation -> !participation.joined)
        .map(matchParticipation -> matchParticipation.uuid)
        .collect(Collectors.toList());
  }

  public List<UUID> getOfflinePlayers(Match match) {
    return players.keySet().stream()
        .filter(absence -> match.getPlayer(absence) == null)
        .collect(Collectors.toList());
  }

  private boolean playersAbandoned(List<UUID> players) {
    if (players.size() <= 5) {
      Integer seriesId = Ingame.get().getRankedManager().getMatch().getSeries().getId();
      players.forEach(player -> playerAbandoned(player, seriesId));
    }

    return players.size() > 0;
  }

  private void playerAbandoned(UUID player, Integer seriesId) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(),
            () ->
                Ingame.get()
                    .getApiManager()
                    .postPlayerPunishment(new Punishment(player, seriesId)));
  }

  public boolean isPlaying(UUID uuid) {
    return players.containsKey(uuid);
  }

  public static class MatchParticipation {

    final UUID uuid;
    boolean joined = false;
    long absentLength = 0;
    Long playerLeftAt = null;

    public MatchParticipation(UUID uuid) {
      this.uuid = uuid;
    }

    public UUID getUUID() {
      return uuid;
    }

    public void playerJoined() {
      joined = true;
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

    public boolean hasJoined() {
      return joined;
    }

    public boolean hasLeft() {
      return playerLeftAt != null;
    }

    public Duration currentAbsentDuration() {
      // Not joined or not left...
      if (!hasJoined() || !hasLeft()) return Duration.ZERO;

      return Duration.ofMillis(System.currentTimeMillis() - playerLeftAt);
    }
  }
}
