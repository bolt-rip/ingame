package rip.bolt.ingame.commands;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.ranked.MatchStatus;
import rip.bolt.ingame.ranked.RankedManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.Audience;

public class RankedAdminCommands {

  @Command(
      aliases = "poll",
      desc = "Poll the API once for a new Bolt match",
      perms = "ingame.staff")
  public void poll(CommandSender sender, Match match) throws CommandException {
    if (match.getPhase() == MatchPhase.RUNNING)
      throw new CommandException(
          ChatColor.RED + "You may not run this command while a game is running!");

    RankedManager ranked = Ingame.get().getRankedManager();
    if (ranked == null)
      throw new CommandException(ChatColor.RED + "You are not in a ranked server!");

    Audience.get(sender)
        .sendMessage(
            text("Manual poll has been triggered, checking API for match.", NamedTextColor.GRAY));

    ranked.manualPoll();
  }

  @Command(
      aliases = {"clear", "reset"},
      desc = "Clear the currently stored Bolt match",
      perms = "ingame.staff")
  public void clear(CommandSender sender) throws CommandException {
    RankedManager ranked = Ingame.get().getRankedManager();
    if (ranked == null)
      throw new CommandException(ChatColor.RED + "You are not in a ranked server!");

    Audience.get(sender)
        .sendMessage(
            text(
                "Currently stored Bolt match "
                    + ranked.getMatch().getMatchId()
                    + " has been removed.",
                NamedTextColor.GRAY));

    ranked.manualReset();
  }

  @Command(
      aliases = "match",
      desc = "View info about the current Bolt match",
      perms = "ingame.staff")
  public void match(CommandSender sender) throws CommandException {
    RankedManager ranked = Ingame.get().getRankedManager();
    if (ranked == null)
      throw new CommandException(ChatColor.RED + "You are not in a ranked server!");

    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No match Bolt match currently loaded.");

    Audience.get(sender).sendMessage(text(boltMatch.toString(), NamedTextColor.GRAY));
  }

  @Command(
      aliases = "cancel",
      desc = "Report the current Bolt match as cancelled",
      perms = "ingame.staff")
  public void cancel(CommandSender sender, Match match) throws CommandException {
    RankedManager ranked = Ingame.get().getRankedManager();
    if (ranked == null)
      throw new CommandException(ChatColor.RED + "You are not in a ranked server!");

    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No match Bolt match currently loaded.");

    if (!boltMatch.getStatus().canTransitionTo(MatchStatus.CANCELLED)) {
      throw new CommandException(ChatColor.RED + "Unable to transition to the cancelled state.");
    }

    ranked.postMatchStatus(match, MatchStatus.CANCELLED);
    if (match.isRunning()) {
      match.finish();
    }

    Audience.get(sender)
        .sendMessage(
            text(
                "Match " + boltMatch.getMatchId() + " has been reported as cancelled.",
                NamedTextColor.GRAY));
    match.sendMessage(text("Match has been cancelled by an admin.", NamedTextColor.RED));
  }
}
