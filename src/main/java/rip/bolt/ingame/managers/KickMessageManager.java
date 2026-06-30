package rip.bolt.ingame.managers;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.util.Audience;

public class KickMessageManager implements Listener {

  private static final LegacyComponentSerializer SERIALIZER =
      LegacyComponentSerializer.legacySection();

  private final MatchManager matchManager;

  public KickMessageManager(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerKick(PlayerKickEvent event) {
    BoltMatch match = matchManager.getMatch();
    if (match == null || match.getStatus().isFinished() || match.getTeams() == null) return;
    if (match.getParticipation(event.getPlayer().getUniqueId()) == null) return;

    Audience.get(event.getPlayer())
        .sendMessage(Messages.withSeparators(Messages.rejoinServer(AppData.getServerName())));
  }
}
