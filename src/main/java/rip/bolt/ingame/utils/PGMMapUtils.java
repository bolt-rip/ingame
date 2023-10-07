package rip.bolt.ingame.utils;

import dev.pgm.events.EventsPlugin;
import java.util.Map;
import java.util.stream.Collectors;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltPGMMap;
import rip.bolt.ingame.api.definitions.BoltTournamentTeam;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;

public class PGMMapUtils {

  public static BoltPGMMap getBoltPGMMap(BoltMatch boltMatch, Match match) {
    String pgmMapName = match.getMap().getName();

    if (boltMatch.getMap() != null && boltMatch.getMap().getName().equals(pgmMapName)) {
      return boltMatch.getMap();
    }

    return new BoltPGMMap(pgmMapName);
  }

  public static void setTeamColors(BoltMatch boltMatch) {
    Map<Integer, Team> pgmTeams =
        EventsPlugin.get().getTeamManager().teams().stream()
            .map(tournamentTeam -> (BoltTournamentTeam) tournamentTeam)
            .collect(
                Collectors.toMap(BoltTournamentTeam::getTeamId, BoltTournamentTeam::getPgmTeam));

    boltMatch
        .getTeams()
        .forEach(
            team -> {
              Team pgmTeam = pgmTeams.get(team.getTeamId());
              if (pgmTeam != null) {
                team.setColor(
                    "#" + String.format("%06X", 0xFFFFFF & pgmTeam.getFullColor().asRGB()));
              }
            });
  }
}
