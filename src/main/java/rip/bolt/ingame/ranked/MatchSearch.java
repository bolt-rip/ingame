package rip.bolt.ingame.ranked;

import dev.pgm.events.Tournament;
import java.time.Duration;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;

public class MatchSearch {

  private final Consumer<BoltMatch> setupMatch;
  private int syncTaskId = -1;

  public MatchSearch(Consumer<BoltMatch> setupMatch) {
    this.setupMatch = setupMatch;
  }

  public void stop() {
    if (isSyncTaskRunning()) Bukkit.getScheduler().cancelTask(syncTaskId);
  }

  public boolean isSyncTaskRunning() {
    return syncTaskId != -1
        && (Bukkit.getScheduler().isCurrentlyRunning(syncTaskId)
            || Bukkit.getScheduler().isQueued(syncTaskId));
  }

  public void trigger() {
    trigger(false);
  }

  public void trigger(boolean force) {
    Ingame.newSharedChain("match")
        .abortIf(o -> !force && !isSyncTaskRunning())
        .asyncFirst(() -> Ingame.get().getApiManager().fetchMatchData())
        .abortIfNull()
        .syncLast(setupMatch::accept)
        .execute();
  }

  public void startIn(Duration delay) {
    startIn(delay.getSeconds() * 20);
  }

  public void startIn(long delay) {
    stop();

    syncTaskId =
        Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(Tournament.get(), this::trigger, delay, 15 * 20);
  }
}
