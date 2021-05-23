package rip.bolt.ingame;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import dev.pgm.events.EventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import rip.bolt.ingame.api.APIManager;
import rip.bolt.ingame.commands.AdminCommands;
import rip.bolt.ingame.commands.ForfeitCommands;
import rip.bolt.ingame.commands.PugCommands;
import rip.bolt.ingame.commands.RequeueCommands;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.utils.AudienceProvider;
import rip.bolt.ingame.utils.MapInfoParser;
import rip.bolt.ingame.utils.PartyProvider;
import rip.bolt.ingame.utils.TeamsProvider;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.graph.CommandExecutor;
import tc.oc.pgm.command.graph.MatchPlayerProvider;
import tc.oc.pgm.command.graph.MatchProvider;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import tc.oc.pgm.lib.app.ashcon.intake.fluent.DispatcherNode;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.AbstractModule;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;

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

    Bukkit.getPluginManager().registerEvents(matchManager, this);
    Bukkit.getPluginManager().registerEvents(matchManager.getRankManager(), this);

    BasicBukkitCommandGraph g = new BasicBukkitCommandGraph(new CommandModule());
    DispatcherNode node = g.getRootDispatcherNode();
    node.registerCommands(new RequeueCommands(matchManager));
    node.registerCommands(new ForfeitCommands(matchManager));

    node.registerNode("ingame").registerCommands(new AdminCommands(matchManager));

    DispatcherNode pugNode = node.registerNode("pug");
    pugCommands = new PugCommands(matchManager);
    pugNode.registerCommands(pugCommands);
    pugNode.registerNode("team").registerCommands(pugCommands.getTeamCommands());

    pugCommands.setCommandList(pugNode.getDispatcher().getAliases());

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

  public MatchManager getMatchManager() {
    return matchManager;
  }

  public PugCommands getPugCommands() {
    return pugCommands;
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
      bind(EventsPlugin.class).toInstance(EventsPlugin.get());
      bind(MapOrder.class).toInstance(PGM.get().getMapOrder());
    }

    private void configureProviders() {
      bind(MatchPlayer.class).toProvider(new MatchPlayerProvider());
      bind(Match.class).toProvider(new MatchProvider());
      bind(Party.class).toProvider(new PartyProvider());
      bind(TeamMatchModule.class).toProvider(new TeamsProvider());
      bind(Audience.class).toProvider(new AudienceProvider());
      bind(MapInfo.class).toProvider(new MapInfoParser());
    }
  }
}
