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
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.github.paperspigot.PaperSpigotConfig;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltKnockback;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.Messages;
import rip.bolt.ingame.utils.PGMMapUtils;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.util.Audience;

public class RankedManager implements Listener {

  private final PlayerWatcher playerWatcher;
  private final RequeueManager requeueManager;
  private final RankManager rankManager;
  private final StatsManager statsManager;
  private final SpectatorManager spectatorManager;
  private final TabManager tabManager;
  private final MatchSearch poll;

  private TournamentFormat format;
  private BoltMatch match;

  private Duration cycleTime = Duration.ofSeconds(0);
  private CancelReason cancelReason = null;

  public RankedManager(Plugin plugin) {
    playerWatcher = new PlayerWatcher(this);
    requeueManager = new RequeueManager();
    rankManager = new RankManager(this);
    statsManager = new StatsManager(this);
    spectatorManager = new SpectatorManager(playerWatcher);
    tabManager = new TabManager(plugin);

    MatchPreloader.create();

    poll = new MatchSearch(this::setupMatch);
    poll.startIn(Duration.ofSeconds(5));
  }

  public void setupMatch(BoltMatch match) {
    if (!this.isServerReady()) return;
    if (!this.isMatchValid(match)) return;

    this.match = match;
    this.cancelReason = null;
    poll.stop();

    Tournament.get().getTeamManager().clear();
    for (TournamentTeam team : match.getTeams()) Tournament.get().getTeamManager().addTeam(team);

    playerWatcher.addPlayers(
        match.getTeams().stream()
            .flatMap(team -> team.getPlayers().stream())
            .map(TournamentPlayer::getUUID)
            .collect(Collectors.toList()));

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
                "ranked",
                cycleTime,
                AppData.matchStartDuration(),
                match.getMap().getName(),
                1,
                true,
                true));
    cycleTime = Duration.ofSeconds(5);
    format.addRound(ranked);

    Ingame.get()
        .getServer()
        .getPluginManager()
        .callEvent(new BoltMatchStatusChangeEvent(match, null, MatchStatus.CREATED));

    BoltKnockback knockback = match.getSeries().getKnockback();
    setupKnockback(knockback);

    Bukkit.broadcastMessage(ChatColor.YELLOW + "A new match is starting on this server!");
    Tournament.get()
        .getTournamentManager()
        .createTournament(PGM.get().getMatchManager().getMatches().next(), format);
  }

  private void setupKnockback(@Nullable BoltKnockback knockback) {
    if (knockback == null) knockback = BoltKnockback.defaults();

    PaperSpigotConfig.knockbackFriction = knockback.getFriction();
    PaperSpigotConfig.knockbackHorizontal = knockback.getHorizontal();
    PaperSpigotConfig.knockbackVertical = knockback.getVertical();
    PaperSpigotConfig.knockbackVerticalLimit = knockback.getVerticalLimit();
    PaperSpigotConfig.knockbackExtraHorizontal = knockback.getExtraHorizontal();
    PaperSpigotConfig.knockbackExtraVertical = knockback.getExtraVertical();
  }

  private void updateMatch(BoltMatch newMatch) {
    BoltMatch oldMatch = this.match;
    if (oldMatch == null
        || newMatch == null
        || !Objects.equals(oldMatch.getId(), newMatch.getId())
        || newMatch.getStatus() != MatchStatus.ENDED) {
      return;
    }

    this.match = newMatch;
    rankManager.handleMatchUpdate(oldMatch, newMatch);
  }

  private boolean isMatchValid(BoltMatch match) {
    return match != null
        && match.getId() != null
        && !match.getId().isEmpty()
        && match.getMap() != null
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

  public RequeueManager getRequeueManager() {
    return requeueManager;
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
    } else {
      // Prompt players to requeue and start polling
      Audience.get(match.getCompetitors()).sendMessage(Messages.requeue());
      this.getPoll().startIn(Duration.ofSeconds(15));
    }
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    postMatchStatus(event.getMatch(), MatchStatus.LOADED);
  }

  @EventHandler(priority = EventPriority.MONITOR)
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
            () ->
                event.getMatch().getCompetitors().stream()
                    .map(Competitor::getPlayers)
                    .flatMap(Collection::stream)
                    .forEach(requeueManager::sendRequeueMessage),
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
    if (boltMatch == null) return null;

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

  public SpectatorManager getSpectatorManager() {
    return spectatorManager;
  }
}
