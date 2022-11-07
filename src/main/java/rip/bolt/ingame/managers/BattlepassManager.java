package rip.bolt.ingame.managers;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.Series;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;
import tc.oc.occ.dispense.events.UpdateObjectiveTrackingEvent;

public class BattlepassManager implements Listener {

  public BattlepassManager() {
    Bukkit.getServer().getPluginManager().registerEvents(this, Ingame.get());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBoltMatchStateChange(BoltMatchStatusChangeEvent event) {
    if (!Objects.equals(event.getNewStatus(), MatchStatus.CREATED)) return;

    // Check if service should allow for battlepass progression
    Series.Service service = event.getBoltMatch().getSeries().getService();
    boolean enabled = (service != Series.Service.PUG);

    Bukkit.getServer().getPluginManager().callEvent(new UpdateObjectiveTrackingEvent(enabled));
  }
}
