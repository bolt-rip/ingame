package rip.bolt.ingame.ready;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.app.ashcon.intake.Command;
import tc.oc.pgm.match.ObservingParty;

public class ReadyCommands {

    private final ReadyManager readyManger;
    private final ReadyParties readyParties;
    private final ReadySystem readySystem;

    public ReadyCommands(ReadyManager readyManager) {
        this.readyManger = readyManager;
        this.readyParties = readyManager.getReadyParties();
        this.readySystem = readyManager.getReadySystem();
    }

    @Command(aliases = "ready", desc = "Ready up")
    public void readyCommand(CommandSender sender, Match match, MatchPlayer player) {
        readyParties.preconditionsCheckMatch(match);

        if (!preConditions(match))
            return;

        if (!canReady(sender, player))
            return;

        if (readyParties.isReady(player.getParty())) {
            sender.sendMessage(ChatColor.RED + "You are already ready!");
            return;
        }

        if (!readyManger.playerTeamFull(player.getParty())) {
            sender.sendMessage(ChatColor.RED + "You can not ready until your team is full!");
            return;
        }

        readyManger.readyTeam(player.getParty());
    }

    @Command(aliases = "unready", desc = "Mark your team as no longer being ready")
    public void unreadyCommand(CommandSender sender, Match match, MatchPlayer player) {
        readyParties.preconditionsCheckMatch(match);

        if (!preConditions(match))
            return;

        if (!canReady(sender, player))
            return;

        if (!readyParties.isReady(player.getParty())) {
            sender.sendMessage(ChatColor.RED + "You are already unready!");
            return;
        }

        readyManger.unreadyTeam(player.getParty());
    }

    private boolean preConditions(Match match) {
        return !match.isRunning() && !match.isFinished();
    }

    private boolean canReady(CommandSender sender, MatchPlayer player) {
        if (!readySystem.canReadyAction()) {
            sender.sendMessage(ChatColor.RED + "You are not able to ready at this time!");
            return false;
        }

        if (!AppData.observersMustReady() && player.getParty() instanceof ObservingParty) {
            sender.sendMessage(ChatColor.RED + "Observers are not allowed to ready!");
            return false;
        }

        return !(player.getParty() instanceof ObservingParty) || sender.hasPermission("ingame.staff");
    }

}
