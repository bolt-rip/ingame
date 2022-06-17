package rip.bolt.ingame.ranked;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchResult;
import rip.bolt.ingame.api.definitions.Participation;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.api.definitions.User;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class RankManager implements Listener {

  private static final Component RANK_PREFIX = text("Bolt rank: ");
  private static final Component PLACEMENT_MATCHES = text("Placement matches: ");
  private static final Component ARROW = text("➔ ", NamedTextColor.WHITE);
  private static final Component MATCH_SEPARATOR = text("-", NamedTextColor.DARK_GRAY);

  private static final LegacyComponentSerializer SERIALIZER =
      LegacyComponentSerializer.legacySection();

  private final RankedManager manager;
  private final OnlinePlayerMapAdapter<PermissionAttachment> permissions;

  public RankManager(RankedManager manager) {
    this.manager = manager;
    this.permissions = new OnlinePlayerMapAdapter<>(Ingame.get());
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

  public void handleMatchUpdate(@Nonnull BoltMatch oldMatch, @Nonnull BoltMatch newMatch) {
    MatchManager matchManager = PGM.get().getMatchManager();

    List<RankUpdate> updates =
        newMatch.getTeams().stream()
            .map(Team::getParticipations)
            .flatMap(Collection::stream)
            .map(Participation::getUser)
            .map(
                user ->
                    new RankUpdate(
                        oldMatch.getUser(user.getUuid()),
                        user,
                        matchManager.getPlayer(user.getUuid())))
            .filter(RankUpdate::isValid)
            .collect(Collectors.toList());

    if (updates.isEmpty()) return;

    Match match = updates.get(0).player.getMatch();
    Map<UUID, PlayerStats> stats = new HashMap<>();
    Bukkit.getServer().getPluginManager().callEvent(new MatchStatsEvent(match, true, true, stats));

    if (AppData.Web.getMatch() != null) {
      match.sendMessage(Messages.matchLink(newMatch));
      match.sendMessage(empty());
    }

    updates.forEach(update -> notifyUpdate(update.old, update.updated, update.player));
  }

  public void notifyUpdate(@Nonnull User old, @Nonnull User user, @Nonnull MatchPlayer player) {
    updatePlayer(player, player.getParty());

    Component rank = RANK_PREFIX;
    if (!old.getRanking().getRank().equals(user.getRanking().getRank()))
      rank = rank.append(getRank(old)).append(ARROW);
    rank = rank.append(getRank(user)).append(text("("));

    if (old.getRanking().getRating() != null
        && old.getRanking().getRating() < user.getRanking().getRating())
      rank = rank.append(mmr(old)).append(text(" ")).append(ARROW);
    rank = rank.append(mmr(user)).append(text(")"));

    player.sendMessage(rank);

    if (user.getHistory() != null) {
      List<MatchResult> results = new ArrayList<>(user.getHistory());
      Collections.reverse(results);

      while ((results.size() % 15) != 0) results.add(MatchResult.UNKNOWN);

      player.sendMessage(PLACEMENT_MATCHES);
      for (int i = 0; i < results.size(); )
        player.sendMessage(Component.join(MATCH_SEPARATOR, results.subList(i, i += 15)));
    }
  }

  private static class RankUpdate {
    private final User old, updated;
    private final MatchPlayer player;

    public RankUpdate(User old, User updated, MatchPlayer player) {
      this.old = old;
      this.updated = updated;
      this.player = player;
    }

    public boolean isValid() {
      return old != null && updated != null && player != null;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    updatePlayer(event.getPlayer(), event.getNewParty());
  }

  private Component getRank(User user) {
    String rank = user.getRank();
    return PGM.get().getConfiguration().getGroups().stream()
        .filter(g -> g.getId().equals(rank))
        .map(c -> c.getFlair().getComponent(true))
        .findFirst()
        .orElse(text("Unknown ", NamedTextColor.DARK_GRAY));
  }

  private TextColor getRankColor(User user) {
    // Flairs/ranks are plaintext with §1 color codes, this is hacky but it normalizes
    Component rankComp = SERIALIZER.deserialize(SERIALIZER.serialize(getRank(user)));
    if (rankComp.color() != null) return rankComp.color();
    for (Component child : rankComp.children()) {
      if (child.color() != null) return child.color();
    }
    return NamedTextColor.WHITE; // Fallback to white
  }

  private Component mmr(User user) {
    if (user.getRanking().getRating() == null) return text(" - ", NamedTextColor.DARK_GRAY);
    return text(user.getRanking().getRating(), getRankColor(user));
  }
}
