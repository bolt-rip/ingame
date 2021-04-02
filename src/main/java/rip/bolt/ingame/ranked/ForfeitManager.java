package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import dev.pgm.events.Tournament;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeamManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.teams.Team;

public class ForfeitManager {

  private static final Duration FORFEIT_DURATION = AppData.forfeitAfter();

  private final PlayerWatcher playerWatcher;

  private final Map<Competitor, LeaveAnnouncer> leaves = new HashMap<>();
  private final Map<Competitor, ForfeitPoll> forfeit = new HashMap<>();

  public ForfeitManager(PlayerWatcher playerWatcher) {
    this.playerWatcher = playerWatcher;
  }

  public ForfeitPoll getForfeitPoll(Competitor team) {
    return forfeit.computeIfAbsent(team, ForfeitPoll::new);
  }

  public boolean mayForfeit(Competitor team) {
    if (!AppData.forfeitEnabled()) return false;
    if (team.getMatch().getDuration().compareTo(FORFEIT_DURATION) >= 0) return true;

    return getRegisteredPlayers(team)
            .map(playerWatcher::getParticipation)
            .filter(Objects::nonNull)
            .anyMatch(PlayerWatcher.MatchParticipation::hasAbandoned);
  }

  public void clearPolls() {
    leaves.clear();
    forfeit.clear();
  }

  private Stream<UUID> getRegisteredPlayers(Competitor team) {
    TournamentTeamManager teamManager = Tournament.get().getTeamManager();
    return teamManager
        .tournamentTeam(team)
        .map(t -> t.getPlayers().stream())
        .orElse(Stream.empty())
        .map(TournamentPlayer::getUUID);
  }

  public void updateCountdown(Party team) {
    if (!AppData.forfeitEnabled() || !(team instanceof Competitor)) return;

    leaves.computeIfAbsent((Competitor) team, LeaveAnnouncer::new).update();
  }

  public class LeaveAnnouncer {
    private final Competitor team;
    private boolean hasCompleted;

    private ScheduledFuture<?> scheduledFuture;

    public LeaveAnnouncer(Competitor team) {
      this.team = team;
    }

    private void broadcast() {
      if (this.hasCompleted) return;
      this.hasCompleted = true;
      team.sendMessage(Messages.forfeit());
    }

    private void update() {
      if (this.hasCompleted) return;

      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
        scheduledFuture = null;
      }

      getRegisteredPlayers(team)
          .map(playerWatcher::getParticipation)
          .filter(PlayerWatcher.MatchParticipation::canStartCountdown)
          .map(PlayerWatcher.MatchParticipation::absentDuration)
          .max(Duration::compareTo)
          .map(PlayerWatcher.ABSENT_MAX::minus)
          .filter(duration -> !duration.isNegative())
          .ifPresent(
              duration ->
                  scheduledFuture =
                      team.getMatch()
                          .getExecutor(MatchScope.RUNNING)
                          .schedule(this::broadcast, duration.toMillis(), TimeUnit.MILLISECONDS));
    }
  }

  public static class ForfeitPoll {

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
