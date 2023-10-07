package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.pgm.events.team.TournamentPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** A team for a bolt match, has the id & name, as well as the involved participants */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team implements BoltTournamentTeam {

  private Integer id;
  private String name;
  private String mmr;
  private String color;

  private Double score;

  private List<Participation> participations;

  public Team() {}

  public Team(int id) {
    this.id = id;
    this.participations = new ArrayList<>();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMmr() {
    return mmr;
  }

  public void setMmr(String mmr) {
    this.mmr = mmr;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public List<Participation> getParticipations() {
    return participations;
  }

  public void setParticipations(List<Participation> participations) {
    this.participations = participations;
  }

  @Override
  @JsonIgnore
  public List<? extends TournamentPlayer> getPlayers() {
    return getParticipations().stream().map(Participation::getUser).collect(Collectors.toList());
  }

  @Override
  public void forEachPlayer(Consumer<Player> func) {
    participations.stream()
        .map(Participation::getUser)
        .map(User::getUUID)
        .map(Bukkit::getPlayer)
        .filter(Objects::nonNull)
        .forEach(func);
  }

  @Override
  public String toString() {
    return "Team "
        + getName()
        + ": \n "
        + getParticipations().stream()
            .map(Participation::getUser)
            .map(User::toString)
            .collect(Collectors.joining(", "));
  }

  @Override
  @JsonIgnore
  public Integer getTeamId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Team that = (Team) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
