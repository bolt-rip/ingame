package rip.bolt.ingame.utils;

import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.text.event.ClickEvent;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.Style;

public class Components {

  public static Component command(Style style, String command, String... args) {
    if (!command.startsWith("/")) command = "/" + command;
    for (String arg : args) command += " " + Components.toArgument(arg);

    return Component.text(command, style)
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
        .hoverEvent(
            Component.text("Click to run ", NamedTextColor.GREEN)
                .append(Component.text(command, style)));
  }

  static String toArgument(String input) {
    if (input == null) return null;
    return input.replace(" ", "┈");
  }
}
