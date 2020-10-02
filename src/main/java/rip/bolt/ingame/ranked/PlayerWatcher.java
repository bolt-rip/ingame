package rip.bolt.ingame.ranked;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Tournament;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerWatcher implements Listener {

    private static final Duration ABSENT_MAX = Duration.ofSeconds(AppData.absentSecondsLimit());

    final Map<UUID, Duration> absentLengths = new HashMap<>();
    final Map<UUID, Duration> playerLeftAt = new HashMap<>();

    public void addPlayers(List<UUID> uuids) {
        absentLengths.clear();
        playerLeftAt.clear();

        uuids.forEach(uuid -> this.absentLengths.put(uuid, Duration.ZERO));
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
        if (!event.getMatch().isRunning() && !this.isPlaying(player)) {
            return;
        }

        this.playerLeftAt.put(player.getId(), getDurationNow());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchEnd(MatchFinishEvent event) {
        if (event.getMatch().getDuration().compareTo(ABSENT_MAX) > 0) {
            this.absentLengths.forEach((key, value) -> updateAbsenceLengths(key));

            Collection<Map.Entry<UUID, Duration>> absentPlayers = this.absentLengths.entrySet()
                    .stream()
                    .filter(absence -> absence.getValue().compareTo(ABSENT_MAX) > 0)
                    .collect(Collectors.toList());

            absentPlayers.forEach(absence -> playerAbandoned(absence.getKey()));

            if (absentPlayers.size() > 0) {
                event.getMatch().sendMessage(ChatColor.GRAY + "A player was a temporarily banned due to lack of participation. "
                        + "As the match was unbalanced it will take less of an effect on player scores.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchStart(MatchStartEvent event) {
        List<UUID> absentPlayers = this.absentLengths.keySet().
                stream().
                filter(absence -> event.getMatch().getPlayer(absence) == null).collect(Collectors.toList());

        absentPlayers.forEach(this::playerAbandoned);

        if (absentPlayers.size() > 0) {
            event.getMatch().finish();
            event.getMatch().sendMessage(ChatColor.RED + "Match could not be started due to lack of players.");
            event.getMatch().sendMessage(ChatColor.GRAY + "The offending players have received a temporary ban.");
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

    private void playerAbandoned(UUID player) {
        Tournament.get().getApiManager().postMatchPlayerAbandon(player);
    }

    private boolean isPlaying(MatchPlayer player) {
        return this.absentLengths.containsKey(player.getId());
    }

    private Duration getDurationNow() {
        return Duration.ofMillis(Instant.now().toEpochMilli());
    }

}
