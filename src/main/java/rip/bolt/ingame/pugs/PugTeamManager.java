package rip.bolt.ingame.pugs;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentTeamManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Participation;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.api.definitions.User;
import rip.bolt.ingame.api.definitions.pug.PugLobby;
import rip.bolt.ingame.api.definitions.pug.PugPlayer;
import rip.bolt.ingame.api.definitions.pug.PugTeam;
import rip.bolt.ingame.events.BoltMatchResponseEvent;
import rip.bolt.ingame.managers.MatchManager;
import tc.oc.pgm.api.party.Party;

public class PugTeamManager implements Listener {

  private final MatchManager matchManager;
  private final PugManager pugManager;
  private final TournamentTeamManager teamManager;

  private final Map<String, ManagedTeam> pugTeams;

  public PugTeamManager(MatchManager matchManager, PugManager pugManager) {
    this.matchManager = matchManager;
    this.pugManager = pugManager;
    this.teamManager = EventsPlugin.get().getTeamManager();

    this.pugTeams = new HashMap<>();
  }

  private PugLobby getLobby() {
    return pugManager.getLobby();
  }

  private List<PugTeam> getTeams() {
    return pugManager.getLobby().getTeams();
  }

  public ManagedTeam getTeam(String pugTeamId) {
    return pugTeams.get(pugTeamId);
  }

  public ManagedTeam getTeam(Integer boltTeamId) {
    for (ManagedTeam managedTeam : pugTeams.values()) {
      if (managedTeam.getBoltTeam() == null) continue;

      if (managedTeam.getBoltTeam().getId().equals(boltTeamId)) {
        return managedTeam;
      }
    }

    return null;
  }

  public ManagedTeam getTeam(@Nullable Party team) {
    for (ManagedTeam mt : pugTeams.values()) {
      if (team == null || mt.getPgmTeam() == team) return mt;
    }
    return null;
  }

  /**
   * Process cycling teams from an old match into a new one This method may add or remove teams,
   * unregister or register teams from events.
   */
  public void setupTeams(BoltMatch match) {
    Map<String, PugTeam> teams =
        getTeams().stream().collect(Collectors.toMap(PugTeam::getId, Function.identity()));

    List<String> toRemoveIds = new ArrayList<>(pugTeams.keySet());
    toRemoveIds.retainAll(teams.keySet());

    // Pain. EventsPlugin doesn't allow removing just one team, gotta remove them all and re-add.
    if (!toRemoveIds.isEmpty()) {
      teamManager.clear();
      for (String toRemoveId : toRemoveIds) {
        ManagedTeam mt = pugTeams.remove(toRemoveId);
        mt.clean();
      }
    }

    Iterator<Team> boltTeamsIt = match.getTeams().iterator();
    teams.forEach(
        (id, team) -> {
          ManagedTeam mt = pugTeams.computeIfAbsent(id, ManagedTeam::new);
          mt.clean();
          mt.setPugTeam(team);
          mt.setBoltTeam(boltTeamsIt.next());
          teamManager.addTeam(mt);
        });
  }

  public void syncMatchTeams() {
    // We have not cycled yet, just wait to sync
    if (!Objects.equals(matchManager.getPGMMatchId(), getLobby().getMatch().getId())) return;
    // Do not update teams after match end, before stats get reported
    if (getLobby().getMatch().getStatus().isFinished()) return;

    // Push all players on to correct team (if not already on)
    getTeams().forEach(this::syncMatchTeam);

    // Events plugin to sync in game players with stored teams
    this.teamManager.syncTeams();
  }

  private void syncMatchTeam(PugTeam lobbyTeam) {
    ManagedTeam mt = getTeam(lobbyTeam.getId());
    mt.setPugTeam(lobbyTeam);
    teamManager.fromTournamentTeam(mt).ifPresent(mt::setPgmTeam);

    // Sync team names
    String newTeamName = lobbyTeam.getName();
    String oldTeamName = mt.getBoltTeam().getName();
    if (!Objects.equals(oldTeamName, newTeamName)) {
      mt.getBoltTeam().setName(newTeamName);
      if (mt.getPgmTeam() != null) mt.getPgmTeam().setName(newTeamName);
    }

    // Change size if required
    if (mt.getPgmTeam() != null) {
      int curr = mt.getPgmTeam().getMaxPlayers();
      int wanted = lobbyTeam.getMaxPlayers();

      if (curr != wanted) mt.getPgmTeam().setMaxSize(wanted, wanted);
    }

    // Loop players and get current participation or create new
    List<Participation> participations =
        mt.getPlayers().stream()
            .map(player -> getOrCreate(mt.getBoltTeam(), player))
            .collect(Collectors.toList());

    // Clear team participation list and populate with new
    mt.getBoltTeam().getParticipations().clear();
    mt.getBoltTeam().getParticipations().addAll(participations);
  }

  private Participation getOrCreate(Team team, PugPlayer player) {
    return team.getParticipations().stream()
        .filter(participation -> participation.getUser().getUuid().equals(player.getUuid()))
        .findFirst()
        .orElseGet(() -> new Participation(new User(player.getUUID(), player.getUsername())));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onBoltMatchResponse(BoltMatchResponseEvent event) {
    event
        .getResponseMatch()
        .getTeams()
        .forEach(
            team -> {
              ManagedTeam mt = getTeam(team.getId());
              if (mt != null) mt.setBoltTeam(team);
            });

    syncMatchTeams();
  }
}
