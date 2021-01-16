package rip.bolt.ingame.ranked;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import rip.bolt.ingame.Ingame;
import tc.oc.pgm.api.PGM;

/**
 * Make sure that the first PGM match is loaded by running in an async thread.
 *
 * <p>Locks are used to prevent the creation of two matches which can happen due to conflicts here
 * and in PGMListener on AsyncPlayerPreLoginEvent.
 */
public class MatchPreloader implements Listener {

  private final ReentrantLock lock;

  private MatchPreloader() {
    this.lock = new ReentrantLock();
    Bukkit.getScheduler().runTaskAsynchronously(Ingame.get(), this::createMatch);
  }

  public static void create() {
    Bukkit.getPluginManager().registerEvents(new MatchPreloader(), Ingame.get());
  }

  private void createMatch() {
    lock.lock();

    try {
      if (!PGM.get().getMatchManager().getMatches().hasNext())
        PGM.get().getMatchManager().createMatch(null).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    } finally {
      HandlerList.unregisterAll(this);
      lock.unlock();
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPrePlayerLoginLow(final AsyncPlayerPreLoginEvent event) {
    lock.lock();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPrePlayerLoginHigh(final AsyncPlayerPreLoginEvent event) {
    lock.unlock();
  }
}
