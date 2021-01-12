package rip.bolt.ingame.ranked;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;

public class FetchMatchAsync implements Runnable {

  private final Consumer<BoltMatch> callback;

  public FetchMatchAsync(Consumer<BoltMatch> setupMatch) {
    this.callback = setupMatch;
  }

  @Override
  public void run() {
    BoltMatch boltMatch = Ingame.get().getApiManager().fetchMatchData();

    if (boltMatch != null) {
      Bukkit.runOnMainThread(Ingame.get(), false, () -> callback.accept(boltMatch));
    }
  }
}
