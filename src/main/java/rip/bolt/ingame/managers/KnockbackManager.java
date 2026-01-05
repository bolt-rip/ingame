package rip.bolt.ingame.managers;

import static rip.bolt.ingame.utils.ReflectionUtils.setConfigValue;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;
import rip.bolt.ingame.api.definitions.BoltKnockback;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;

public class KnockbackManager implements Listener {
  public KnockbackManager() {}

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBoltMatchStateChange(BoltMatchStatusChangeEvent event) {
    if (Objects.equals(event.getNewStatus(), MatchStatus.CREATED)) {
      setupKnockback(event.getBoltMatch().getSeries().getKnockback());
    }
  }

  public void setupKnockback(@Nullable BoltKnockback knockback) {
    if (knockback == null) knockback = BoltKnockback.defaults();

    setConfigValue("knockbackFriction", knockback.getFriction());
    setConfigValue("knockbackHorizontal", knockback.getHorizontal());
    setConfigValue("knockbackVertical", knockback.getVertical());
    setConfigValue("knockbackVerticalLimit", knockback.getVerticalLimit());
    setConfigValue("knockbackExtraHorizontal", knockback.getExtraHorizontal());
    setConfigValue("knockbackExtraVertical", knockback.getExtraVertical());
  }
}
