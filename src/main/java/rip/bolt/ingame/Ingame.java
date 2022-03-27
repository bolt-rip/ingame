package rip.bolt.ingame;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import dev.pgm.events.Tournament;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import rip.bolt.ingame.api.APIManager;
import rip.bolt.ingame.commands.ForfeitCommands;
import rip.bolt.ingame.commands.RankedAdminCommands;
import rip.bolt.ingame.commands.RequeueCommands;
import rip.bolt.ingame.ranked.RankedManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.graph.CommandExecutor;
import tc.oc.pgm.command.graph.MatchPlayerProvider;
import tc.oc.pgm.command.graph.MatchProvider;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import tc.oc.pgm.lib.app.ashcon.intake.fluent.DispatcherNode;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.AbstractModule;

public class Ingame extends JavaPlugin {

  private static TaskChainFactory taskChainFactory;
  private RankedManager rankedManager;
  private APIManager apiManager;

  private static Ingame plugin;

  @Override
  public void onEnable() {
    plugin = this;
    saveDefaultConfig();

    taskChainFactory = BukkitTaskChainFactory.create(this);

    apiManager = new APIManager();

    rankedManager = new RankedManager(this);

    Bukkit.getPluginManager().registerEvents(rankedManager, this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getPlayerWatcher(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getRankManager(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getRequeueManager(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getSpectatorManager(), this);

    BasicBukkitCommandGraph g = new BasicBukkitCommandGraph(new CommandModule());
    DispatcherNode node = g.getRootDispatcherNode();
    node.registerCommands(new RequeueCommands(rankedManager));
    node.registerCommands(new ForfeitCommands(rankedManager));

    DispatcherNode subNode = node.registerNode("ingame");
    subNode.registerCommands(new RankedAdminCommands(rankedManager));
    new CommandExecutor(this, g).register();

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

  public RankedManager getRankedManager() {
    return rankedManager;
  }

  public static Ingame get() {
    return plugin;
  }

  private static class CommandModule extends AbstractModule {

    @Override
    protected void configure() {
      configureInstances();
      configureProviders();
    }

    private void configureInstances() {
      bind(PGM.class).toInstance(PGM.get());
      bind(Tournament.class).toInstance(Tournament.get());
    }

    private void configureProviders() {
      bind(MatchPlayer.class).toProvider(new MatchPlayerProvider());
      bind(Match.class).toProvider(new MatchProvider());
    }
  }
}
