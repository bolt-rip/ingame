package rip.bolt.ingame.managers;

import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.pugs.PugManager;
import rip.bolt.ingame.ranked.RankedManager;

public abstract class GameManager implements Listener {

  public final MatchManager matchManager;

  protected GameManager(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  public static GameManager of(MatchManager matchManager, BoltMatch match) {
    GameManager old = matchManager.getGameManager();
    GameManager newManager = of(match).apply(matchManager);

    if (old != newManager) {
      old.disable();
      newManager.enable(matchManager);
    }
    newManager.setup(match);
    return newManager;
  }

  private static Function<MatchManager, GameManager> of(BoltMatch match) {
    switch (match.getSeries().getService()) {
      case PUG:
      case TM:
      case DRAFT:
        return PugManager::of;
      case RANKED:
      default:
        return RankedManager::new;
    }
  }

  /** Called when the game manager is created. */
  public void enable(MatchManager manager) {
    Bukkit.getPluginManager().registerEvents(this, Ingame.get());
  }

  public abstract void setup(BoltMatch match);

  /** Called when the game manager is removed. */
  public void disable() {
    HandlerList.unregisterAll(this);
  }

  public static class NoopManager extends GameManager {
    public NoopManager(MatchManager matchManager) {
      super(matchManager);
    }

    @Override
    public void enable(MatchManager manager) {}

    @Override
    public void setup(BoltMatch match) {}

    @Override
    public void disable() {}
  }
}
