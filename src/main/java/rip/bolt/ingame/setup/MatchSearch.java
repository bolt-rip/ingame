package rip.bolt.ingame.setup;

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

  public synchronized void startIn(long delay) {

    System.out.println("[Ingame] Request poll for " + delay);

    // Don't poll if already polling
    if (this.isSyncTaskRunning()) return;

    System.out.println("[Ingame] Starting poll for " + delay);

    stop();

    syncTaskId =
        Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(Ingame.get(), this::trigger, delay, 15 * 20);
  }
}
