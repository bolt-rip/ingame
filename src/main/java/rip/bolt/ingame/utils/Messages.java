package rip.bolt.ingame.utils;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static rip.bolt.ingame.utils.Components.command;
import static rip.bolt.ingame.utils.Components.link;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.player.MatchPlayer;

public class Messages {

  public static Component requeue() {
    return text("You can queue for another match using ", NamedTextColor.GREEN)
        .append(command(Style.style(NamedTextColor.YELLOW, TextDecoration.UNDERLINED), "requeue"));
  }

  public static Component forfeit() {
    return text("A teammate left the match for too long.", NamedTextColor.GRAY)
        .append(newline())
        .append(
            text("You can vote to give up using ", NamedTextColor.GREEN)
                .append(
                    command(
                        Style.style(NamedTextColor.YELLOW, TextDecoration.UNDERLINED), "forfeit")));
  }

  public static Component matchStartCancelled() {
    return text("Match could not be started due to lack of players.", NamedTextColor.RED)
        .append(newline())
        .append(
            text("The offending player(s) have received a temporary ban.", NamedTextColor.GRAY));
  }

  public static Component participationBan() {
    return text("Player(s) temporarily banned due to lack of participation.", NamedTextColor.GRAY);
  }

  public static Component matchLink(BoltMatch match) {
    String url = AppData.Web.getMatchLink().replace("{matchId}", match.getId());

    return text("Match link: ", NamedTextColor.WHITE)
        .append(link(Style.style(NamedTextColor.BLUE, TextDecoration.UNDERLINED), url));
  }

  public static Component profileLink(MatchPlayer player) {
    String url = AppData.Web.getProfileLink().replace("{name}", player.getNameLegacy());

    return text("Profile link: ", NamedTextColor.WHITE)
        .append(link(Style.style(NamedTextColor.BLUE, TextDecoration.UNDERLINED), url));
  }
}
