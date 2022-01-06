package rip.bolt.ingame.commands;

import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.ranked.MatchStatus;
import rip.bolt.ingame.ranked.RankedManager;
import rip.bolt.ingame.ranked.RequeueManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;

public class RequeueCommands {

  private final RequeueManager requeue;

  public RequeueCommands(RankedManager ranked) {
    this.requeue = ranked.getRequeueManager();
  }

  @Command(aliases = "requeue", desc = "Requeue for another ranked match")
  public void requeue(MatchPlayer sender, Match match) throws CommandException {
    if (!AppData.allowRequeue()) {
      throw new CommandException(
          ChatColor.RED + "The requeue command is not enabled on this server.");
    }

    boolean finished = match.getPhase() == MatchPhase.FINISHED;
    boolean cancelled =
        Ingame.get().getRankedManager().getMatch().getStatus().equals(MatchStatus.CANCELLED);

    if (!(finished || cancelled))
      throw new CommandException(
          ChatColor.RED + "You may only run this command after a match has ended.");

    requeue.requestRequeue(sender);
  }
}
