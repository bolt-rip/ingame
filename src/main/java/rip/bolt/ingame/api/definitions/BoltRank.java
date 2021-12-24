package rip.bolt.ingame.api.definitions;

import java.util.Locale;

public enum BoltRank {
  S("S"),
  A_PLUS("A+"),
  A("A"),
  B_PLUS("B"),
  B("B"),
  C_PLUS("C+"),
  C("C"),
  D_PLUS("D+"),
  D("D"),
  UNRANKED("Unranked");

  private final String displayName;

  BoltRank(String name) {
    this.displayName = name;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public String getId() {
    return this.name().toLowerCase(Locale.ROOT);
  }

  public String toString() {
    return this.name();
  }
}
