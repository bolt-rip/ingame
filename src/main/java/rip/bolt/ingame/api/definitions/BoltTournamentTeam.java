package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentTeam;
import javax.annotation.Nullable;
import tc.oc.pgm.teams.Team;

public interface BoltTournamentTeam extends TournamentTeam {

  Integer getTeamId();

  @JsonIgnore
  default @Nullable Team getPgmTeam() {
    return EventsPlugin.get().getTeamManager().fromTournamentTeam(this).orElse(null);
  }
}
