package rip.bolt.ingame.ranked;

public enum MatchStatus {
  CREATED,
  STARTED,
  ENDED,
  CANCELLED;

  private MatchStatus() {}

  public boolean canTransitionTo(MatchStatus next) {
    switch (this) {
      case CREATED:
        return true;
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
