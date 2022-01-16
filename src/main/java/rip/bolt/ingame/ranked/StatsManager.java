package rip.bolt.ingame.ranked;

import java.util.Collection;
import org.bukkit.event.Listener;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Participation;
import rip.bolt.ingame.api.definitions.Stats;
import rip.bolt.ingame.api.definitions.Team;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;

public class StatsManager implements Listener {

  private final RankedManager manager;

  public StatsManager(RankedManager manager) {
    this.manager = manager;
  }

  public void handleMatchUpdate(BoltMatch boltMatch, Match match) {
    StatsMatchModule statsModule = match.needModule(StatsMatchModule.class);

    boltMatch.getTeams().stream()
        .map(Team::getParticipations)
        .flatMap(Collection::stream)
        .forEach(
            participation ->
                populatePlayerStats(
                    participation, statsModule.getPlayerStat(participation.getUser().getUUID())));
  }

  public void populatePlayerStats(Participation participation, PlayerStats stats) {
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
            stats.getShotsTaken()));
  }
}
