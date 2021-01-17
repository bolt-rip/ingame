package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A response sent by the API used in commands with feedback. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoltResponse {

  private boolean success;
  private String message;

  public boolean getSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }
}
