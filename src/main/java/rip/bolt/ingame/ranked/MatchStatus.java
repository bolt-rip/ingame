package rip.bolt.ingame.ranked;

public enum MatchStatus {
  CREATED,
  LOADED,
  STARTED,
  ENDED,
  CANCELLED;

  public boolean canTransitionTo(MatchStatus next) {
    switch (this) {
      case CREATED:
        return next == LOADED || next == CANCELLED;
      case LOADED:
        return next == STARTED || next == CANCELLED;
      case STARTED:
        return next == ENDED || next == CANCELLED;
      case ENDED:
      case CANCELLED:
        return false;
      default:
        throw new IllegalStateException("Unknown transition state for " + next);
    }
  }

  public String toString() {
    return this.name();
  }
}
