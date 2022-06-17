package rip.bolt.ingame.ranked;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

public class SpectatorManager implements Listener {

  private static final String SPECTATE_VANISH = "events.spectate.vanish";
  private static final String PGM_VANISH = "pgm.vanish";
  private final PlayerWatcher watcher;

  private final HashMap<UUID, PermissionAttachment> permissions = new HashMap<>();

  public SpectatorManager(PlayerWatcher playerWatcher) {
    this.watcher = playerWatcher;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBoltMatchStateChange(BoltMatchStatusChangeEvent event) {
    if (Objects.equals(event.getNewStatus(), MatchStatus.CREATED)) {
      Bukkit.getOnlinePlayers().forEach(this::updatePlayer);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerJoinEvent(PlayerJoinMatchEvent event) {
    updatePlayer(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    PermissionAttachment attachment = permissions.remove(event.getPlayer().getUniqueId());
    if (attachment != null) attachment.remove();
  }

  private void updatePlayer(Player player) {
    if (Ingame.get().getRankedManager().getMatch() == null) return;

    boolean hidden = Ingame.get().getRankedManager().getMatch().getSeries().getHideObservers();
    boolean playing = watcher.isPlaying(player.getUniqueId());

    // Allow people who can vanish to remain in current state
    if (!playing && player.hasPermission(PGM_VANISH)) return;

    if (!hidden || playing) {
      unvanish(player);
    } else {
      vanish(player);
    }
  }

  private void vanish(Player player) {
    permissions.computeIfAbsent(
        player.getUniqueId(), uuid -> player.addAttachment(Ingame.get(), SPECTATE_VANISH, true));

    // Set vanish if not already
    if (Integration.isVanished(player)) return;

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    Integration.setVanished(matchPlayer, true, true);
  }

  private void unvanish(Player player) {
    PermissionAttachment attachment = permissions.remove(player.getUniqueId());
    if (attachment != null) attachment.remove();

    // Remove vanish if not already
    if (!Integration.isVanished(player)) return;

    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    Integration.setVanished(matchPlayer, false, true);
  }
}
