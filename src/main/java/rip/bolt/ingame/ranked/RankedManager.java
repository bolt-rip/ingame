package rip.bolt.ingame.ranked;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class RankedManager implements Listener {

    private boolean cycledToRightMap;

    private TournamentFormat format;
    private BoltMatch match;

    private PlayerWatcher playerWatcher;

    private Runnable pollTask;
    private int pollTaskId = -1;
    
    private Duration cycleTime = Duration.ofSeconds(0);

    public RankedManager() {

        playerWatcher = new PlayerWatcher(this);

        pollTask = new Runnable() {
            
            @Override
            public void run() {
                if (setupMatch()) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "A new match is starting on this server!");
                    format.nextRound(PGM.get().getMatchManager().getMatches().next());
                }
            }
        };

        // fetch match
        setupMatch();

        // we use an async task otherwise the server will not start
        // pgm loads the world in the main thread using a task
        // createMatch(String).get() is blocking
        // bukkit won't be able to complete the load world task on the main thread
        // since this task will be blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(Ingame.get(), new Runnable() {

            @Override
            public void run() {
                try {
                    PGM.get().getMatchManager().createMatch(null).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    public boolean setupMatch() {
        match = Ingame.get().getApiManager().fetchMatchData();
        if (match == null) {
            if (!cycledToRightMap && pollTaskId == -1) // we haven't played a game on this server yet
                pollTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Tournament.get(), pollTask, 15 * 20, 15 * 20); // every 15 seconds
            return false;
        }

        if (pollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pollTaskId);
            pollTaskId = -1;
        }

        Tournament.get().getTeamManager().clear();
        for (TournamentTeam team : match.getTeams())
            Tournament.get().getTeamManager().addTeam(team);

        playerWatcher.addPlayers(match.getTeams().stream().flatMap(team -> team.getPlayers().stream()).map(TournamentPlayer::getUUID).collect(Collectors.toList()));

        format = new TournamentFormatImpl(Tournament.get().getTeamManager(), new TournamentRoundOptions(false, false, true, Duration.ofMinutes(30), Duration.ofSeconds(30), Duration.ofSeconds(40), new BestOfCalculation<>(1)), new RoundReferenceHolder());
        SingleRound ranked = new SingleRound(format, new SingleRoundOptions("ranked", cycleTime, Duration.ofSeconds(300), match.getMap(), 1, true, true));
        cycleTime = Duration.ofSeconds(5);
        format.addRound(ranked);

        return true;
    }

    public BoltMatch getMatch() {
        return match;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMatchLoad(MatchLoadEvent event) {
        if (!cycledToRightMap && format != null) {
            format.nextRound(event.getMatch());
            cycledToRightMap = true;
        }
    }

    @EventHandler
    public void onMatchFinish(MatchFinishEvent event) {
        if (event.getWinner() != null) {
            match.getWinners().add(event.getWinner().getNameLegacy());
        }

        // run async to stop server lag
        Bukkit.getScheduler().runTaskAsynchronously(Tournament.get(), new Runnable() {

            @Override
            public void run() {
                Ingame.get().getApiManager().postMatchData(match);
            }

        });

        pollTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Tournament.get(), pollTask, 30 * 20, 30 * 20); // every 30 seconds
    }

    @EventHandler
    public void onPlayerRunCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/tm") || event.getMessage().toLowerCase().startsWith("/tm ") || event.getMessage().equalsIgnoreCase("/tourney") || event.getMessage().toLowerCase().startsWith("/tourney ") || event.getMessage().equalsIgnoreCase("/tournament") || event.getMessage().toLowerCase().startsWith("/tournament ")) {
            // Allow staff to run tm commands.
            if (!event.getPlayer().hasPermission("ingame.staff")) {
                event.getPlayer().sendMessage(ChatColor.RED + "This command is disabled in Ranked.");
                event.setCancelled(true);
            }
        }
    }

    public void poll() {
        pollTask.run();
    }

    public PlayerWatcher getPlayerWatcher() {
        return playerWatcher;
    }
}
