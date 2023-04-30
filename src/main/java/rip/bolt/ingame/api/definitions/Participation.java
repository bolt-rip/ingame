package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** The participation of a user in a team */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participation {
  private User user;
  private UUID userUuid;
  private Integer teamId;
  private Stats stats;
  private Integer deafenPenalty;

  public Participation() {}

  public Participation(User user, Integer teamId) {
    this.user = user;
    this.userUuid = user.getUUID();
    this.teamId = teamId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public UUID getUserUuid() {
    return userUuid;
  }

  public void setUserUuid(UUID userUuid) {
    this.userUuid = userUuid;
  }

  public Integer getTeamId() {
    return teamId;
  }

  public void setTeamId(Integer teamId) {
    this.teamId = teamId;
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
