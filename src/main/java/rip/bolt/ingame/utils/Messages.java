package rip.bolt.ingame.utils;

import static rip.bolt.ingame.utils.Components.command;
import static rip.bolt.ingame.utils.Components.link;
import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.newline;
import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.Style;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.TextDecoration;

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

  public static Component matchLink(BoltMatch match) {
    String url = AppData.Web.getMatch().replace("{matchId}", match.getId());

    return text("Match link: ", NamedTextColor.WHITE)
        .append(link(Style.style(NamedTextColor.BLUE, TextDecoration.UNDERLINED), url));
  }

  public static Component profileLink(MatchPlayer player) {
    String url = AppData.Web.getProfile().replace("{name}", player.getNameLegacy());

    return text("Profile link: ", NamedTextColor.WHITE)
        .append(link(Style.style(NamedTextColor.BLUE, TextDecoration.UNDERLINED), url));
  }

  public static Component[] loadoutEditorTutorial() {
    return new Component[] {
      text("Click on two items to swap them.", NamedTextColor.YELLOW),
      text("Close your inventory to save your changes.", NamedTextColor.YELLOW)
    };
  }
}
