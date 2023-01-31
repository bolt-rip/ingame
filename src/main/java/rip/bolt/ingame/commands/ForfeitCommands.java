package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.text;

import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.ranked.RankedManager;
import rip.bolt.ingame.ranked.forfeit.ForfeitManager;
import rip.bolt.ingame.ranked.forfeit.ForfeitPoll;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;

public class ForfeitCommands {

  @CommandMethod("forfeit|ff")
  @CommandDescription("Accept that you have no chance of winning")
  public void forfeit(MatchManager matchManager, MatchPlayer sender, Match match)
      throws CommandException {
    if (!AppData.forfeitEnabled())
      throw new CommandException(
          ChatColor.RED + "The forfeit command is not enabled on this server.");

    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof RankedManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    RankedManager rankedManager = (RankedManager) gameManager;
    ForfeitManager forfeits = rankedManager.getPlayerWatcher().getForfeitManager();

    if (match.getPhase() != MatchPhase.RUNNING)
      throw new CommandException(ChatColor.RED + "You may only run this command during a match.");

    if (!(sender.getParty() instanceof Competitor))
      throw new CommandException(
          ChatColor.RED + "Only match players are able to run this command.");

    Competitor team = (Competitor) sender.getParty();
    if (!forfeits.mayForfeit(team))
      throw new CommandException(
          ChatColor.YELLOW + "It's too early to forfeit this match, you can still win!");

    ForfeitPoll poll = forfeits.getForfeitPoll(team);

    if (poll.getVoted().contains(sender.getId()))
      throw new CommandException(ChatColor.RED + "You have already voted to forfeit this match.");

    sender.sendMessage(text("You have voted to forfeit this match."));
    poll.addVote(sender);
  }
}
