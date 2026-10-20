package rip.bolt.ingame.utils;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentTeam;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import rip.bolt.ingame.Ingame;
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
    Map<Integer, Team> pgmTeams = new HashMap<>();
    Set<Integer> duplicateIds = new HashSet<>();
    for (TournamentTeam tournamentTeam : EventsPlugin.get().getTeamManager().teams()) {
      BoltTournamentTeam boltTeam = (BoltTournamentTeam) tournamentTeam;
      Team pgmTeam = boltTeam.getPgmTeam();
      if (pgmTeam == null) {
        Ingame.get()
            .getLogger()
            .warning("Team " + boltTeam.getTeamId() + " has no PGM team, not setting its color");
        continue;
      }
      if (pgmTeams.putIfAbsent(boltTeam.getTeamId(), pgmTeam) != null) {
        duplicateIds.add(boltTeam.getTeamId());
      }
    }

    if (!duplicateIds.isEmpty()) {
      Ingame.get()
          .getLogger()
          .warning("Duplicate team ids " + duplicateIds + ", not setting their colors");
      pgmTeams.keySet().removeAll(duplicateIds);
    }

    boltMatch.getTeams().forEach(team -> {
      Team pgmTeam = pgmTeams.get(team.getTeamId());
      if (pgmTeam != null) {
        team.setColor(
            "#" + String.format("%06X", 0xFFFFFF & pgmTeam.getFullColor().asRGB()));
      }
    });
  }
}
