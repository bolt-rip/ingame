package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The participation of a user in a team */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participation {

  private User user;

  public Participation() {}

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
