package rip.bolt.ingame.ranked;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.nick.feature.NickFeature;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.ranked.forfeit.PlayerWatcher;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

public class NickManager implements Listener {

  private final PlayerWatcher watcher;

  public NickManager(PlayerWatcher playerWatcher) {
    this.watcher = playerWatcher;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerJoinMatch(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();

    UUID playerId = player.getId();
    if (!watcher.isPlaying(playerId)) return;
    if (Ingame.get().getMatchManager().getMatch() == null) return;
    if (Integration.getNick(player.getBukkit()) == null) return;

    NickFeature nick = Community.get().getFeatures().getNick();

    nick.removeOnlineNick(playerId);

    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(playerId));

    // Delay so message appears after Community nick message
    event
        .getMatch()
        .getExecutor(MatchScope.LOADED)
        .schedule(
            () -> player.sendMessage(text(
                "Your nick was removed as you are playing in this match.", NamedTextColor.RED)),
            2,
            TimeUnit.SECONDS);
  }
}
