package rip.bolt.ingame.events;

import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;

@NullMarked
public class BoltMatchStatusChangeEvent extends BoltMatchEvent {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  @Nullable
  private final MatchStatus oldStatus;

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
    return HANDLER_LIST;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
