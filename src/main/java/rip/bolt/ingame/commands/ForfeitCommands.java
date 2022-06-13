package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.ranked.ForfeitManager;
import rip.bolt.ingame.ranked.RankedManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

public class ForfeitCommands extends BaseCommand {

  @Dependency private RankedManager ranked;
  @Dependency private ForfeitManager forfeits;

  @CommandAlias("forfeit|ff")
  @Description("Accept that you have no chance of winning")
  public void forfeit(MatchPlayer sender, Match match) {
    if (!AppData.forfeitEnabled())
      throw new InvalidCommandArgument(
          ChatColor.RED + "The forfeit command is not enabled on this server.", false);

    if (match.getPhase() != MatchPhase.RUNNING)
      throw new InvalidCommandArgument(
          ChatColor.RED + "You may only run this command during a match.", false);

    if (!(sender.getParty() instanceof Competitor))
      throw new InvalidCommandArgument(
          ChatColor.RED + "Only match players are able to run this command.", false);

    Competitor team = (Competitor) sender.getParty();
    if (!forfeits.mayForfeit(team))
      throw new InvalidCommandArgument(
          ChatColor.YELLOW + "It's too early to forfeit this match, you can still win!", false);

    ForfeitManager.ForfeitPoll poll = forfeits.getForfeitPoll(team);

    if (poll.getVoted().contains(sender.getId()))
      throw new InvalidCommandArgument(
          ChatColor.RED + "You have already voted to forfeit this match.", false);

    sender.sendMessage(text("You have voted to forfeit this match."));
    poll.addVote(sender);
  }
}
