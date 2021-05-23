package rip.bolt.ingame.ranked.forfeit;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;

public class LeaveAnnouncer {

  private final PlayerWatcher watcher;
  private final ForfeitManager forfeitManager;

  private final Competitor team;
  private boolean hasCompleted;

  private ScheduledFuture<?> scheduledFuture;

  public LeaveAnnouncer(PlayerWatcher watcher, Competitor team) {
    this.watcher = watcher;
    this.team = team;

    this.forfeitManager = watcher.getForfeitManager();
  }

  private void broadcast() {
    if (this.hasCompleted) return;
    this.hasCompleted = true;
    team.sendMessage(Messages.forfeit());
  }

  void update() {
    if (this.hasCompleted) return;

    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }

    forfeitManager
        .getRegisteredPlayers(team)
        .map(watcher::getParticipation)
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
