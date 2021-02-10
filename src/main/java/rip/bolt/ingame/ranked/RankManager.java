package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import rip.bolt.ingame.api.definitions.MatchResult;
import rip.bolt.ingame.api.definitions.User;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.TextColor;
import tc.oc.pgm.lib.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class RankManager implements Listener {

  private static final Component RANK_PREFIX = text("Rank: ");
  private static final Component MMR_PREFIX = text("MMR: ");
  private static final Component PLACEMENT_MATCHES = text("Placement matches: ");
  private static final Component ARROW = text(" ➔ ", NamedTextColor.WHITE);
  private static final Component MATCH_SEPARATOR = text("-", NamedTextColor.DARK_GRAY);

  private static final LegacyComponentSerializer SERIALIZER =
      LegacyComponentSerializer.legacySection();

  private final RankedManager manager;
  private final OnlinePlayerMapAdapter<PermissionAttachment> permissions;

  public RankManager(RankedManager manager) {
    this.manager = manager;
    this.permissions = new OnlinePlayerMapAdapter<>(Ingame.get());
  }

  public void updatePlayer(@Nonnull MatchPlayer mp) {
    updatePlayer(mp, mp.getParty());
  }

  public void updatePlayer(@Nonnull MatchPlayer mp, @Nullable Party party) {
    Player player = mp.getBukkit();
    PermissionAttachment perm = permissions.remove(player);
    BoltMatch match = manager.getMatch();
    User user = match == null ? null : match.getUser(mp.getId());

    if (perm != null) mp.getBukkit().removeAttachment(perm);
    if (user != null && party instanceof Competitor)
      permissions.put(
          player, player.addAttachment(Ingame.get(), "pgm.group." + user.getRank(), true));

    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(mp.getId()));
  }

  public MatchPlayer notifyUpdates(User user) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(user.getUuid());

    User old = manager.getMatch().getUser(user.getUuid());
    if (old == null || player == null) return player;

    player.sendMessage(
        !old.getRank().equals(user.getRank())
            ? RANK_PREFIX.append(getRank(old)).append(ARROW).append(getRank(user))
            : RANK_PREFIX.append(getRank(user)));

    player.sendMessage(
        old.getMmr() != null && old.getMmr() < user.getMmr()
            ? MMR_PREFIX.append(mmr(old)).append(ARROW).append(mmr(user))
            : MMR_PREFIX.append(mmr(user)));

    if (user.getHistory() != null) {
      List<MatchResult> results = new ArrayList<>(user.getHistory());
      Collections.reverse(results);

      while ((results.size() % 15) != 0) results.add(MatchResult.UNKNOWN);

      player.sendMessage(PLACEMENT_MATCHES);
      for (int i = 0; i < results.size(); )
        player.sendMessage(Component.join(MATCH_SEPARATOR, results.subList(i, i += 15)));
    }

    return player;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerPartyChangeEvent e) {
    updatePlayer(e.getPlayer(), e.getNewParty());
  }

  private Component getRank(User user) {
    String rank = user.getRank();
    return PGM.get().getConfiguration().getGroups().stream()
        .filter(g -> g.getId().equals(rank))
        .map(c -> c.getFlair().getComponent(true))
        .findFirst()
        .orElse(text("Unknown", NamedTextColor.DARK_GRAY));
  }

  private TextColor getRankColor(User user) {
    // Flairs/ranks are plaintext with §1 color codes, this is hacky but it normalizes
    Component rankComp = SERIALIZER.deserialize(SERIALIZER.serialize(getRank(user)));
    if (rankComp.color() != null) return rankComp.color();
    for (Component child : rankComp.children()) {
      if (child.color() != null) return rankComp.color();
    }
    return NamedTextColor.WHITE; // Fallback to white
  }

  private Component mmr(User user) {
    if (user.getMmr() == null) return text(" - ", NamedTextColor.DARK_GRAY);
    return text(user.getMmr(), getRankColor(user));
  }
}
