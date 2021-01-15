package rip.bolt.ingame.ranked;

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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class RankedManager implements Listener {

  private final PlayerWatcher playerWatcher;
  private final MatchSearch poll;

  private TournamentFormat format;
  private BoltMatch match;

  private Duration cycleTime = Duration.ofSeconds(0);

  public RankedManager() {

    playerWatcher = new PlayerWatcher(this);

    poll = new MatchSearch(this::setupMatch);
    poll.startIn(Duration.ofSeconds(15));

    // we use an async task otherwise the server will not start
    // pgm loads the world in the main thread using a task
    // createMatch(String).get() is blocking
    // bukkit won't be able to complete the load world task on the main thread
    // since this task will be blocking the main thread
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(),
            () -> {
              try {
                PGM.get().getMatchManager().createMatch(null).get();
              } catch (InterruptedException e) {
                e.printStackTrace();
              } catch (ExecutionException e) {
                e.printStackTrace();
              }
            });
  }

  public void setupMatch(BoltMatch match) {
    if (this.match != null && this.match.getMatchId().equals(match.getMatchId())) {
      return;
    }

    this.match = match;
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
                "ranked", cycleTime, Duration.ofSeconds(300), match.getMap(), 1, true, true));
    cycleTime = Duration.ofSeconds(5);
    format.addRound(ranked);

    Bukkit.broadcastMessage(ChatColor.YELLOW + "A new match is starting on this server!");
    Tournament.get()
        .getTournamentManager()
        .createTournament(PGM.get().getMatchManager().getMatches().next(), format);
  }

  public BoltMatch getMatch() {
    return match;
  }

  public PlayerWatcher getPlayerWatcher() {
    return playerWatcher;
  }

  public void manualPoll() {
    poll.trigger(true);
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    match.setMap(event.getMatch().getMap().getName());
    match.setStartedAt(Instant.now());

    // run async to stop server lag
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Tournament.get(), () -> Ingame.get().getApiManager().postMatchStart(match));
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    match.setEndedAt(Instant.now());
    if (event.getWinner() != null) {
      match.setWinner(event.getWinner().getNameLegacy());
    }

    // run async to stop server lag
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Tournament.get(), () -> Ingame.get().getApiManager().postMatchEnd(match));

    poll.startIn(Duration.ofSeconds(30));
  }
}
