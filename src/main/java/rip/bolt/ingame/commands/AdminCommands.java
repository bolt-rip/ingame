package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import javax.annotation.Nullable;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.MatchStatus;
import rip.bolt.ingame.api.definitions.Punishment;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.managers.GameManager;
import rip.bolt.ingame.managers.MatchManager;
import rip.bolt.ingame.pugs.PugManager;
import rip.bolt.ingame.utils.CancelReason;
import rip.bolt.ingame.utils.Messages;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Switch;
import tc.oc.pgm.lib.app.ashcon.intake.parametric.annotation.Text;
import tc.oc.pgm.util.Audience;

public class AdminCommands {

  private final MatchManager matchManager;

  public AdminCommands(MatchManager ranked) {
    this.matchManager = ranked;
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

    matchManager.manualPoll(repeat);

    Audience.get(sender)
        .sendMessage(
            text("Manual poll has been triggered, checking API for match.", NamedTextColor.GRAY));
  }

  @Command(
      aliases = {"clear", "reset"},
      desc = "Clear the currently stored Bolt match",
      perms = "ingame.staff.clear")
  public void clear(CommandSender sender) throws CommandException {
    BoltMatch match = matchManager.getMatch();
    if (match == null)
      throw new CommandException(
          ChatColor.RED + "Unable to clear as no ranked match currently stored.");

    matchManager.manualReset();

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
    BoltMatch boltMatch = matchManager.getMatch();
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
    GameManager gameTypeManager = matchManager.getGameManager();
    String gameManager = gameTypeManager.getClass().getSimpleName();
    TextComponent managerType =
        text("Game manager is ", NamedTextColor.GRAY)
            .append(text(gameManager, NamedTextColor.AQUA));

    boolean polling = matchManager.getPoll().isSyncTaskRunning();
    TextComponent apiPolling =
        text("API polling is ", NamedTextColor.GRAY)
            .append(
                text(
                    polling ? "running" : "not running",
                    polling ? NamedTextColor.GREEN : NamedTextColor.RED));

    boolean websocket = false;
    if (gameTypeManager instanceof PugManager) {
      websocket = ((PugManager) gameTypeManager).getBoltWebSocket().isOpen();
    }

    TextComponent websocketConnected =
        text("Websocket is ", NamedTextColor.GRAY)
            .append(
                text(
                    websocket ? "connected" : "not connected",
                    websocket ? NamedTextColor.GREEN : NamedTextColor.RED));

    Audience.get(sender)
        .sendMessage(
            managerType.append(
                newline().append(apiPolling.append(newline().append(websocketConnected)))));
  }

  @Command(
      aliases = "cancel",
      desc = "Report the current Bolt match as cancelled",
      perms = "ingame.staff.cancel")
  public void cancel(CommandSender sender, Match match) throws CommandException {
    BoltMatch boltMatch = matchManager.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No Bolt match currently loaded.");

    if (!boltMatch.getStatus().canTransitionTo(MatchStatus.CANCELLED)) {
      throw new CommandException(ChatColor.RED + "Unable to transition to the cancelled state.");
    }

    matchManager.cancel(match, CancelReason.MANUAL_CANCEL);

    Audience.get(sender)
        .sendMessage(
            text(
                "Match " + boltMatch.getId() + " has been reported as cancelled.",
                NamedTextColor.GRAY));
    match.sendMessage(text("Match has been cancelled by an admin.", NamedTextColor.RED));
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

  @Command(
      aliases = "reconnect",
      desc = "Reconnect to the matches websocket",
      perms = "ingame.staff.reconnect")
  public void reconnect(CommandSender sender) throws CommandException {
    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof PugManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    Audience.get(sender)
        .sendMessage(text("Reconnecting to match websocket.. ", NamedTextColor.GRAY));

    ((PugManager) gameManager).connect(matchManager.getMatch());
  }

  @Command(
      aliases = "disconnect",
      desc = "Disconnect from the matches websocket",
      perms = "ingame.staff.reconnect")
  public void disconnect(CommandSender sender) throws CommandException {
    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof PugManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    Audience.get(sender)
        .sendMessage(text("Disconnecting from match websocket.. ", NamedTextColor.GRAY));

    ((PugManager) gameManager).disconnect();
  }
}
