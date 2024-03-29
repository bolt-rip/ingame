package rip.bolt.ingame.pugs;

import static net.kyori.adventure.text.Component.empty;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.pug.PugCommand;
import rip.bolt.ingame.api.definitions.pug.PugTeam;
import rip.bolt.ingame.commands.PugCommands;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.events.BoltMatchResponseEvent;
import rip.bolt.ingame.events.BoltMatchStatusChangeEvent;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.teams.events.TeamResizeEvent;

public class PugListener implements Listener {

  private final PugManager pugManager;
  private final Set<UUID> transitioningPlayers;

  public PugListener(PugManager pugManager) {
    this.pugManager = pugManager;

    transitioningPlayers = new HashSet<>();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoinLow(PlayerJoinEvent event) {
    transitioningPlayers.add(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    transitioningPlayers.remove(event.getPlayer().getUniqueId());
    pugManager.syncPlayerStatus(event.getPlayer(), true);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuitLow(PlayerQuitEvent event) {
    transitioningPlayers.add(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    transitioningPlayers.remove(event.getPlayer().getUniqueId());
    pugManager.syncPlayerStatus(event.getPlayer(), false);
  }

  @EventHandler
  public void onPlayerVanish(PlayerVanishEvent event) {
    if (transitioningPlayers.contains(event.getPlayer().getId())) return;
    pugManager.syncPlayerStatus(event.getPlayer().getBukkit(), !event.isVanished());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchCycleEvent(MatchLoadEvent event) {
    pugManager.syncMatchTeams();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    // A bit hacky, but we don't have another way to know if it's global chat.
    if (event.getFormat() == null || !event.getFormat().equals("<%s>: %s")) return;
    Player p = event.getPlayer();
    if (p == null) return;
    pugManager.write(PugCommand.sendMessage(p, event.getMessage()));
  }

  @EventHandler
  public void onBoltMatchStateChange(BoltMatchStatusChangeEvent event) {
    BoltMatch match = event.getBoltMatch();
    System.out.println("[Ingame] <- Match ID " + match.getId());
    System.out.println("[Ingame] <- Match Status: " + match.getStatus());

    pugManager.write(PugCommand.setMatchStatus(match));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onBoltMatchResponse(BoltMatchResponseEvent event) {
    BoltMatch newMatch = event.getResponseMatch();
    if (!event.hasMatchFinished() || newMatch.getStatus() != MatchStatus.ENDED) return;

    Match match = event.getPgmMatch();
    match.callEvent(new MatchStatsEvent(match, true, true));

    if (AppData.Web.getMatchLink() != null) {
      match.sendMessage(Messages.matchLink(newMatch));
      match.sendMessage(empty());
    }
  }

  @EventHandler
  public void onTeamChangeSize(TeamResizeEvent event) {
    int newMax = event.getTeam().getMaxPlayers();
    if (pugManager.getLobby().getTeams().stream().allMatch(pt -> pt.getMaxPlayers() == newMax))
      return;
    pugManager.write(
        PugCommand.setTeamSize(null, newMax * pugManager.getLobby().getTeams().size()));
  }

  @EventHandler
  public void onTeamRename(PartyRenameEvent event) {
    if (!(event.getParty() instanceof Competitor)) return;
    PugTeam pugTeam = pugManager.findPugTeam(event.getParty());
    if (pugTeam == null || pugTeam.getName().equals(event.getParty().getNameLegacy())) return;

    String newName = event.getParty().getNameLegacy();
    if (newName.length() > 32) newName = newName.substring(0, 32);
    pugManager.write(PugCommand.setTeamName(null, pugTeam, newName));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onParticipate(PlayerParticipationStartEvent event) {
    if (!event.isCancelled()) return;

    // Events should expose this constant. It'll still be dirty, but will survive updates.
    if (!isMessage(event.getCancelReason(), "You may not join in a tournament setting!")) return;
    event.cancel(Component.empty());

    // Events probably cancelled the join
    PugTeam team = pugManager.findPugTeam(event.getCompetitor());
    if (team != null) pugManager.write(PugCommand.joinTeam(event.getPlayer().getBukkit(), team));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onLeaveParticipate(PlayerParticipationStopEvent event) {
    if (!event.isCancelled()) return;

    // Events should expose this constant. It'll still be dirty, but will survive updates.
    if (!isMessage(event.getCancelReason(), "You may not leave in a tournament setting!")) return;
    event.cancel(Component.empty());

    Party nextParty = event.getNextParty();

    if (nextParty instanceof Competitor) {
      PugTeam team = pugManager.findPugTeam(nextParty);
      if (team != null) pugManager.write(PugCommand.joinTeam(event.getPlayer().getBukkit(), team));
    } else if (nextParty != null) {
      pugManager.write(PugCommand.joinObs(event.getPlayer().getBukkit()));
    }
  }

  private boolean isMessage(Component component, String msg) {
    if (!(component instanceof TextComponent)) return false;
    TextComponent textComponent = (TextComponent) component;
    return textComponent.content().equals(msg);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerPGMCommand(PlayerCommandPreprocessEvent event) {
    String cmd = event.getMessage().substring(1).toLowerCase(Locale.ROOT);

    for (String command : PugCommands.getCommandList()) {
      if (cmd.startsWith(command)) {
        event.setMessage("/pug " + event.getMessage().substring(1));
        return;
      }
    }
  }

  // TODO handle cycles as new matches.

}
