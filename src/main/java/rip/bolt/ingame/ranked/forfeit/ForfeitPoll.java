package rip.bolt.ingame.ranked.forfeit;

import static net.kyori.adventure.text.Component.text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.teams.Team;

public class ForfeitPoll {

  private final Competitor team;
  private final Set<UUID> voted = new HashSet<>();

  public ForfeitPoll(Competitor team) {
    this.team = team;
  }

  public Set<UUID> getVoted() {
    return voted;
  }

  public void addVote(MatchPlayer player) {
    voted.add(player.getId());
    check();
  }

  public void check() {
    if (hasPassed()) endMatch();
  }

  private boolean hasPassed() {
    return voted.stream().filter(uuid -> team.getPlayer(uuid) != null).count()
        >= Math.min(
            team instanceof Team ? ((Team) team).getMaxPlayers() - 1 : 0, team.getPlayers().size());
  }

  public void endMatch() {
    Match match = team.getMatch();
    match.sendMessage(
        team.getName().append(text(" has voted to forfeit the match.", NamedTextColor.WHITE)));

    // Create victory condition for other team or tie
    VictoryCondition victoryCondition =
        match.getCompetitors().stream()
            .filter(competitor -> !competitor.equals(team))
            .findFirst()
            .<VictoryCondition>map(CompetitorVictoryCondition::new)
            .orElseGet(TieVictoryCondition::new);

    match.addVictoryCondition(victoryCondition);
    match.finish(null);
  }
}
