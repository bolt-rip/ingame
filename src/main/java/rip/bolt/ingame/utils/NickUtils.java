package rip.bolt.ingame.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import rip.bolt.ingame.managers.NickManager;

public class NickUtils {

  public static void createManager(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("Community")) return;
    plugin.getServer().getPluginManager().registerEvents(new NickManager(), plugin);
  }
}
