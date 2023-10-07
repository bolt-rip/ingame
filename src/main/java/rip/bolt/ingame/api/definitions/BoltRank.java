package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import java.util.Locale;

public enum BoltRank {
  GRANDMASTER("Grandmaster"),
  MASTER_5("Master 5"),
  MASTER_4("Master 4"),
  MASTER_3("Master 3"),
  MASTER_2("Master 2"),
  MASTER_1("Master 1"),
  DIAMOND_5("Diamond 5"),
  DIAMOND_4("Diamond 4"),
  DIAMOND_3("Diamond 3"),
  DIAMOND_2("Diamond 2"),
  DIAMOND_1("Diamond 1"),
  PLATINUM_5("Platinum 5"),
  PLATINUM_4("Platinum 4"),
  PLATINUM_3("Platinum 3"),
  PLATINUM_2("Platinum 2"),
  PLATINUM_1("Platinum 1"),
  GOLD_5("Gold 5"),
  GOLD_4("Gold 4"),
  GOLD_3("Gold 3"),
  GOLD_2("Gold 2"),
  GOLD_1("Gold 1"),
  SILVER_5("Silver 5"),
  SILVER_4("Silver 4"),
  SILVER_3("Silver 3"),
  SILVER_2("Silver 2"),
  SILVER_1("Silver 1"),
  BRONZE_5("Bronze 5"),
  BRONZE_4("Bronze 4"),
  BRONZE_3("Bronze 3"),
  BRONZE_2("Bronze 2"),
  BRONZE_1("Bronze 1"),
  @JsonEnumDefaultValue
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
