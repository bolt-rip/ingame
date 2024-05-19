package rip.bolt.ingame.commands;

import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import rip.bolt.ingame.api.definitions.pug.PugCommand;
import rip.bolt.ingame.api.definitions.pug.PugTeam;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.pugs.PugManager;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.FlagYielding;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.internal.CommandNode;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

@Command("pug")
public class PugCommands {

  private final MatchManager matchManager;
  private static Collection<String> commandList;

  public PugCommands(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  public TeamCommands getTeamCommands() {
    return new TeamCommands();
  }

  public static Collection<String> getCommandList() {
    return commandList;
  }

  public static void setupSubCommands(@Nullable CommandNode<CommandSender> pugNode) {
    if (pugNode == null) {
      commandList = Collections.emptyList();
      return;
    }

    commandList =
        pugNode.children().stream()
            .flatMap(n -> n.component().aliases().stream())
            .collect(Collectors.toList());
  }

  private PugManager needPugManager() {
    GameManager gm = matchManager.getGameManager();
    if (!(gm instanceof PugManager))
      throw exception("This command is only available on pug matches");
    return (PugManager) gm;
  }

  @Command("leave|obs|spectator|spec")
  @CommandDescription("Leave the match")
  public void leave(Player sender) {
    needPugManager().write(PugCommand.joinObs(sender));
  }

  @Command("join|play [team]")
  @CommandDescription("Join the match")
  public void join(MatchPlayer player, Match match, @Argument("team") @FlagYielding Party team) {
    PugManager pm = needPugManager();
    if (team != null && !(team instanceof Competitor)) {
      leave(player.getBukkit()); // This supports /join obs
      return;
    }

    // If no team is specified, find emptiest
    if (team == null) {
      final TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
      if (tmm != null)
        team = tmm.getEmptiestJoinableTeam(null, JoinRequest.fromPlayer(player, null)).getTeam();
    }

    PugTeam pugTeam = pm.findPugTeam(team);

    if (pugTeam != null) pm.write(PugCommand.joinTeam(player.getBukkit(), pugTeam));
    else throw exception("command.teamNotFound");
  }

  @Command("start|begin [duration]")
  @CommandDescription("Start the match")
  public void start(MatchPlayer sender, @Argument("duration") Duration duration) {
    needPugManager().write(PugCommand.startMatch(sender.getBukkit(), duration));
  }

  @Command("setnext|sn [map]")
  @CommandDescription("Change the next map")
  public void setNext(MatchPlayer sender, @Argument("map") @FlagYielding MapInfo map) {
    if (map == null) throw exception("Map not found!");
    needPugManager().write(PugCommand.setMap(sender.getBukkit(), map));
  }

  @Command("cycle [duration] [map]")
  @CommandDescription("Cycle to the next match")
  public void cycle(
      MatchPlayer sender,
      @Argument("duration") Duration duration,
      @Argument("map") @FlagYielding MapInfo map) {
    PugManager pm = needPugManager();
    if (map != null) pm.write(PugCommand.cycleMatch(sender.getBukkit(), map));
    else pm.write(PugCommand.cycleMatch(sender.getBukkit()));
  }

  @Command("recycle|rematch [duration]")
  @CommandDescription("Reload (cycle to) the current map")
  public void recycle(
      MatchManager matchManager, MatchPlayer sender, @Argument("duration") Duration duration) {
    PugManager pm = needPugManager();
    if (matchManager.getMatch() == null || matchManager.getMatch().getMap() == null) {
      throw exception("Could not find current map to recycle, use cycle instead");
    }

    pm.write(PugCommand.cycleMatch(sender.getBukkit(), matchManager.getMatch().getMap()));
  }

  @Command("pug team")
  public class TeamCommands {

    @Command("force <player> [team]")
    @CommandDescription("Force a player onto a team")
    public void force(
        MatchPlayer sender, @Argument("player") MatchPlayer player, @Argument("team") Party team) {
      PugManager pm = needPugManager();

      PugTeam pugTeam;
      if (team != null && !(team instanceof Competitor)) {
        pugTeam = null; // Moving  to obs
      } else {
        // Team could be null, for FFA, but it'd return a pugTeam regardless.
        pugTeam = pm.findPugTeam(team);
        if (pugTeam == null) throw exception("command.teamNotFound");
      }
      pm.write(PugCommand.movePlayer(sender.getBukkit(), player.getBukkit(), pugTeam));
    }

    @Command("balance")
    @CommandDescription("Balance teams according to MMR")
    public void balance(Player sender) {
      needPugManager().write(PugCommand.balance(sender));
    }

    @Command("shuffle")
    @CommandDescription("Shuffle players among the teams")
    public void shuffle(Player sender) {
      needPugManager().write(PugCommand.shuffle(sender));
    }

    @Command("clear")
    @CommandDescription("Clear all teams")
    public void clear(Player sender) {
      needPugManager().write(PugCommand.clear(sender));
    }

    @Command("alias <team> <name>")
    @CommandDescription("Rename a team")
    public void alias(
        MatchPlayer sender,
        @Argument("team") Party team,
        @Argument("name") @Greedy String newName) {
      PugManager pm = needPugManager();

      if (newName.length() > 32) newName = newName.substring(0, 32);

      if (!(team instanceof Team)) throw exception("command.teamNotFound");

      PugTeam pugTeam = pm.findPugTeam(team);
      if (pugTeam == null) throw exception("command.teamNotFound");
      pm.write(PugCommand.setTeamName(sender.getBukkit(), pugTeam, newName));
    }

    @Command("size <team> <max-players>")
    @CommandDescription("Set the max players on a team")
    public void size(
        Player sender, @Argument("team") String ignore, @Argument("max-players") Integer max) {
      PugManager pm = needPugManager();
      int teams = pm.getLobby().getTeams().size();
      pm.write(PugCommand.setTeamSize(sender, max * teams));
    }
  }
}
