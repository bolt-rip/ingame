package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.time.Instant;
import java.util.List;

public class BoltMatch {

  @JsonProperty("id")
  private String matchId;

  private String map;

  @JsonProperty(access = Access.WRITE_ONLY)
  private List<Team> teams;

  private String winner;

  private Instant startedAt;
  private Instant endedAt;

  private boolean invalidate = false;

  public BoltMatch() {}

  public BoltMatch(String matchId) {
    this.matchId = matchId;
  }

  public String getMatchId() {
    return matchId;
  }

  public void setMatchId(String matchId) {
    this.matchId = matchId;
  }

  public String getMap() {
    return map;
  }

  public void setMap(String map) {
    this.map = map;
  }

  public List<Team> getTeams() {
    return teams;
  }

  public void setTeams(List<Team> teams) {
    this.teams = teams;
  }

  public String getWinner() {
    return winner;
  }

  public void setWinner(String winner) {
    this.winner = winner;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(Instant endedAt) {
    this.endedAt = endedAt;
  }

  public boolean isInvalidate() {
    return invalidate;
  }

  public void setInvalidate(boolean invalidate) {
    this.invalidate = invalidate;
  }

  public void invalidate() {
    this.invalidate = true;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("Match ID: ").append(getMatchId()).append("\n");

    str.append("Teams: ");
    for (int i = 0; i < getTeams().size(); i++)
      str.append(getTeams().get(i).toString()).append("\n");

    return str.toString();
  }
}
