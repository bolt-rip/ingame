package rip.bolt.ingame;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
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
import rip.bolt.ingame.ranked.ForfeitManager;
import rip.bolt.ingame.ranked.RankedManager;
import rip.bolt.ingame.ranked.RequeueManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class Ingame extends JavaPlugin {

  private static TaskChainFactory taskChainFactory;
  private RankedManager rankedManager;
  private APIManager apiManager;
  private BukkitCommandManager commands;

  private static Ingame plugin;

  @Override
  public void onEnable() {
    plugin = this;
    saveDefaultConfig();

    taskChainFactory = BukkitTaskChainFactory.create(this);

    apiManager = new APIManager();
    rankedManager = new RankedManager(this);
    commands = new BukkitCommandManager(this);

    Bukkit.getPluginManager().registerEvents(rankedManager, this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getPlayerWatcher(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getRankManager(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getRequeueManager(), this);
    Bukkit.getPluginManager().registerEvents(rankedManager.getSpectatorManager(), this);

    registerCommands();

    getLogger().info("[Ingame] Ingame is now enabled!");
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
    getLogger().info("[Ingame] Ingame is now disabled!");
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

  private void registerCommands() {
    commands.registerDependency(Tournament.class, Tournament.get());
    commands.registerDependency(RankedManager.class, rankedManager);
    commands.registerDependency(
        ForfeitManager.class, rankedManager.getPlayerWatcher().getForfeitManager());
    commands.registerDependency(RequeueManager.class, rankedManager.getRequeueManager());

    commands
        .getCommandContexts()
        .registerIssuerOnlyContext(
            Match.class, c -> PGM.get().getMatchManager().getMatch(c.getSender()));

    commands
        .getCommandContexts()
        .registerIssuerOnlyContext(
            MatchPlayer.class,
            c -> {
              if (!c.getIssuer().isPlayer()) {
                throw new InvalidCommandArgument("You are unable to run this command");
              }
              final MatchPlayer player = PGM.get().getMatchManager().getPlayer(c.getPlayer());
              if (player != null) {
                return player;
              }
              throw new InvalidCommandArgument(
                  "Sorry, an error occured while resolving your player");
            });

    commands.registerCommand(new RequeueCommands());
    commands.registerCommand(new ForfeitCommands());
    commands.registerCommand(new RankedAdminCommands());
  }
}
