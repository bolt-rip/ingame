package rip.bolt.ingame.events;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.ranked.MatchStatus;

public class BoltMatchStatusChangeEvent extends BoltMatchEvent {

  private static final HandlerList handlers = new HandlerList();

  @Nullable private final MatchStatus oldStatus;
  private final MatchStatus newStatus;

  public BoltMatchStatusChangeEvent(
      BoltMatch boltMatch, @Nullable MatchStatus oldStatus, MatchStatus newStatus) {
    super(boltMatch);
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
  }

  public @Nullable MatchStatus getOldStatus() {
    return oldStatus;
  }

  public MatchStatus getNewStatus() {
    return newStatus;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
