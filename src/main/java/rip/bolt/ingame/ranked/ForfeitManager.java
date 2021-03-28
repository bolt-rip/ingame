package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import dev.pgm.events.Tournament;
import dev.pgm.events.team.TournamentTeamManager;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.teams.Team;

public class ForfeitManager {

  private final Map<Competitor, ForfeitCheck> checks = new HashMap<>();

  public ForfeitManager() {}

  @Nullable
  public ForfeitCheck getChecker(Competitor team) {
    return checks.get(team);
  }

  public void clearCheckers() {
    checks.clear();
  }

  public void startCountdown(Competitor team, PlayerWatcher.MatchParticipation participation) {
    if (!AppData.allowForfeit()) return;
    ForfeitCheck existingCheck = checks.get(team);

    if (existingCheck != null) {
      // Do not continue if existing check is in voting stage
      if (existingCheck.isVotable()) {
        existingCheck.check();
        return;
      }

      // Do nothing if new check is longer than currently running one
      if (existingCheck.scheduledFuture.getDelay(TimeUnit.SECONDS)
          < participation.absentDuration().getSeconds()) return;

      existingCheck.stopTimer();
    }

    checks.put(team, new ForfeitCheck(team, participation));
  }

  public void stopCountdown(
      PlayerWatcher.MatchParticipation participation,
      Map<UUID, PlayerWatcher.MatchParticipation> players) {
    checks.values().stream()
        .filter(check -> check.participation.equals(participation) && !check.isVotable())
        .peek(ForfeitCheck::stopTimer)
        .findFirst()
        .map(ForfeitCheck::getTeam)
        .ifPresent(checks::remove);

    // Check if another item needs starting
    TournamentTeamManager teamManager = Tournament.get().getTeamManager();
    teamManager
        .tournamentTeamPlayer(participation.uuid)
        .map(team -> team.getPlayers().stream())
        .orElse(Stream.empty())
        .map(tournamentPlayer -> players.get(tournamentPlayer.getUUID()))
        .filter(Objects::nonNull)
        .filter(PlayerWatcher.MatchParticipation::canStartCountdown)
        .max(Comparator.comparingLong(p -> p.absentLength))
        .ifPresent(p -> teamManager.playerTeam(p.uuid).ifPresent(t -> startCountdown(t, p)));
  }

  public static class ForfeitCheck {

    private final Competitor team;
    private final PlayerWatcher.MatchParticipation participation;
    private final Set<UUID> voted = new HashSet<>();

    private ScheduledFuture<?> scheduledFuture;
    private boolean votable = false;

    public ForfeitCheck(Competitor team, PlayerWatcher.MatchParticipation participation) {
      this.team = team;
      this.participation = participation;

      startTimer();
    }

    public Competitor getTeam() {
      return team;
    }

    public Set<UUID> getVoted() {
      return voted;
    }

    public boolean isVotable() {
      return votable;
    }

    private void startTimer() {
      if (participation.absentDuration().compareTo(PlayerWatcher.ABSENT_MAX) > 0) {
        broadcast();
      } else {

        Duration length = PlayerWatcher.ABSENT_MAX.minus(participation.absentDuration());
        scheduledFuture =
            team.getMatch()
                .getExecutor(MatchScope.RUNNING)
                .schedule(this::broadcast, length.getSeconds(), TimeUnit.SECONDS);
      }
    }

    private void stopTimer() {
      if (scheduledFuture.isDone() || scheduledFuture.isCancelled()) return;
      scheduledFuture.cancel(true);
    }

    private void broadcast() {
      votable = true;
      team.sendMessage(Messages.forfeit());
      check();
    }

    public void addVote(MatchPlayer player) {
      voted.add(player.getId());
      check();
    }

    public void check() {
      if (hasPassed()) endMatch();
    }

    private boolean hasPassed() {
      // If all but one player has voted or all of the online players
      long votes = voted.stream().filter(uuid -> team.getPlayer(uuid) != null).count();

      return votes
          >= Math.min(
              team instanceof Team ? ((Team) team).getMaxPlayers() - 1 : 0,
              team.getPlayers().size());
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
}
