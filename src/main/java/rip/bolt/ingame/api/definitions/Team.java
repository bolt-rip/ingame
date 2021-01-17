package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;
import java.util.List;

/**
 * Class to represent a given team in a Bolt match. The team will have a list of {@link
 * Participant}s (aka the team roster) assigned to it. This list of players is fetched from the Bolt
 * API in the {@link rip.bolt.ingame.api.APIManager} class.
 *
 * @author Picajoluna
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team implements TournamentTeam {

  private String name;

  @JsonProperty("players")
  private List<Participant> participants;

  public Team() {}

  public Team(String name, List<Participant> participants) {
    this.name = name;
    this.participants = participants;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Participant> getPlayers() {
    return this.participants;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("Team ").append(getName()).append(": ");

    // Print list of participants on the team
    for (TournamentPlayer player : getPlayers()) str.append(player.toString()).append(", ");
    str.setLength(str.length() - 2); // remove the final ", "

    return str.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Team that = (Team) o;

    return name.equals(that.getName());
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
