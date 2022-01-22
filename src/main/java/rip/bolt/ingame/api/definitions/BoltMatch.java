package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import rip.bolt.ingame.ranked.MatchStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoltMatch {

  private String id;
  private BoltPGMMap map;
  private List<Team> teams;

  private Team winner;

  private Instant startedAt;
  private Instant endedAt;

  private MatchStatus status;

  public BoltMatch() {}

  public BoltMatch(String matchId) {
    this.id = matchId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BoltPGMMap getMap() {
    return map;
  }

  public void setMap(BoltPGMMap map) {
    this.map = map;
  }

  public List<Team> getTeams() {
    return teams;
  }

  public void setTeams(List<Team> teams) {
    this.teams = teams;
  }

  public Team getWinner() {
    return winner;
  }

  public void setWinner(Team winner) {
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

  public MatchStatus getStatus() {
    return status;
  }

  public void setStatus(MatchStatus status) {
    this.status = status;
  }

  public User getUser(UUID uuid) {
    return teams.stream()
        .map(Team::getParticipations)
        .flatMap(Collection::stream)
        .map(Participation::getUser)
        .filter(u -> u.getUUID().equals(uuid))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String toString() {
    StringBuilder str =
        new StringBuilder()
            .append("Match ID: ")
            .append(getId())
            .append("\n")
            .append("Map: ")
            .append(getMap().getName())
            .append("\n")
            .append("Status: ")
            .append(getStatus())
            .append("\n");

    str.append("Teams: ").append("\n");
    for (int i = 0; i < getTeams().size(); i++)
      str.append(getTeams().get(i).toString()).append("\n");

    return str.toString();
  }
}
