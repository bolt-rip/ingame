package rip.bolt.ingame.managers;

import java.util.Objects;
import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.github.paperspigot.PaperSpigotConfig;
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

    PaperSpigotConfig.knockbackFriction = knockback.getFriction();
    PaperSpigotConfig.knockbackHorizontal = knockback.getHorizontal();
    PaperSpigotConfig.knockbackVertical = knockback.getVertical();
    PaperSpigotConfig.knockbackVerticalLimit = knockback.getVerticalLimit();
    PaperSpigotConfig.knockbackExtraHorizontal = knockback.getExtraHorizontal();
    PaperSpigotConfig.knockbackExtraVertical = knockback.getExtraVertical();
  }
}
