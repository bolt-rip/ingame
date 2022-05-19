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
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public class PlayerWatcher implements Listener {

  public static final Duration ABSENT_MAX = Duration.ofSeconds(AppData.absentSecondsLimit());

  private final RankedManager rankedManager;
  private final ForfeitManager forfeitManager;
  private final CancelManager cancelManager;

  private final Map<UUID, MatchParticipation> players = new HashMap<>();

  public PlayerWatcher(RankedManager rankedManager) {
    this.rankedManager = rankedManager;
    this.forfeitManager = new ForfeitManager(this);
    this.cancelManager = new CancelManager(this);
  }

  public RankedManager getRankedManager() {
    return rankedManager;
  }

  public ForfeitManager getForfeitManager() {
    return forfeitManager;
  }

  public void addPlayers(List<UUID> uuids) {
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
    // If match was cancelled, don't bother with the rest
    if (rankedManager.getCancelReason() != null) return;

    // Duration less than max absent period no bans to check
    if (event.getMatch().getDuration().compareTo(ABSENT_MAX) > 0) return;

    if (playersAbandoned(getParticipationsBelowDuration(ABSENT_MAX))) {
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
    List<UUID> offlinePlayers = getOfflinePlayers(event.getMatch());
    if (offlinePlayers.isEmpty()) return;

    // If a player never joined mark as abandoned
    if (playersAbandoned(getNonJoinedPlayers())) {
      rankedManager.cancel(event.getMatch(), CancelReason.AUTOMATED_CANCEL);
      event.getMatch().sendMessage(Messages.matchStartCancelled());
    }

    // Set everyone who is not online as "absent"
    players.values().stream()
        .filter(participation -> offlinePlayers.contains(participation.getUUID()))
        .forEach(MatchParticipation::playerLeft);

    cancelManager.startCountdown(event.getMatch(), offlinePlayers, null);
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

  private List<UUID> getParticipationsBelowDuration(Duration minimumDuration) {
    return players.values().stream()
        .filter(participation -> participation.absentDuration().compareTo(minimumDuration) >= 0)
        .map(MatchParticipation::getUUID)
        .collect(Collectors.toList());
  }

  public boolean playersAbandoned(List<UUID> players) {
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
