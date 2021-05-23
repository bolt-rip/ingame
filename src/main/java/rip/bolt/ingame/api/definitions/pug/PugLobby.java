package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import rip.bolt.ingame.api.definitions.BoltPGMMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugLobby {

  private String id;
  private String name;
  private BoltPGMMap selectedMap;
  private PugMatch match;
  private PugPlayer owner;
  private PugState state;
  private List<PugPlayer> mods;
  private List<PugTeam> teams;
  private List<PugPlayer> observers;

  private PugVisibility visibility;
  private PugPermissions permissions;

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

  public BoltPGMMap getSelectedMap() {
    return selectedMap;
  }

  public void setSelectedMap(BoltPGMMap selectedMap) {
    this.selectedMap = selectedMap;
  }

  public PugMatch getMatch() {
    return match;
  }

  public void setMatch(PugMatch match) {
    this.match = match;
  }

  public PugPlayer getOwner() {
    return owner;
  }

  public void setOwner(PugPlayer owner) {
    this.owner = owner;
  }

  public PugState getState() {
    return state;
  }

  public void setState(PugState state) {
    this.state = state;
  }

  public List<PugPlayer> getMods() {
    return mods;
  }

  public void setMods(List<PugPlayer> mods) {
    this.mods = mods;
  }

  @JsonIgnore
  public List<PugPlayer> getPlayers() {
    return Stream.concat(teams.stream().flatMap(t -> t.getPlayers().stream()), observers.stream())
        .collect(Collectors.toList());
  }

  public List<PugTeam> getTeams() {
    return teams;
  }

  public void setTeams(List<PugTeam> teams) {
    this.teams = teams;
  }

  public List<PugPlayer> getObservers() {
    return observers;
  }

  public void setObservers(List<PugPlayer> observers) {
    this.observers = observers;
  }

  public PugVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(PugVisibility visibility) {
    this.visibility = visibility;
  }

  public PugPermissions getPermissions() {
    return permissions;
  }

  public void setPermissions(PugPermissions permissions) {
    this.permissions = permissions;
  }

  @Override
  public String toString() {
    return "PugLobby{"
        + "id='"
        + id
        + '\''
        + ", selectedMap="
        + selectedMap
        + ", owner="
        + owner
        + ", mods="
        + mods
        + ", teams="
        + teams
        + ", observers="
        + observers
        + '}';
  }
}
