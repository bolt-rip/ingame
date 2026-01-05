package rip.bolt.ingame.api.definitions;

public enum MatchStatus {
  CREATED,
  LOADED,
  CANCELLED,
  STARTED,
  ENDED;

  public boolean canTransitionTo(MatchStatus next) {
    return switch (this) {
      case CREATED -> next == LOADED || next == CANCELLED;
      case LOADED -> next == STARTED || next == CANCELLED;
      case STARTED -> next == ENDED || next == CANCELLED;
      case ENDED, CANCELLED -> false;
    };
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
