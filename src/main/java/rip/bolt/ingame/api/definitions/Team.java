package rip.bolt.ingame.api.definitions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;

/**
 * Class to represent a given team in a Bolt match.
 * The team will have a list of {@link Participant}s (aka the team roster) assigned to it.
 * This list of players is fetched from the Bolt API in the {@link rip.bolt.ingame.api.APIManager} class.
 * 
 * @author Picajoluna
 */
@JsonDeserialize(as = Team.class)
public class Team implements TournamentTeam {

    private String name;

    @JsonProperty("players")
    private List<Participant> participants;

    public Team() {

    }

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
        str.append("Team " + getName() + ": ");

        // Print list of participants on the team
        for (TournamentPlayer player : getPlayers())
                str.append(player.toString() + ", ");
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
