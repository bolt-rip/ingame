package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** A team for a bolt match, has the id & name, as well as the involved participants */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team implements TournamentTeam {

  private Integer id;
  private String name;

  @JsonProperty(access = Access.WRITE_ONLY)
  private List<Participation> participations;

  public Team() {}

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

  public List<Participation> getParticipations() {
    return participations;
  }

  public void setParticipations(List<Participation> participations) {
    this.participations = participations;
  }

  @Override
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
        + ": "
        + getParticipations().stream()
            .map(Participation::getUser)
            .map(User::toString)
            .collect(Collectors.joining(", "));
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
