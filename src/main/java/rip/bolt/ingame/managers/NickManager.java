package rip.bolt.ingame.managers;

import dev.pgm.community.Community;
import dev.pgm.community.nick.feature.NickFeature;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

public class NickManager implements Listener {

  public NickManager() {}

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerJoinMatch(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();

    if (!(event.getNewParty() instanceof Competitor)) return;
    if (Ingame.get().getMatchManager().getMatch() == null) return;
    if (Integration.getNick(player.getBukkit()) == null) return;

    UUID playerId = player.getId();

    NickFeature nick = Community.get().getFeatures().getNick();

    nick.removeOnlineNick(playerId);

    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(playerId));

    // Delay so message appears after Community nick message
    event
        .getMatch()
        .getExecutor(MatchScope.LOADED)
        .schedule(() -> player.sendMessage(Messages.nickRemoved()), 2, TimeUnit.SECONDS);
  }
}
