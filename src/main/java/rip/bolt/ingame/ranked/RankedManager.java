package rip.bolt.ingame.ranked;

import com.google.common.collect.Iterables;
import dev.pgm.events.Tournament;
import dev.pgm.events.format.RoundReferenceHolder;
import dev.pgm.events.format.TournamentFormat;
import dev.pgm.events.format.TournamentFormatImpl;
import dev.pgm.events.format.TournamentRoundOptions;
import dev.pgm.events.format.rounds.single.SingleRound;
import dev.pgm.events.format.rounds.single.SingleRoundOptions;
import dev.pgm.events.format.winner.BestOfCalculation;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.Audience;

public class RankedManager implements Listener {

  private final PlayerWatcher playerWatcher;
  private final RankManager rankManager;
  private final MatchSearch poll;

  private TournamentFormat format;
  private BoltMatch match;

  private Duration cycleTime = Duration.ofSeconds(0);
  private boolean manuallyCanceled;

  public RankedManager() {
    playerWatcher = new PlayerWatcher(this);
    rankManager = new RankManager(this);
    MatchPreloader.create();

    poll = new MatchSearch(this::setupMatch);
    poll.startIn(Duration.ofSeconds(15));
  }

  public void setupMatch(BoltMatch match) {
    if (!this.isServerReady()) return;
    if (!this.isMatchValid(match)) return;

    this.match = match;
    this.manuallyCanceled = false;
    poll.stop();

    Tournament.get().getTeamManager().clear();
    for (TournamentTeam team : match.getTeams()) Tournament.get().getTeamManager().addTeam(team);

    playerWatcher.addPlayers(
        match.getTeams().stream()
            .flatMap(team -> team.getPlayers().stream())
            .map(TournamentPlayer::getUUID)
            .collect(Collectors.toList()));
    rankManager.updateAll();

    format =
        new TournamentFormatImpl(
            Tournament.get().getTeamManager(),
            new TournamentRoundOptions(
                false,
                false,
                false,
                Duration.ofMinutes(30),
                Duration.ofSeconds(30),
                Duration.ofSeconds(40),
                new BestOfCalculation<>(1)),
            new RoundReferenceHolder());
    SingleRound ranked =
        new SingleRound(
            format,
            new SingleRoundOptions(
                "ranked", cycleTime, Duration.ofSeconds(300), match.getMap(), 1, true, true));
    cycleTime = Duration.ofSeconds(5);
    format.addRound(ranked);

    Bukkit.broadcastMessage(ChatColor.YELLOW + "A new match is starting on this server!");
    Tournament.get()
        .getTournamentManager()
        .createTournament(PGM.get().getMatchManager().getMatches().next(), format);
  }

  private void updateMatch(BoltMatch match) {
    // TODO: handle the result after updating a match
  }

  private void updatePlayer(Player player) {}

  private boolean isMatchValid(BoltMatch match) {
    return match != null
        && match.getId() != null
        && !match.getId().isEmpty()
        && match.getMap() != null
        && !match.getMap().isEmpty()
        && (match.getStatus().equals(MatchStatus.CREATED)
            || match.getStatus().equals(MatchStatus.LOADED))
        && (this.match == null || !Objects.equals(this.match.getId(), match.getId()));
  }

  private boolean isServerReady() {
    return !RestartManager.isQueued() && RestartManager.getCountdown() == null;
  }

  public BoltMatch getMatch() {
    return match;
  }

  public PlayerWatcher getPlayerWatcher() {
    return playerWatcher;
  }

  public RankManager getRankManager() {
    return rankManager;
  }

  public MatchSearch getPoll() {
    return poll;
  }

  public void manualPoll(boolean repeat) {
    if (repeat) {
      poll.startIn(0L);
      return;
    }

    poll.trigger(true);
  }

  public void manualReset() {
    this.match = null;
  }

  public void manualCancel(Match match) {
    this.manuallyCanceled = true;
    this.postMatchStatus(match, MatchStatus.CANCELLED);
  }

  public boolean isManuallyCanceled() {
    return manuallyCanceled;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    postMatchStatus(event.getMatch(), MatchStatus.LOADED);
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    postMatchStatus(event.getMatch(), MatchStatus.STARTED);
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    postMatchStatus(event.getMatch(), MatchStatus.ENDED);

    poll.startIn(Duration.ofSeconds(30));

    if (!AppData.allowRequeue()) return;
    // delay requeue message until after match stats are sent
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Ingame.get(),
            () -> Audience.get(event.getMatch().getCompetitors()).sendMessage(Messages.requeue()),
            20);
  }

  public void postMatchStatus(Match match, MatchStatus status) {
    Instant now = Instant.now();
    Ingame.newSharedChain("match")
        .syncFirst(() -> transition(match, this.match, status, now))
        .abortIfNull()
        .async(Ingame.get().getApiManager()::postMatch)
        .syncLast(this::updateMatch)
        .execute();
  }

  public BoltMatch transition(
      Match match, BoltMatch boltMatch, MatchStatus newStatus, Instant transitionAt) {
    if (boltMatch == null || !boltMatch.getStatus().canTransitionTo(newStatus)) return null;

    switch (newStatus) {
      case LOADED:
        break;
      case STARTED:
        boltMatch.setMap(match.getMap().getName());
        boltMatch.setStartedAt(transitionAt);
        break;
      case ENDED:
        boltMatch.setEndedAt(transitionAt);
        Collection<Competitor> winners = match.getWinners();
        if (winners.size() == 1) {
          format
              .teamManager()
              .tournamentTeam(Iterables.getOnlyElement(winners))
              .filter(t -> t instanceof Team)
              .map(t -> (Team) t)
              .ifPresent(boltMatch::setWinner);
        }
        break;
      case CANCELLED:
        boltMatch.setEndedAt(transitionAt);
        break;
    }

    boltMatch.setStatus(newStatus);

    return boltMatch;
  }
}
