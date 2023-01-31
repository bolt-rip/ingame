package rip.bolt.ingame;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import org.bukkit.plugin.java.JavaPlugin;
import rip.bolt.ingame.api.APIManager;
import rip.bolt.ingame.commands.IngameCommandGraph;
import rip.bolt.ingame.commands.PugCommands;
import rip.bolt.ingame.managers.MatchManager;

public class Ingame extends JavaPlugin {

  private static TaskChainFactory taskChainFactory;
  private MatchManager matchManager;
  private APIManager apiManager;

  private PugCommands pugCommands;

  private static Ingame plugin;

  @Override
  public void onEnable() {
    plugin = this;
    saveDefaultConfig();

    taskChainFactory = BukkitTaskChainFactory.create(this);

    apiManager = new APIManager();
    matchManager = new MatchManager(this);

    try {
      new IngameCommandGraph(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    System.out.println("[Ingame] Ingame is now enabled!");
  }

  public static <T> TaskChain<T> newChain() {
    return taskChainFactory.newChain();
  }

  public static <T> TaskChain<T> newSharedChain(String name) {
    return taskChainFactory.newSharedChain(name);
  }

  @Override
  public void onDisable() {
    plugin = null;
    System.out.println("[Ingame] Ingame is now disabled!");
  }

  public APIManager getApiManager() {
    return apiManager;
  }

  public MatchManager getMatchManager() {
    return matchManager;
  }

  public PugCommands getPugCommands() {
    return pugCommands;
  }

  public static Ingame get() {
    return plugin;
  }
}
