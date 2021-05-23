package rip.bolt.ingame.commands;

import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import rip.bolt.ingame.api.definitions.pug.PugCommand;
import rip.bolt.ingame.api.definitions.pug.PugTeam;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.pugs.PugManager;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.Type;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.annotation.Sender;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Default;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Text;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

public class PugCommands {

  private final MatchManager matchManager;

  private Collection<String> commandList;

  public PugCommands(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  public TeamCommands getTeamCommands() {
    return new TeamCommands();
  }

  public Collection<String> getCommandList() {
    return commandList;
  }

  public void setCommandList(Collection<String> commandList) {
    this.commandList = commandList;
  }

  private PugManager needPugManager() {
    GameManager gm = matchManager.getGameManager();
    if (!(gm instanceof PugManager))
      throw exception("This command is only available on pug matches");
    return (PugManager) gm;
  }

  @Command(
      aliases = {"leave", "obs", "spectator", "spec"},
      desc = "Leave the match",
      perms = Permissions.LEAVE)
  public void leave(@Sender Player sender) {
    needPugManager().write(PugCommand.joinObs(sender));
  }

  @Command(
      aliases = {"join", "play"},
      desc = "Join the match",
      usage = "[team] - defaults to random")
  public void join(@Sender MatchPlayer player, Match match, @Nullable Party team) {
    PugManager pm = needPugManager();
    if (team != null && !(team instanceof Competitor)) {
      leave(player.getBukkit()); // This supports /join obs
      return;
    }

    // If no team is specified, find emptiest
    if (team == null) {
      final TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
      if (tmm != null) team = tmm.getEmptiestJoinableTeam(player, false).getTeam();
    }

    PugTeam pugTeam = pm.findPugTeam(team);

    if (pugTeam != null) pm.write(PugCommand.joinTeam(player.getBukkit(), pugTeam));
    else throw exception("command.teamNotFound");
  }

  @Command(
      aliases = {"start", "begin"},
      desc = "Start the match")
  public void start(@Sender Player sender, @Default("20s") Duration duration) {
    needPugManager().write(PugCommand.startMatch(sender, duration));
  }

  @Command(
      aliases = {"setnext", "sn"},
      desc = "Change the next map",
      usage = "[map name]")
  public void setNext(@Sender Player sender, @Fallback(Type.NULL) @Text MapInfo map) {
    if (map == null) throw exception("Map not found!");
    needPugManager().write(PugCommand.setMap(sender, map));
  }

  @Command(aliases = "cycle", desc = "Cycle to the next match")
  public void cycle(
      @Sender Player sender, @Default("5s") Duration duration, @Nullable MapInfo map) {
    PugManager pm = needPugManager();
    if (map != null) pm.write(PugCommand.cycleMatch(sender, map));
    else pm.write(PugCommand.cycleMatch(sender));
  }

  @Command(aliases = "recycle", desc = "Reload (cycle to) the current map", usage = "[seconds]")
  public void recycle(@Sender Player sender, @Default("5s") Duration duration) {
    PugManager pm = needPugManager();
    if (matchManager.getMatch() == null || matchManager.getMatch().getMap() == null) {
      throw exception("Could not find current map to recycle, use cycle instead");
    }

    pm.write(PugCommand.cycleMatch(sender, matchManager.getMatch().getMap()));
  }

  public class TeamCommands {

    @Command(aliases = "force", desc = "Force a player onto a team")
    public void force(@Sender Player sender, Player player, @Nullable Party team) {
      PugManager pm = needPugManager();

      PugTeam pugTeam;
      if (team != null && !(team instanceof Competitor)) {
        pugTeam = null; // Moving  to obs
      } else {
        // Team could be null, for FFA, but it'd return a pugTeam regardless.
        pugTeam = pm.findPugTeam(team);
        if (pugTeam == null) throw exception("command.teamNotFound");
      }
      pm.write(PugCommand.movePlayer(sender, player, pugTeam));
    }

    @Command(
        aliases = {"balance"},
        desc = "Balance teams according to MMR")
    public void balance(@Sender Player sender) {
      needPugManager().write(PugCommand.balance(sender));
    }

    @Command(
        aliases = {"shuffle"},
        desc = "Shuffle players among the teams")
    public void shuffle(@Sender Player sender) {
      needPugManager().write(PugCommand.shuffle(sender));
    }

    @Command(
        aliases = {"clear"},
        desc = "Clear all teams")
    public void clear(@Sender Player sender) {
      needPugManager().write(PugCommand.clear(sender));
    }

    @Command(
        aliases = {"alias"},
        desc = "Rename a team",
        usage = "<old name> <new name>")
    public void alias(@Sender Player sender, Party team, @Text String newName) {
      PugManager pm = needPugManager();

      if (newName.length() > 32) newName = newName.substring(0, 32);

      if (!(team instanceof Team)) throw exception("command.teamNotFound");

      PugTeam pugTeam = pm.findPugTeam(team);
      if (pugTeam == null) throw exception("command.teamNotFound");
      pm.write(PugCommand.setTeamName(sender, pugTeam, newName));
    }

    @Command(
        aliases = {"size"},
        desc = "Set the max players on a team",
        usage = "<*> <max-players>")
    public void size(@Sender Player sender, String ignore, Integer max) {
      PugManager pm = needPugManager();
      int teams = pm.getLobby().getTeams().size();
      pm.write(PugCommand.setTeamSize(sender, max * teams));
    }
  }
}
