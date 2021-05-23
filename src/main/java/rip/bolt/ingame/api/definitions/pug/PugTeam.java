package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugTeam {

  private String id;
  private String name;
  private int maxPlayers;
  private List<PugPlayer> players;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public List<PugPlayer> getPlayers() {
    return players;
  }

  public void setPlayers(List<PugPlayer> players) {
    this.players = players;
  }

  // Check team id only as Events DefaultTeamManager
  // finds team looking this equals any registered tm team
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PugTeam pugTeam = (PugTeam) o;
    return Objects.equals(id, pugTeam.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
