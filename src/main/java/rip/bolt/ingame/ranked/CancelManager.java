package rip.bolt.ingame.ranked;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import com.google.common.collect.Ordering;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;

public class CancelManager {

  public static final Duration CANCEL_TIME_LIMIT = AppData.autoCancelBefore();
  public static final Duration CANCEL_ABSENCE_LENGTH = AppData.autoCancelCountdown();

  private final PlayerWatcher playerWatcher;
  private LeaverCountdown countdown = null;

  public CancelManager(PlayerWatcher playerWatcher) {
    this.playerWatcher = playerWatcher;
  }

  private void cancelMatch(Match match, UUID player) {
    playerWatcher.playersAbandoned(Collections.singletonList(player));
    playerWatcher.getRankedManager().cancel(match, CancelReason.AUTOMATED_CANCEL);
    match.sendMessage(Messages.participationBan());
  }

  public void clearCountdown() {
    if (countdown != null) countdown.cancelCountdown();
    this.countdown = null;
  }

  public void playerJoined(MatchPlayer player) {
    if (countdown != null && countdown.player.equals(player.getId())) clearCountdown();
    if (canCancel(player.getMatch())) startCountdownIfRequired(player.getMatch());
  }

  public void startCountdownIfRequired(Match match) {
    // Check if countdown required for any participants
    PlayerWatcher.MatchParticipation participation =
        playerWatcher.getParticipations().values().stream()
            .filter(
                matchParticipation ->
                    match.getPlayer(matchParticipation.getUUID()) == null
                        && matchParticipation.hasJoined())
            .max(Comparator.comparing(PlayerWatcher.MatchParticipation::currentAbsentDuration))
            .orElse(null);

    if (participation == null || (countdown != null && countdown.player == participation.getUUID()))
      return;

    // Cancel and clear existing countdown
    clearCountdown();

    Duration duration =
        Ordering.natural()
            .max(CANCEL_ABSENCE_LENGTH.minus(participation.absentDuration()), Duration.ZERO);

    countdown = new LeaverCountdown(this, match, participation.getUUID(), duration);
  }

  public void playerLeft(MatchPlayer player) {
    Match match = player.getMatch();
    if (canCancel(match)) {
      startCountdownIfRequired(match);
    }
  }

  private boolean canCancel(Match match) {
    return (match.getDuration().compareTo(CANCEL_TIME_LIMIT) < 0);
  }

  public static class LeaverCountdown {

    private CancelManager manager;
    private final Match match;
    private final UUID player;
    private long duration;

    private final ScheduledFuture<?> scheduledFuture;

    public LeaverCountdown(
        CancelManager cancelManager, Match match, UUID player, Duration duration) {
      this.manager = cancelManager;
      this.match = match;
      this.player = player;
      this.duration = (duration.toMillis() + 999) / 1000;

      scheduledFuture =
          match
              .getExecutor(MatchScope.RUNNING)
              .scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    private void broadcast() {
      match.sendWarning(
          text("Cancelling match in ")
              .append(text(duration, NamedTextColor.YELLOW))
              .append(text((duration == 1) ? " second" : " seconds")));
    }

    private void tick() {
      if (duration <= 0) {
        // Cancel match
        this.manager.cancelMatch(match, player);
        // Cancel scheduler
        cancelCountdown();
      } else if (duration % 5 == 0 || duration <= 3) broadcast();

      duration--;
    }

    public void cancelCountdown() {
      this.scheduledFuture.cancel(false);
    }

    @Override
    public String toString() {
      return "LeaverCountdown{" + "player=" + player + ", duration=" + duration + '}';
    }
  }
}
