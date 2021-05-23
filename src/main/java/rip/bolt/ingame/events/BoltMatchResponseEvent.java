package rip.bolt.ingame.events;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;
import tc.oc.pgm.api.match.Match;

public class BoltMatchResponseEvent extends BoltMatchEvent {

  private static final HandlerList handlers = new HandlerList();

  private final Match pgmMatch;
  private final BoltMatch responseMatch;
  @Nullable private final MatchStatus oldStatus;

  public BoltMatchResponseEvent(
      Match pgmMatch,
      BoltMatch boltMatch,
      BoltMatch responseMatch,
      @Nullable MatchStatus oldStatus) {
    super(boltMatch);
    this.pgmMatch = pgmMatch;
    this.responseMatch = responseMatch;
    this.oldStatus = oldStatus;
  }

  public Match getPgmMatch() {
    return pgmMatch;
  }

  public BoltMatch getResponseMatch() {
    return responseMatch;
  }

  @Nullable
  public MatchStatus getOldStatus() {
    return oldStatus;
  }

  public boolean hasMatchFinished() {
    return oldStatus != null && !oldStatus.isFinished() && responseMatch.getStatus().isFinished();
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
