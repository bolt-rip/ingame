package rip.bolt.ingame.commands;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.ranked.RankedManager;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.Audience;

public class RankedAdminCommands {

  @Command(aliases = "poll", desc = "Poll the API for a new game", perms = "ingame.staff")
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
}
