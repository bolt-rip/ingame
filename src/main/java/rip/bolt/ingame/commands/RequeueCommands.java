package rip.bolt.ingame.commands;

import org.bukkit.ChatColor;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.ranked.RankedManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;

public class RequeueCommands {

  @Command("requeue")
  @CommandDescription("Requeue for another ranked match")
  public void requeue(MatchManager matchManager, MatchPlayer sender, Match match)
      throws CommandException {
    if (!AppData.allowRequeue()) {
      throw new CommandException(
          ChatColor.RED + "The requeue command is not enabled on this server.");
    }

    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof RankedManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    RankedManager manager = (RankedManager) gameManager;

    boolean finished = match.getPhase() == MatchPhase.FINISHED;
    boolean cancelled =
        Ingame.get().getMatchManager().getMatch().getStatus().equals(MatchStatus.CANCELLED);

    if (!(finished || cancelled))
      throw new CommandException(
          ChatColor.RED + "You may only run this command after a match has ended.");

    manager.getRequeueManager().requestRequeue(sender);
  }
}
