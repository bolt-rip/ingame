package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A response sent by the API used in commands with feedback. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoltResponse {

  private boolean success;

  @JsonAlias({"reason", "message"})
  private String reason;

  public boolean getSuccess() {
    return success;
  }

  public String getReason() {
    return reason;
  }
}
