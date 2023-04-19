package rip.bolt.ingame.pugs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import dev.pgm.events.EventsPlugin;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.jetbrains.annotations.Nullable;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.pug.PugCommand;
import rip.bolt.ingame.api.definitions.pug.PugLobby;
import rip.bolt.ingame.api.definitions.pug.PugMatch;
import rip.bolt.ingame.api.definitions.pug.PugPlayer;
import rip.bolt.ingame.api.definitions.pug.PugState;
import rip.bolt.ingame.api.definitions.pug.PugTeam;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.utils.CancelReason;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;

public class PugManager extends GameManager {

  private final ObjectMapper objectMapper;
  private final String wsUrl;
  private final PugListener listener;

  private final PugTeamManager teamManager;

  private BoltWebSocket boltWebSocket;
  private PugLobby pugLobby;

  private boolean newConnection = true;

  private PugManager(MatchManager matchManager) {
    super(matchManager);

    this.objectMapper = Ingame.get().getApiManager().objectMapper;
    this.wsUrl = AppData.Socket.getUrl();

    this.listener = new PugListener(this);
    this.teamManager = new PugTeamManager(matchManager, this);
  }

  public static PugManager of(MatchManager matchManager) {
    if (matchManager.getGameManager() instanceof PugManager) {
      PugManager existing = (PugManager) matchManager.getGameManager();
      if (existing.pugLobby.getId().equals(matchManager.getMatch().getLobbyId())) return existing;
    }
    return new PugManager(matchManager);
  }

  @Override
  public void enable(MatchManager manager) {
    super.enable(manager);
    connect(manager.getMatch());

    Bukkit.getPluginManager().registerEvents(listener, Ingame.get());
    Bukkit.getPluginManager().registerEvents(teamManager, Ingame.get());

    EventsPlugin.get().getTeamManager().clear();
  }

  @Override
  public void setup(BoltMatch match) {
    if (this.pugLobby != null) teamManager.setupTeams(match);
  }

