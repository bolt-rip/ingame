package rip.bolt.ingame.ready;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.start.StartCountdown;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.teams.Team;

import java.time.Duration;

public class ReadyManager {

    private final ReadyParties readyParties;
    private final ReadySystem readySystem;

    public ReadyManager(ReadySystem readySystem, ReadyParties readyParties) {
        this.readyParties = readyParties;
        this.readySystem = readySystem;
    }

    public ReadyParties getReadyParties() {
        return readyParties;
    }

    public ReadySystem getReadySystem() {
        return readySystem;
    }

    public boolean playerTeamFull(Party party) {
        if (party instanceof Team) {
            Team team = (Team) party;

            return team.getSize(false) >= team.getMaxPlayers();
        }

        return false;
    }

    public void readyTeam(Party party) {
        if (party.isNamePlural()) {
            Bukkit.broadcastMessage(party.getColor() + party.getNameLegacy() + ChatColor.RESET + " are now ready.");
        } else {
            Bukkit.broadcastMessage(party.getColor() + party.getNameLegacy() + ChatColor.RESET + " is now ready.");
        }

        readyParties.ready(party);

        Match match = party.getMatch();
        if (readyParties.allReady(match))
            match.needModule(StartMatchModule.class).forceStartCountdown(Duration.ofSeconds(20), Duration.ZERO);
    }

    public void unreadyTeam(Party party) {
        if (party.isNamePlural()) {
            Bukkit.broadcastMessage(party.getColor() + party.getNameLegacy() + ChatColor.RESET + " are now unready.");
        } else {
            Bukkit.broadcastMessage(party.getColor() + party.getNameLegacy() + ChatColor.RESET + " is now unready.");
        }

        Match match = party.getMatch();
        if (readyParties.allReady(match)) {
            readyParties.unReady(party);
            if (readySystem.unreadyShouldCancel()) {
                // check if unready should cancel
                match.getCountdown().cancelAll(StartCountdown.class);
            }
        } else {
            readyParties.unReady(party);
        }
    }

}
