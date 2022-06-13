package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.text;

import app.ashcon.intake.Command;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.Punishment;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.ranked.MatchStatus;
import rip.bolt.ingame.ranked.RankedManager;
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.util.Audience;

@CommandAlias("ingame")
public class RankedAdminCommands extends BaseCommand {

  @Dependency private RankedManager ranked;

  @Subcommand("poll")
  @Description("Poll the API once for a new Bolt match")
  @CommandPermission("ingame.staff.poll")
  public void poll(CommandSender sender, Match match, @Default("false") boolean repeat) {
    if (match.getPhase() == MatchPhase.RUNNING)
      throw new InvalidCommandArgument(
          ChatColor.RED + "You may not run this command while a game is running!", false);

    ranked.manualPoll(repeat);

    Audience.get(sender)
        .sendMessage(
            text("Manual poll has been triggered, checking API for match.", NamedTextColor.GRAY));
  }

  @Subcommand("clear|reset")
  @Description("Clear the currently stored Bolt match")
  @CommandPermission("ingame.staff.clear")
  public void clear(CommandSender sender) {
    BoltMatch match = ranked.getMatch();
    if (match == null)
      throw new InvalidCommandArgument(
          ChatColor.RED + "Unable to clear as no ranked match currently stored.", false);

    ranked.manualReset();

    Audience.get(sender)
        .sendMessage(
            text(
                "Currently stored Bolt match " + match.getId() + " has been removed.",
                NamedTextColor.GRAY));
  }

  @Subcommand("match")
  @Description("View info about the current Bolt match")
  @CommandPermission("ingame.staff.match")
  public void match(CommandSender sender) {
    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new InvalidCommandArgument(ChatColor.RED + "No Bolt match currently loaded.", false);

    Audience audience = Audience.get(sender);
    audience.sendMessage(text(boltMatch.toString(), NamedTextColor.GRAY));
    if (AppData.Web.getMatch() != null) audience.sendMessage(Messages.matchLink(boltMatch));
  }

  @Command(
      aliases = "status",
      desc = "View the status of the API polling",
      perms = "ingame.staff.status")
  @Subcommand("status")
  @Description("View the status of the API polling")
  @CommandPermission("ingame.staff.status")
  public void status(CommandSender sender) {
    boolean polling = ranked.getPoll().isSyncTaskRunning();

    Audience.get(sender)
        .sendMessage(
            text("API polling is ", NamedTextColor.GRAY)
                .append(
                    text(
                        polling ? "running" : "not running",
                        polling ? NamedTextColor.GREEN : NamedTextColor.RED)));
  }

  @Command(
      aliases = "cancel",
      desc = "Report the current Bolt match as cancelled",
      perms = "ingame.staff.cancel")
  @Subcommand("cancel")
  @Description("Report the current Bolt match as cancelled")
  @CommandPermission("ingame.staff.cancel")
  public void cancel(CommandSender sender, Match match) {
    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new InvalidCommandArgument(ChatColor.RED + "No Bolt match currently loaded.", false);

    if (!boltMatch.getStatus().canTransitionTo(MatchStatus.CANCELLED)) {
      throw new InvalidCommandArgument(
          ChatColor.RED + "Unable to transition to the cancelled state.", false);
    }

    ranked.cancel(match, CancelReason.MANUAL_CANCEL);

    Audience.get(sender)
        .sendMessage(
            text(
                "Match " + boltMatch.getId() + " has been reported as cancelled.",
                NamedTextColor.GRAY));
    match.sendMessage(text("Match has been cancelled by an admin.", NamedTextColor.RED));
  }

  @Subcommand("ban")
  @Description("Manually queue bans a player")
  @CommandPermission("ingame.staff.ban")
  public void ban(CommandSender sender, @Flags("other") Player target, @Optional String reason) {
    Audience.get(sender)
        .sendMessage(text(target.getName() + " has been queue banned.", NamedTextColor.GRAY));

    Punishment punishment = new Punishment(target.getUniqueId(), sender, reason);

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(), () -> Ingame.get().getApiManager().postPlayerPunishment(punishment));
  }
}
