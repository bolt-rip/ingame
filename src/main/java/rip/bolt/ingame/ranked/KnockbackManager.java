package rip.bolt.ingame.ranked;

import org.github.paperspigot.PaperSpigotConfig;
import rip.bolt.ingame.api.definitions.BoltKnockback;

import javax.annotation.Nullable;

public class KnockbackManager {
    public KnockbackManager() {}

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
