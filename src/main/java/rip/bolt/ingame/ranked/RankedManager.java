package rip.bolt.ingame.ranked;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;
import java.util.Collection;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.events.BoltMatchResponseEvent;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.ranked.forfeit.PlayerWatcher;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

public class RankedManager extends GameManager {

  private final PlayerWatcher playerWatcher;
  private final SpectatorManager spectatorManager;
  private final RequeueManager requeueManager;

  public RankedManager(MatchManager matchManager) {
    super(matchManager);

    this.playerWatcher = new PlayerWatcher(matchManager);
    this.spectatorManager = new SpectatorManager(playerWatcher);
    this.requeueManager = new RequeueManager();
  }

  @Override
  public void enable(MatchManager manager) {
    super.enable(manager);

    Bukkit.getPluginManager().registerEvents(this.playerWatcher, Ingame.get());
    Bukkit.getPluginManager().registerEvents(this.spectatorManager, Ingame.get());
    Bukkit.getPluginManager().registerEvents(this.requeueManager, Ingame.get());
  }

  @Override
  public void setup(BoltMatch match) {
    EventsPlugin.get().getTeamManager().clear();
    for (TournamentTeam team : match.getTeams()) EventsPlugin.get().getTeamManager().addTeam(team);
    playerWatcher.addPlayers(
        match.getTeams().stream()
            .flatMap(team -> team.getPlayers().stream())
            .map(TournamentPlayer::getUUID)
            .collect(Collectors.toList()));
  }

  @Override
  public void disable() {
    super.disable();
    HandlerList.unregisterAll(this.playerWatcher);
    HandlerList.unregisterAll(this.spectatorManager);
    HandlerList.unregisterAll(this.requeueManager);
  }

  public PlayerWatcher getPlayerWatcher() {
    return playerWatcher;
  }

  public SpectatorManager getSpectatorManager() {
    return spectatorManager;
  }

  public RequeueManager getRequeueManager() {
    return requeueManager;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBoltMatchResponse(BoltMatchResponseEvent event) {
    if (!AppData.allowRequeue()) return;

    if (event.hasMatchFinished()) {
      sendRequeueMessage(event.getPgmMatch());
    }
  }

  private void sendRequeueMessage(Match match) {
    match.getCompetitors().stream()
        .map(Competitor::getPlayers)
        .flatMap(Collection::stream)
        .forEach(requeueManager::sendRequeueMessage);
  }
}
