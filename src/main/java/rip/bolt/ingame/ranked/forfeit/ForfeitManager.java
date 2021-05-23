package rip.bolt.ingame.ranked.forfeit;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeamManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;

public class ForfeitManager {

  private static final Duration FORFEIT_DURATION = AppData.forfeitAfter();

  private final PlayerWatcher playerWatcher;

  private final Map<Competitor, LeaveAnnouncer> leaves = new HashMap<>();
  private final Map<Competitor, ForfeitPoll> forfeit = new HashMap<>();

  public ForfeitManager(PlayerWatcher playerWatcher) {
    this.playerWatcher = playerWatcher;
  }

  public ForfeitPoll getForfeitPoll(Competitor team) {
    return forfeit.computeIfAbsent(team, ForfeitPoll::new);
  }

  public boolean mayForfeit(Competitor team) {
    if (!AppData.forfeitEnabled()) return false;
    if (team.getMatch().getDuration().compareTo(FORFEIT_DURATION) >= 0) return true;

    return getRegisteredPlayers(team)
        .map(playerWatcher::getParticipation)
        .filter(Objects::nonNull)
        .anyMatch(PlayerWatcher.MatchParticipation::hasAbandoned);
  }

  public void clearPolls() {
    leaves.clear();
    forfeit.clear();
  }

  Stream<UUID> getRegisteredPlayers(Competitor team) {
    TournamentTeamManager teamManager = EventsPlugin.get().getTeamManager();
    return teamManager
        .tournamentTeam(team)
        .map(t -> t.getPlayers().stream())
        .orElse(Stream.empty())
        .map(TournamentPlayer::getUUID);
  }

  public void updateCountdown(Party team) {
    if (!AppData.forfeitEnabled() || !(team instanceof Competitor)) return;

    leaves.computeIfAbsent((Competitor) team, this::createAnnouncer).update();
  }

  private LeaveAnnouncer createAnnouncer(Competitor team) {
    return new LeaveAnnouncer(playerWatcher, team);
  }
}
