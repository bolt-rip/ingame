package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pgm.events.team.TournamentPlayer;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugPlayer implements TournamentPlayer {

  private UUID uuid;
  private String username;

  public PugPlayer() {}

  public PugPlayer(UUID uuid, String username) {
    this.uuid = uuid;
    this.username = username;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    return "PugPlayer{" + "uuid=" + uuid + ", username='" + username + '\'' + '}';
  }

  @Override
  @JsonIgnore
  @JsonProperty("UUID")
  public UUID getUUID() {
    return uuid;
  }

  @Override
  public boolean canVeto() {
    return false;
  }
}
