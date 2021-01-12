package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import dev.pgm.events.Tournament;
import java.time.Duration;
import java.time.Instant;
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
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;

public class PlayerWatcher implements Listener {

  private final RankedManager rankedManager;

  private static final Duration ABSENT_MAX = Duration.ofSeconds(AppData.absentSecondsLimit());

  final Map<UUID, Duration> absentLengths = new HashMap<>();
  final Map<UUID, Duration> playerLeftAt = new HashMap<>();

  public PlayerWatcher(RankedManager rankedManager) {
    this.rankedManager = rankedManager;
  }

  public void addPlayers(List<UUID> uuids) {
    absentLengths.clear();
    playerLeftAt.clear();

    uuids.forEach(uuid -> this.absentLengths.put(uuid, Duration.ZERO));
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerLogin(final PlayerLoginEvent event) {
    if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL
        || event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
      // allow if player is on a team
      if (this.absentLengths.containsKey(event.getPlayer().getUniqueId())) {
        event.allow();
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    if (!event.getMatch().isRunning() && !this.isPlaying(player)) {
      return;
    }

    updateAbsenceLengths(player.getId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLeave(PlayerLeaveMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    if (!this.isPlaying(player) || !event.getMatch().isRunning()) {
      return;
    }

    this.playerLeftAt.put(player.getId(), getDurationNow());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (event.getMatch().getDuration().compareTo(ABSENT_MAX) > 0) {
      this.absentLengths.forEach((key, value) -> updateAbsenceLengths(key));

      List<UUID> absentPlayers =
          this.absentLengths.entrySet().stream()
              .filter(absence -> absence.getValue().compareTo(ABSENT_MAX) > 0)
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      playersAbandoned(absentPlayers);

      if (absentPlayers.size() > 0) {
        rankedManager.getMatch().invalidate();
        event
            .getMatch()
            .sendMessage(
                text(
                    "A player was a temporarily banned due to lack of participation. "
                        + "As the match was unbalanced it will take less of an effect on player scores.",
                    NamedTextColor.GRAY));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    List<UUID> absentPlayers =
        this.absentLengths.keySet().stream()
            .filter(absence -> event.getMatch().getPlayer(absence) == null)
            .collect(Collectors.toList());

    playersAbandoned(absentPlayers);

    if (absentPlayers.size() > 0) {
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
  }

  private void updateAbsenceLengths(UUID player) {
    if (this.playerLeftAt.containsKey(player)) {
      Duration leftAt = this.playerLeftAt.get(player);

      Duration totalAbsentLength = this.absentLengths.get(player);
      Duration absentLength = getDurationNow().minus(leftAt).plus(totalAbsentLength);

      this.playerLeftAt.remove(player);
      this.absentLengths.put(player, absentLength);
    }
  }

  private void playersAbandoned(List<UUID> players) {
    if (players.size() <= 5) {
      players.forEach(player -> playerAbandoned(player, absentLengths.get(player)));
    }
  }

  private void playerAbandoned(UUID player, Duration duration) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Tournament.get(),
            () -> Ingame.get().getApiManager().postMatchPlayerAbandon(player, duration));
  }

  private boolean isPlaying(MatchPlayer player) {
    return this.absentLengths.containsKey(player.getId());
  }

  private Duration getDurationNow() {
    return Duration.ofMillis(Instant.now().toEpochMilli());
  }
}
