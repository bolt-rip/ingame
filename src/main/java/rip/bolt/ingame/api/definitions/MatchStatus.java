package rip.bolt.ingame.api.definitions;

public enum MatchStatus {
  CREATED,
  LOADED,
  CANCELLED,
  STARTED,
  ENDED;

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

  public boolean isPreGame() {
    return this == CREATED || this == LOADED;
  }

  public boolean isFinished() {
    return this == ENDED || this == CANCELLED;
  }

  public String toString() {
    return this.name();
  }
}
