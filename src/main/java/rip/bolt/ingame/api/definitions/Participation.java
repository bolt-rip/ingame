package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

/** The participation of a user in a team */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participation {

  private User user;
  private Stats stats;
  private Integer deafenPenalty;

  public Participation() {}

  public Participation(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Stats getStats() {
    return stats;
  }

  public void setStats(Stats stats) {
    this.stats = stats;
  }

  public @Nullable Integer getDeafenPenalty() {
    return deafenPenalty;
  }

  public void setDeafenPenalty(Integer deafenPenalty) {
    this.deafenPenalty = deafenPenalty;
  }
}
