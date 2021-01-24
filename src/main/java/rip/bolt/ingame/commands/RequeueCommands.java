package rip.bolt.ingame.commands;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import net.md_5.bungee.api.ChatColor;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltResponse;
import rip.bolt.ingame.config.AppData;
import rip.bolt.ingame.ranked.MatchStatus;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.lib.app.ashcon.intake.CommandException;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;

public class RequeueCommands {

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

    Ingame.newChain()
        .asyncFirst(() -> Ingame.get().getApiManager().postPlayerRequeue(sender.getId()))
        .syncLast(response -> sendResponse(sender, response))
        .execute();
  }

  private void sendResponse(MatchPlayer player, BoltResponse response) {
    player.sendMessage(
        text(
            response.getMessage(),
            response.getSuccess() ? NamedTextColor.GREEN : NamedTextColor.RED));
  }
}
