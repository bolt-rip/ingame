package rip.bolt.ingame.managers;

import com.google.common.collect.Iterables;
import dev.pgm.events.EventsPlugin;
import dev.pgm.events.format.RoundReferenceHolder;
import dev.pgm.events.format.TournamentFormat;
import dev.pgm.events.format.TournamentFormatImpl;
import dev.pgm.events.format.TournamentRoundOptions;
import dev.pgm.events.format.rounds.single.SingleRound;
import dev.pgm.events.format.rounds.single.SingleRoundOptions;
import dev.pgm.events.format.winner.BestOfCalculation;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.events.BoltMatchResponseEvent;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;
import rip.bolt.ingame.pugs.ManagedTeam;
import rip.bolt.ingame.setup.MatchPreloader;
import rip.bolt.ingame.setup.MatchSearch;
import rip.bolt.ingame.utils.BattlepassUtils;
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.PGMMapUtils;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.StartRestartCountdownEvent;
import tc.oc.pgm.result.TieVictoryCondition;

public class MatchManager implements Listener {

  private final RankManager rankManager;
  private final StatsManager statsManager;
  private final TabManager tabManager;
  private final KnockbackManager knockbackManager;
  private final BattlepassManager battlepassManager;

  private final MatchSearch poll;

  private GameManager gameManager;
  private TournamentFormat format;

  private BoltMatch match;
  private String pgmMatchId;
  private Match pgmMatch;

  private BoltMatch deferredMatch; // While restarting, this will be used to store pending match
  private boolean isRestarting = false;

  private Duration cycleTime = Duration.ofSeconds(0);
  private CancelReason cancelReason = null;

  public MatchManager(Plugin plugin) {
    gameManager = new GameManager.NoopManager(this);
    rankManager = new RankManager(this);
    statsManager = new StatsManager();
    tabManager = new TabManager(plugin);
    knockbackManager = new KnockbackManager();
    battlepassManager = BattlepassUtils.createManager();

    Bukkit.getPluginManager().registerEvents(this, plugin);
    Bukkit.getPluginManager().registerEvents(rankManager, plugin);
    Bukkit.getPluginManager().registerEvents(knockbackManager, plugin);

    MatchPreloader.create();

    poll = new MatchSearch(this::setupMatch);
    poll.startIn(Duration.ofSeconds(5));
  }

  public void setupMatch(BoltMatch match) {
    if (isRestarting) {
      this.deferredMatch = match;
      return;
    }

    if (!this.isMatchValid(match)) return;

    this.match = match;
    this.cancelReason = null;
    poll.stop();

    gameManager = GameManager.of(this, match);
    format =
        new TournamentFormatImpl(
            EventsPlugin.get().getTeamManager(),
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
                "ranked",
                cycleTime,
                AppData.matchStartDuration(),
                match.getMap().getName(),
                1,
                true,
                true));
    format.addRound(ranked);
    this.cycleTime = Duration.ofSeconds(5);

    Ingame.get()
        .getServer()
        .getPluginManager()
        .callEvent(new BoltMatchStatusChangeEvent(match, null, MatchStatus.CREATED));

