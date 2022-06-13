package rip.bolt.ingame.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.ranked.MatchStatus;
import rip.bolt.ingame.ranked.RequeueManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;

public class RequeueCommands extends BaseCommand {

  @Dependency private RequeueManager requeue;

  @CommandAlias("requeue")
  @Description("Requeue for another ranked match")
  public void requeue(MatchPlayer sender, Match match) {
    if (!AppData.allowRequeue()) {
      throw new InvalidCommandArgument(
          ChatColor.RED + "The requeue command is not enabled on this server.", false);
    }

    boolean finished = match.getPhase() == MatchPhase.FINISHED;
    boolean cancelled =
        Ingame.get().getRankedManager().getMatch().getStatus().equals(MatchStatus.CANCELLED);

    if (!(finished || cancelled))
      throw new InvalidCommandArgument(
          ChatColor.RED + "You may only run this command after a match has ended.", false);

    requeue.requestRequeue(sender);
  }
}
