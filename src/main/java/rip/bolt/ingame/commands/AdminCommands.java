package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.Greedy;
import tc.oc.pgm.util.Audience;

@CommandMethod("ingame")
public class AdminCommands {

  @CommandMethod("poll")
  @CommandDescription("Poll the API once for a new Bolt match")
  @CommandPermission("ingame.staff.poll")
  public void poll(
      MatchManager matchManager,
      CommandSender sender,
      Match match,
      @Flag(value = "repeat", aliases = "r") boolean repeat)
      throws CommandException {
    if (match.getPhase() == MatchPhase.RUNNING)
      throw new CommandException(
          ChatColor.RED + "You may not run this command while a game is running!");

    matchManager.manualPoll(repeat);

    Audience.get(sender)
        .sendMessage(
            text("Manual poll has been triggered, checking API for match.", NamedTextColor.GRAY));
  }

  @CommandMethod("clear|reset")
  @CommandDescription("Clear the currently stored Bolt match")
  @CommandPermission("ingame.staff.clear")
  public void clear(MatchManager matchManager, CommandSender sender) throws CommandException {
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

  @CommandMethod("match")
  @CommandDescription("View info about the current Bolt match")
  @CommandPermission("ingame.staff.match")
  public void match(MatchManager matchManager, CommandSender sender) throws CommandException {
    BoltMatch boltMatch = matchManager.getMatch();
    if (boltMatch == null)
      throw new CommandException(ChatColor.RED + "No Bolt match currently loaded.");

    Audience audience = Audience.get(sender);
    audience.sendMessage(text(boltMatch.toString(), NamedTextColor.GRAY));
    if (AppData.Web.getMatch() != null) audience.sendMessage(Messages.matchLink(boltMatch));
  }

  @CommandMethod("status")
  @CommandDescription("View the status of the API polling")
  @CommandPermission("ingame.staff.status")
  public void status(MatchManager matchManager, CommandSender sender) throws CommandException {
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

  @CommandMethod("cancel")
  @CommandDescription("Report the current Bolt match as cancelled")
  @CommandPermission("ingame.staff.cancel")
  public void cancel(MatchManager matchManager, CommandSender sender, Match match)
      throws CommandException {
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

  @CommandMethod("ban <player> [reason]")
  @CommandDescription("Manually queue bans a player")
  @CommandPermission("ingame.staff.ban")
  public void ban(
      CommandSender sender,
      @Argument("player") MatchPlayer target,
      @Argument("reason") @Greedy String reason) {
    Audience.get(sender)
        .sendMessage(text(target.getName() + " has been queue banned.", NamedTextColor.GRAY));

    Punishment punishment = new Punishment(target.getBukkit().getUniqueId(), sender, reason);

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Ingame.get(), () -> Ingame.get().getApiManager().postPlayerPunishment(punishment));
  }

  @CommandMethod("reconnect")
  @CommandDescription("Reconnect to the matches websocket")
  @CommandPermission("ingame.staff.reconnect")
  public void reconnect(MatchManager matchManager, CommandSender sender) throws CommandException {
    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof PugManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    Audience.get(sender)
        .sendMessage(text("Reconnecting to match websocket.. ", NamedTextColor.GRAY));

    ((PugManager) gameManager).connect(matchManager.getMatch());
  }

  @CommandMethod("disconnect")
  @CommandDescription("Disconnect from the matches websocket")
  @CommandPermission("ingame.staff.reconnect")
  public void disconnect(MatchManager matchManager, CommandSender sender) throws CommandException {
    GameManager gameManager = matchManager.getGameManager();
    if (!(gameManager instanceof PugManager))
      throw new CommandException(ChatColor.RED + "The current match type does not support that.");

    Audience.get(sender)
        .sendMessage(text("Disconnecting from match websocket.. ", NamedTextColor.GRAY));

    ((PugManager) gameManager).disconnect();
  }
}
