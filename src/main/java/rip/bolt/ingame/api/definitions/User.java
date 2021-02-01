package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pgm.events.team.TournamentPlayer;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements TournamentPlayer {

  private UUID uuid;
  private String rank;

  public User() {}

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public String getRank() {
    return rank;
  }

  public void setRank(String rank) {
    this.rank = rank;
  }

  @Override
  @JsonIgnore
  @JsonProperty("UUID")
  public UUID getUUID() {
    return uuid;
  }

  @Override
  public boolean canVeto() {
    return true;
  }

  @Override
  public String toString() {
    return String.valueOf(uuid);
  }
}
