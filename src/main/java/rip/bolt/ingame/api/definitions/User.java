package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import dev.pgm.events.team.TournamentPlayer;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements TournamentPlayer {

  private UUID uuid;
  private Ranking ranking;

  @JsonProperty(access = Access.WRITE_ONLY)
  private List<MatchResult> history;

  public User() {}

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public List<MatchResult> getHistory() {
    return history;
  }

  public void setHistory(List<MatchResult> history) {
    this.history = history;
  }

  @Override
  @JsonIgnore
  @JsonProperty("UUID")
  public UUID getUUID() {
    return uuid;
  }

  public Ranking getRanking() {
    return ranking;
  }

  public void setRanking(Ranking ranking) {
    this.ranking = ranking;
  }

  @JsonIgnore
  public String getRank() {
    return this.getRanking().getRank().getId();
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