    Bukkit.broadcastMessage(ChatColor.YELLOW + "A new match is starting on this server!");
    EventsPlugin.get()
        .getTournamentManager()
        .createTournament(PGM.get().getMatchManager().getMatches().next(), format);
  }

  private void updateMatch(BoltMatch newMatch, MatchStatus oldStatus) {
    BoltMatch oldMatch = this.match;
    if (oldMatch == null
        || newMatch == null
        || !Objects.equals(oldMatch.getId(), newMatch.getId())
        || !Objects.equals(newMatch.getId(), pgmMatchId)) {
      return;
    }

    this.match = newMatch;

    Ingame.get()
        .getServer()
        .getPluginManager()
        .callEvent(new BoltMatchResponseEvent(pgmMatch, oldMatch, newMatch, oldStatus));
  }

  private boolean isMatchValid(BoltMatch match) {
    return match != null
        && match.getId() != null
        && !match.getId().isEmpty()
        && match.getMap() != null
        && (match.getStatus().equals(MatchStatus.CREATED)
            || match.getStatus().equals(MatchStatus.LOADED)
            || match.getStatus().equals(MatchStatus.STARTED))
        && (this.match == null || !Objects.equals(this.match.getId(), match.getId()));
  }

  public BoltMatch getMatch() {
    return match;
  }

  public MatchSearch getPoll() {
    return poll;
  }

  public GameManager getGameManager() {
    return gameManager;
  }

  public Match getPGMMatch() {
    return pgmMatch;
  }

  public String getPGMMatchId() {
    return pgmMatchId;
  }

  public CancelReason getCancelReason() {
    return cancelReason;
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

  public void cancel(Match match, CancelReason reason) {
    this.cancelReason = reason;
    this.postMatchStatus(match, MatchStatus.CANCELLED);

    // Cancel countdowns if match has not started
    if (match.getPhase().equals(MatchPhase.STARTING)) {
      match.getCountdown().cancelAll();
    }

    // Check if match is in progress
    if (match.getPhase().equals(MatchPhase.RUNNING)) {
      // Add tie victory condition if in progress
      match.addVictoryCondition(new TieVictoryCondition());
      match.finish();
    }

    this.getPoll().startIn(Duration.ofSeconds(15));
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    // If match has not started set as current
    if (match != null && !match.getStatus().isFinished()) {
      this.pgmMatchId = this.match.getId();
    }

    this.pgmMatch = event.getMatch();

    postMatchStatus(event.getMatch(), MatchStatus.LOADED);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    postMatchStatus(event.getMatch(), MatchStatus.STARTED);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchFinish(MatchFinishEvent event) {
    if (cancelReason == null) {
      postMatchStatus(event.getMatch(), MatchStatus.ENDED);
    }

    poll.startIn(Duration.ofSeconds(30));
  }

  public void postMatchStatus(Match match, MatchStatus status) {
    if (this.match == null) return;

    Instant now = Instant.now();
    MatchStatus oldStatus = this.match.getStatus();

    Ingame.newSharedChain("match")
        .syncFirst(() -> transition(match, this.match, status, now))
        .abortIfNull()
        .async(Ingame.get().getApiManager()::postMatch)
        .syncLast(newMatch -> updateMatch(newMatch, oldStatus))
        .execute();
  }

  private BoltMatch transition(
      Match match, BoltMatch boltMatch, MatchStatus newStatus, Instant transitionAt) {
    MatchStatus oldStatus = boltMatch.getStatus();
    if (!oldStatus.canTransitionTo(newStatus)) return null;

    switch (newStatus) {
      case LOADED:
        break;
      case STARTED:
        boltMatch.setMap(PGMMapUtils.getBoltPGMMap(boltMatch, match));
        boltMatch.setStartedAt(transitionAt);
        break;
      case ENDED:
        statsManager.handleMatchUpdate(boltMatch, match);
        boltMatch.setEndedAt(transitionAt);
        Collection<Competitor> winners = match.getWinners();
        if (winners.size() == 1) {
          format
              .teamManager()
              .tournamentTeam(Iterables.getOnlyElement(winners))
              .map(t -> t instanceof ManagedTeam ? ((ManagedTeam) t).getBoltTeam() : t)
              .filter(t -> t instanceof Team)
              .map(t -> (Team) t)
              .ifPresent(winner -> boltMatch.setWinner(new Team(winner.getId())));
        }
        break;
      case CANCELLED:
        boltMatch.setEndedAt(transitionAt);
        break;
    }

    boltMatch.setStatus(newStatus);
    Ingame.get()
        .getServer()
        .getPluginManager()
        .callEvent(new BoltMatchStatusChangeEvent(boltMatch, oldStatus, newStatus));

    return boltMatch;
  }

  @EventHandler
  public void onRestartStart(StartRestartCountdownEvent event) {
    this.isRestarting = true;
  }

  @EventHandler
  public void onRestartCancel(CancelRestartEvent event) {
    this.isRestarting = false;
    if (deferredMatch != null) {
      setupMatch(deferredMatch);
      deferredMatch = null;
    }
  }
}
