package rip.bolt.ingame;

import dev.pgm.events.Tournament;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import rip.bolt.ingame.api.APIManager;
import rip.bolt.ingame.commands.RankedAdminCommands;
import rip.bolt.ingame.config.AppData;
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

  private RankedManager rankedManager;
  private APIManager apiManager;

  private static Ingame plugin;

  @Override
  public void onEnable() {
    plugin = this;
    saveDefaultConfig();

    if (!AppData.API.isEnabled()) {
      System.out.println("[Ingame] Ingame was not enabled!");
      return;
    }

    apiManager = new APIManager();

    rankedManager = new RankedManager();

    Bukkit.getPluginManager().registerEvents(rankedManager, this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getPlayerWatcher(), this);

    BasicBukkitCommandGraph g = new BasicBukkitCommandGraph(new CommandModule());
    DispatcherNode node = g.getRootDispatcherNode();
    node.registerCommands(new RankedAdminCommands());
    new CommandExecutor(this, g).register();

    System.out.println("[Ingame] Ingame is now enabled!");
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
