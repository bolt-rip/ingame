package rip.bolt.ingame.utils;

import java.lang.annotation.Annotation;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.lib.app.ashcon.intake.argument.CommandArgs;
import tc.oc.pgm.lib.app.ashcon.intake.argument.MissingArgumentException;
import tc.oc.pgm.lib.app.ashcon.intake.bukkit.parametric.provider.BukkitProvider;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.ProvisionException;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextException;

public final class PartyProvider implements BukkitProvider<Party> {
  public PartyProvider() {}

  public String getName() {
    return "team";
  }

  public Party get(CommandSender sender, CommandArgs args, List<? extends Annotation> list)
      throws MissingArgumentException, ProvisionException {
    String text = args.next();
    Match match = PGM.get().getMatchManager().getMatch(sender);
    if (match == null) {
      throw TextException.exception("command.onlyPlayers", new Component[0]);
    } else if (text.startsWith("obs")) {
      return match.getDefaultParty();
    } else {
      TeamMatchModule teams = match.getModule(TeamMatchModule.class);
      if (teams == null) {
        throw TextException.exception("command.noTeams", new Component[0]);
      } else {
        Team team = teams.bestFuzzyMatch(text);
        if (team == null) {
          throw TextException.invalidFormat(text, Team.class, null);
        } else {
          return team;
        }
      }
    }
  }
}
