package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class BoltMatch {

  @JsonProperty("match")
  private String matchId; // 6 character hex code representing the match id number

  private String map;

  private List<Team> teams;
  private List<String> winners;

  private Instant startTime;
  private Instant endTime;

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

  public List<String> getWinners() {
    return winners;
  }

  public void setWinners(List<String> winners) {
    this.winners = winners;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
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
