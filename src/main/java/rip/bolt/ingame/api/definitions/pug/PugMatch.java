package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import rip.bolt.ingame.api.definitions.BoltPGMMap;
import rip.bolt.ingame.api.definitions.MatchStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugMatch {

  private String id;
  private BoltPGMMap map;
  private String server;
  private MatchStatus status;
  private Instant createdAt;
  private Instant startedAt;
  private List<Integer> teamIds;

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

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public MatchStatus getStatus() {
    return status;
  }

  public void setStatus(MatchStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public List<Integer> getTeamIds() {
    return teamIds;
  }

  public void setTeamIds(List<Integer> teamIds) {
    this.teamIds = teamIds;
  }
}
