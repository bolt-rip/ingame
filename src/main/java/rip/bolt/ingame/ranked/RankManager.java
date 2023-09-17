package rip.bolt.ingame.ranked;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.Participation;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.api.definitions.User;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.events.BoltMatchResponseEvent;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class RankManager implements Listener {

  private static final Component RANK_PREFIX = text("Bolt rank: ");
  private static final Component PLACEMENT_MATCHES = text("Placement matches: ");
  private static final Component ARROW = text("➔ ", NamedTextColor.WHITE);
  private static final JoinConfiguration MATCH_SEPARATOR =
      JoinConfiguration.separator(text("-", NamedTextColor.DARK_GRAY));

  private static final LegacyComponentSerializer SERIALIZER =
      LegacyComponentSerializer.legacySection();
  private static final OnlinePlayerMapAdapter<PermissionAttachment> PERMISSIONS =
      new OnlinePlayerMapAdapter<>(Ingame.get());

  private final MatchManager manager;

  public RankManager(MatchManager manager) {
    this.manager = manager;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onBoltMatchResponse(BoltMatchResponseEvent event) {
    if (event.hasMatchFinished() && event.getResponseMatch().getStatus() == MatchStatus.ENDED) {
      handleMatchUpdate(event.getBoltMatch(), event.getResponseMatch(), event.getPgmMatch());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    updatePlayer(event.getPlayer(), event.getNewParty());
  }

  public void handleMatchUpdate(
      @Nonnull BoltMatch oldMatch, @Nonnull BoltMatch newMatch, Match match) {
    tc.oc.pgm.api.match.MatchManager matchManager = PGM.get().getMatchManager();

    List<RankUpdate> updates =
        newMatch.getTeams().stream()
            .map(Team::getParticipations)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .map(
                participation ->
                    new RankUpdate(
                        oldMatch.getUser(participation.getUser().getUuid()),
                        participation,
                        matchManager.getPlayer(participation.getUser().getUuid())))
            .filter(RankUpdate::isValid)
            .collect(Collectors.toList());

    match.callEvent(new MatchStatsEvent(match, true, true));

    if (AppData.Web.getMatchLink() != null) {
      match.sendMessage(Messages.matchLink(newMatch));
      match.sendMessage(empty());
    }

    updates.forEach(update -> notifyUpdate(update.old, update.updated, update.player));
  }

  public void notifyUpdate(
      @Nonnull User old, @Nonnull Participation participation, @Nonnull MatchPlayer player) {
    updatePlayer(player, player.getParty());
    User user = participation.getUser();

    Component rank = RANK_PREFIX;

    rank = rank.append(getRankChange(old, user));
    rank = rank.append(getRatingChange(old, user, participation));

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

  public void updatePlayer(@Nonnull MatchPlayer mp, @Nullable Party party) {
    Player player = mp.getBukkit();
    PermissionAttachment perm = PERMISSIONS.remove(player);
    BoltMatch match = manager.getMatch();
    User user = match == null ? null : match.getUser(mp.getId());

    if (perm != null) mp.getBukkit().removeAttachment(perm);
    if (user != null && user.getRank() != null && party instanceof Competitor)
      PERMISSIONS.put(
          player, player.addAttachment(Ingame.get(), "pgm.group." + user.getRank(), true));

    Bukkit.getPluginManager().callEvent(new NameDecorationChangeEvent(mp.getId()));
  }

  private static class RankUpdate {
    private final User old;
    private final Participation updated;
    private final MatchPlayer player;

    public RankUpdate(User old, Participation updated, MatchPlayer player) {
      this.old = old;
      this.updated = updated;
      this.player = player;
    }

    public boolean isValid() {
      return old != null && updated != null && player != null;
    }
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

  private Component getRankChange(User old, User user) {
    TextComponent.Builder text = text();

    if (!old.getRanking().getRank().equals(user.getRanking().getRank())) {
      text.append(getRank(old)).append(ARROW);
    }

    text.append(getRank(user));

    return text.build();
  }

  private Component getRatingChange(User old, User user, Participation participation) {
    Integer oldRating = old.getRanking().getRating();
    Integer newRating = user.getRanking().getRating();

    boolean hasRatings = (oldRating != null && newRating != null);

    int deafenPenalty =
        participation.getDeafenPenalty() != null ? Math.abs(participation.getDeafenPenalty()) : 0;
    TextComponent.Builder text = text();

    text.append(text("("));

    if (hasRatings && oldRating < newRating) {
      text.append(mmr(old)).append(text(" ")).append(ARROW);
    }

    if (newRating != null && deafenPenalty != 0) {
      text.append(text(newRating + deafenPenalty, Style.style(TextDecoration.STRIKETHROUGH)))
          .append(text(" "));
    }

    text.append(mmr(user)).append(text(") "));

    if (deafenPenalty != 0) {
      text.append(
          text("-" + deafenPenalty + " deafen penalty", NamedTextColor.RED)
              .hoverEvent(
                  showText(
                      text(
                          "Significant amount of time deafened in voice chat",
                          NamedTextColor.GRAY))));
    }

    return text.build();
  }
}
