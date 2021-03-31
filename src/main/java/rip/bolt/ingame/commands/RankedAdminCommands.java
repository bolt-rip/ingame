package rip.bolt.ingame.commands;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import java.time.Duration;
import javax.annotation.Nullable;
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
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Switch;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Text;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.util.Audience;

public class RankedAdminCommands {

  private final RankedManager ranked;

  public RankedAdminCommands(RankedManager ranked) {
    this.ranked = ranked;
  }

  @Command(
      aliases = "poll",
      desc = "Poll the API once for a new Bolt match",
      perms = "ingame.staff.poll",
      flags = "r")
  public void poll(CommandSender sender, Match match, @Switch('r') boolean repeat)
      throws CommandException {
    if (match.getPhase() == MatchPhase.RUNNING)
      throw new CommandException(
          ChatColor.RED + "You may not run this command while a game is running!");

    ranked.manualPoll(repeat);

    Audience.get(sender)
        .sendMessage(
            text("Manual poll has been triggered, checking API for match.", NamedTextColor.GRAY));
  }

  @Command(
      aliases = {"clear", "reset"},
      desc = "Clear the currently stored Bolt match",
      perms = "ingame.staff.clear")
  public void clear(CommandSender sender) throws CommandException {
    BoltMatch match = ranked.getMatch();
    if (match == null)
      throw new CommandException(
          ChatColor.RED + "Unable to clear as no ranked match currently stored.");

    ranked.manualReset();

    Audience.get(sender)
        .sendMessage(
            text(
                "Currently stored Bolt match " + match.getId() + " has been removed.",
                NamedTextColor.GRAY));
  }

  @Command(
      aliases = "match",
      desc = "View info about the current Bolt match",
      perms = "ingame.staff.match")
  public void match(CommandSender sender) throws CommandException {
    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No Bolt match currently loaded.");

    Audience audience = Audience.get(sender);
    audience.sendMessage(text(boltMatch.toString(), NamedTextColor.GRAY));
    if (AppData.Web.getMatch() != null) audience.sendMessage(Messages.matchLink(boltMatch));
  }

  @Command(
      aliases = "status",
      desc = "View the status of the API polling",
      perms = "ingame.staff.status")
  public void status(CommandSender sender) throws CommandException {
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
  public void cancel(CommandSender sender, Match match) throws CommandException {
    BoltMatch boltMatch = ranked.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No Bolt match currently loaded.");

    if (!boltMatch.getStatus().canTransitionTo(MatchStatus.CANCELLED)) {
      throw new CommandException(ChatColor.RED + "Unable to transition to the cancelled state.");
    }

    ranked.manualCancel(match);

    if (match.getPhase().equals(MatchPhase.STARTING)) {
      match.getCountdown().cancelAll();
    }

    boolean running = match.getPhase().canTransitionTo(MatchPhase.FINISHED);
    if (running) {
      match.addVictoryCondition(new TieVictoryCondition());
      match.finish();
    }

    Audience.get(sender)
        .sendMessage(
            text(
                "Match " + boltMatch.getId() + " has been reported as cancelled.",
                NamedTextColor.GRAY));
    match.sendMessage(text("Match has been cancelled by an admin.", NamedTextColor.RED));

    if (!running) {
      Audience.get(match.getCompetitors()).sendMessage(Messages.requeue());
      ranked.getPoll().startIn(Duration.ofSeconds(15));
    }
  }

  @Command(aliases = "ban", desc = "Manually queue bans a player", perms = "ingame.staff.ban")
  public void ban(CommandSender sender, Player target, @Text @Nullable String reason) {
    Audience.get(sender)
        .sendMessage(text(target.getName() + " has been queue banned.", NamedTextColor.GRAY));

    Punishment punishment = new Punishment(target.getUniqueId(), sender, reason);

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(), () -> Ingame.get().getApiManager().postPlayerPunishment(punishment));
  }
}
