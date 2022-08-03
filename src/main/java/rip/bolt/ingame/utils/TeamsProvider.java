package rip.bolt.ingame.utils;

import static tc.oc.pgm.util.text.TextException.exception;

import java.lang.annotation.Annotation;
import java.util.List;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.lib.app.ashcon.intake.argument.CommandArgs;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import tc.oc.pgm.teams.TeamMatchModule;

public final class TeamsProvider implements BukkitProvider<TeamMatchModule> {

  @Override
  public boolean isProvided() {
    return true;
  }

  @Override
  public TeamMatchModule get(
      CommandSender sender, CommandArgs commandArgs, List<? extends Annotation> list) {
    final Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match != null) {
      final TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams != null) {
        return teams;
      }
    }

    throw exception("command.noTeams");
  }
}
