package rip.bolt.ingame.utils;

import static rip.bolt.ingame.utils.Components.command;
import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.newline;
import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

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
}
