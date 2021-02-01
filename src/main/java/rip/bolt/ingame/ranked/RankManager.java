package rip.bolt.ingame.ranked;

import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.User;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
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
    Iterator<Match> matches = PGM.get().getMatchManager().getMatches();
    while (matches.hasNext()) {
      Match match = matches.next();
      match.getPlayers().forEach(p -> updatePlayer(p, p.getParty()));
    }
  }

  public void updatePlayer(@Nonnull MatchPlayer mp, @Nullable Party party) {
    Player player = mp.getBukkit();
    PermissionAttachment perm = permissions.get(player);
    BoltMatch match = manager.getMatch();
    User user = match == null ? null : match.getUser(mp.getId());

    if ((perm != null) == (user != null && party instanceof Competitor)) return;

    if (perm != null) {
      mp.getBukkit().removeAttachment(perm);
      permissions.remove(mp.getBukkit());
    } else {
      permissions.put(
          player, player.addAttachment(Ingame.get(), "pgm.group." + user.getRank(), true));
    }
    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(mp.getId()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerPartyChangeEvent e) {
    updatePlayer(e.getPlayer(), e.getNewParty());
  }
}
