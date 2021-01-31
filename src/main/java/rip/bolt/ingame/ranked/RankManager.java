package rip.bolt.ingame.ranked;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.User;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class RankManager implements Listener {

  private final RankedManager manager;
  private final OnlinePlayerMapAdapter<PermissionAttachment> permissions;

  public RankManager(RankedManager manager) {
    this.manager = manager;
    this.permissions = new OnlinePlayerMapAdapter<>(Ingame.get());
  }

  public void updateAll() {
    Bukkit.getOnlinePlayers().forEach(this::updatePlayer);
  }

  public void updatePlayer(Player player) {
    PermissionAttachment perm = permissions.get(player);

    User user = manager.getMatch().getUser(player.getUniqueId());
    MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);

    if ((perm != null) == (user != null && mp != null && mp.getCompetitor() != null)) return;

    if (perm != null) {
      player.removeAttachment(perm);
      permissions.remove(player);
    } else {
      permissions.put(
          player, player.addAttachment(Ingame.get(), "pgm.group." + user.getRank(), true));
    }
    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(player.getUniqueId()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerPartyChangeEvent e) {
    updatePlayer(e.getPlayer().getBukkit());
  }
}
