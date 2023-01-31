package rip.bolt.ingame.managers;

import org.bukkit.plugin.Plugin;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.RankedTeamTabEntry;
import tc.oc.pgm.tablist.FreeForAllTabEntry;
import tc.oc.pgm.tablist.MapTabEntry;
import tc.oc.pgm.tablist.MatchFooterTabEntry;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.util.tablist.PlayerTabEntry;

public class TabManager {

  public TabManager(Plugin plugin) {

    IngameTabManager matchTabManager;

    if (AppData.customTabEnabled()) {
      matchTabManager = new IngameTabManager(plugin);
      plugin.getServer().getPluginManager().registerEvents(matchTabManager, plugin);
    }
  }

  public static class IngameTabManager extends MatchTabManager {

    public IngameTabManager(Plugin plugin) {
      super(
          plugin,
          PlayerTabEntry::new,
          RankedTeamTabEntry::new,
          MapTabEntry::new,
          MatchTabManager::headerFactory,
          MatchFooterTabEntry::new,
          FreeForAllTabEntry::new);
    }
  }
}