  @Override
  public void disable() {
    super.disable();
    HandlerList.unregisterAll(listener);
    HandlerList.unregisterAll(teamManager);

    this.disconnect();
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public BoltWebSocket getBoltWebSocket() {
    return boltWebSocket;
  }

  public PugLobby getLobby() {
    return pugLobby;
  }

  public String getLobbyId() {
    return pugLobby != null ? pugLobby.getId() : null;
  }

  public void connect(BoltMatch boltMatch) {
    if (this.getLobbyId() != null && !Objects.equals(boltMatch.getLobbyId(), getLobbyId()))
      disconnect();

    // Check if match is a pug
    if (boltMatch.getLobbyId() == null) return;

    boltWebSocket = new BoltWebSocket(URI.create(wsUrl + "/pugs/" + boltMatch.getLobbyId()), this);
    boltWebSocket.addHeader("Authorization", "Bearer " + AppData.API.getKey());
    boltWebSocket.connect();

    System.out.println("[Ingame] Connected to " + boltMatch.getLobbyId());
  }

  public void reset() {
    this.newConnection = true;
  }

  public void write(PugCommand command) {
    try {
      this.boltWebSocket.send(objectMapper.writeValueAsString(command));
    } catch (JsonProcessingException | WebsocketNotConnectedException e) {
      e.printStackTrace();
    }
  }

  public void disconnect() {
    if (boltWebSocket == null) return;

    System.out.println("[Ingame] Disconnected from " + getLobbyId());

    boltWebSocket.close();
  }

  public void syncPugLobby(JsonNode newLobby) throws IOException {
    if (newLobby == null) return;

    if (this.pugLobby == null) {
      this.pugLobby = objectMapper.treeToValue(newLobby, PugLobby.class);
      setup(matchManager.getMatch());
    } else {
      ObjectReader objectReader = objectMapper.readerForUpdating(this.pugLobby);
      this.pugLobby = objectReader.readValue(newLobby);
    }

    MatchStatus status = matchManager.getMatch().getStatus();
    PugMatch pugMatch = this.pugLobby.getMatch();

    if (newConnection && pugLobby.getState() != PugState.FINISHED) {
      newConnection = false;
      syncOnline();

      // We have just reconnected to the WS, let the pug know about the current match status.
      // If it's old it'll get ignored.
      write(PugCommand.setMatchStatus(this.matchManager.getMatch()));
    }

    // Avoid updating status if there is no match
    if (pugMatch == null || this.pugLobby.getState() == PugState.FINISHED) {
      if (matchManager.getMatch() != null && !matchManager.getMatch().getStatus().isFinished())
        matchManager.cancel(matchManager.getPGMMatch(), CancelReason.MANUAL_CANCEL);

      this.boltWebSocket.close(CloseFrame.NORMAL, "Pug is in finished status");

      // Set the game manager to Noop when pug is closed
      matchManager.setGameManager(GameManager.of(matchManager, null));

      return;
    }

    if (!Objects.equals(matchManager.getMatch().getId(), pugMatch.getId())) {
      if (!matchManager.getMatch().getStatus().isFinished())
        matchManager.cancel(matchManager.getPGMMatch(), CancelReason.MANUAL_CANCEL);

      // Setup the new match and cycle to it
      this.matchManager.setupMatch(
          new BoltMatch(
              matchManager.getMatch().getLobbyId(), matchManager.getMatch().getSeries(), pugMatch));

      return;
    }

    // Detect match cancellation
    if (pugMatch.getStatus().equals(MatchStatus.CANCELLED)) {
      if (!matchManager.getMatch().getStatus().isFinished())
        matchManager.cancel(matchManager.getPGMMatch(), CancelReason.MANUAL_CANCEL);
    }

    // Stop processing 'reactive' components
    if (status.isFinished()) return;

    // Start match countdown if required
    syncMatchStart(status, pugMatch);

    if (pugLobby.getTeams() != null) teamManager.syncMatchTeams();
  }

  public PugTeam findPugTeam(@Nullable Party team) {
    ManagedTeam mt = teamManager.getTeam(team);
    return mt == null ? null : mt.getPugTeam();
  }

  public void syncMatchTeams() {
    teamManager.syncMatchTeams();
  }

  private void syncMatchStart(MatchStatus status, PugMatch pugMatch) {
    if (!status.equals(MatchStatus.LOADED)) return;

    Instant newStart = pugMatch.getStartedAt();

    // Check if started at are same (no change needed)
    if (Objects.equals(matchManager.getMatch().getStartedAt(), newStart)) return;
    matchManager.getMatch().setStartedAt(newStart);

    Match match = matchManager.getPGMMatch();

    // Cancel start by sending a null start time
    if (newStart == null) {
      match.getCountdown().cancelAll(StartCountdown.class);
      return;
    }

    // Always at least 5s start. Round up to 4s up, or 1s down, to keep a multiple of 5s.
    long startingInMillis = Math.max(Duration.between(Instant.now(), newStart).toMillis(), 5_000);
    Duration startIn = Duration.ofSeconds(5 * ((startingInMillis + 4_000) / 5_000));
    match.needModule(StartMatchModule.class).forceStartCountdown(startIn, Duration.ZERO);
  }

  private void syncOnline() {
    List<PugPlayer> lobbyPlayers = this.pugLobby.getPlayers();

    Set<UUID> onlinePlayers =
        Bukkit.getServer().getOnlinePlayers().stream()
            .map(p -> syncPlayerStatus(p, true))
            .filter(Objects::nonNull)
            .map(PugPlayer::getUuid)
            .collect(Collectors.toSet());

    lobbyPlayers.stream()
        .filter(pugPlayer -> !onlinePlayers.contains(pugPlayer.getUuid()))
        .forEach(lobbyPlayer -> syncPlayerStatus(lobbyPlayer, false));
  }

  private void syncPlayerStatus(PugPlayer pugPlayer, boolean online) {
    write(PugCommand.setPlayerStatus(pugPlayer, online));
  }

  public PugPlayer syncPlayerStatus(Player player, boolean online) {
    // Do not send online status for vanished players
    if (online && Integration.isVanished(player)) return null;

    PugPlayer pugPlayer = new PugPlayer(player.getUniqueId(), player.getName());
    syncPlayerStatus(pugPlayer, online);
    return pugPlayer;
  }

  public void reconnect() {
    // Stop trying if lobby has changed
    if (pugLobby.getId() != null
        && !Objects.equals(matchManager.getMatch().getLobbyId(), pugLobby.getId())) return;

    if (boltWebSocket.isOpen()) return;

    new Thread(
            () -> {
              try {
                Thread.sleep(5000L);
              } catch (InterruptedException ignore) {
              }

              boltWebSocket.reconnect();
            })
        .start();
  }
}
