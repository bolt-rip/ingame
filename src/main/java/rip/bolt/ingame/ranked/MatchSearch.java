package rip.bolt.ingame.ranked;

import dev.pgm.events.Tournament;

import java.time.Duration;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;

public class MatchSearch {

  private final FetchMatchAsync fetchTask;
  private final Consumer<BoltMatch> setupMatch;

  private int syncTaskId = -1;
  private int asyncTaskId = -1;

  public MatchSearch(Consumer<BoltMatch> setupMatch) {
    this.setupMatch = setupMatch;
    fetchTask = new FetchMatchAsync(this::callback);
  }

  public void callback(BoltMatch match) {
    if (match != null) setupMatch.accept(match);
    asyncTaskId = -1;
  }

  public void startIn(Duration delay) {
    setupPollTask(delay.getSeconds() * 20);
  }

  public void stop() {
    if (syncTaskId != -1) {
      Bukkit.getScheduler().cancelTask(syncTaskId);
    }
  }

  public void trigger() {
    if (asyncTaskId == -1) {
      asyncTaskId =
              Bukkit.getScheduler().runTaskAsynchronously(Ingame.get(), fetchTask).getTaskId();
    }
  }

  private void setupPollTask(long delay) {
    stop();

    syncTaskId =
            Bukkit.getScheduler()
                    .scheduleSyncRepeatingTask(Tournament.get(), this::trigger, delay, 15 * 20);
  }
}
