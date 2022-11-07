package rip.bolt.ingame.managers;

import dev.pgm.events.EventsPlugin;
import java.util.Collection;
import java.util.UUID;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Participation;
import rip.bolt.ingame.api.definitions.Stats;
import rip.bolt.ingame.api.definitions.Team;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class StatsManager {

  public StatsManager() {}

  public void handleMatchUpdate(BoltMatch boltMatch, Match match) {
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
    ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);

    if (statsModule == null) return;

    boltMatch.getTeams().stream()
        .map(Team::getParticipations)
        .flatMap(Collection::stream)
        .forEach(participation -> populatePlayerStats(participation, statsModule, scoreModule));

    if (scoreModule == null) return;
    boltMatch
        .getTeams()
        .forEach(
            team ->
                EventsPlugin.get()
                    .getTeamManager()
                    .fromTournamentTeam(team)
                    .ifPresent(t -> team.setScore(scoreModule.getScore(t))));
  }

  public void populatePlayerStats(
      Participation participation, StatsMatchModule statsModule, ScoreMatchModule scoreModule) {
    UUID uuid = participation.getUser().getUuid();
    if (statsModule.hasNoStats(uuid)) return;

    PlayerStats stats = statsModule.getPlayerStat(uuid);
    participation.setStats(
        new Stats(
            stats.getKills(),
            stats.getDeaths(),
            stats.getMaxKillstreak(),
            stats.getDamageDone(),
            stats.getBowDamage(),
            stats.getDamageTaken(),
            stats.getBowDamageTaken(),
            stats.getShotsHit(),
            stats.getShotsTaken(),
            scoreModule != null ? scoreModule.getContributions().get(uuid) : 0));
  }
}
