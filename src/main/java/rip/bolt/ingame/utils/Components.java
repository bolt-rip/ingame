package rip.bolt.ingame.utils;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

public class Components {

  private static final String MATCH_ICON = "\u29bf";

  public static Component command(Style style, String command, String... args) {
    StringBuilder builder = new StringBuilder();

    if (!command.startsWith("/")) builder.append("/");
    builder.append(command);
    for (String arg : args) builder.append(" ").append(Components.toArgument(arg));
    command = builder.toString();

    return Component.text(command, style)
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, command))
        .hoverEvent(
            Component.text("Click to run ", NamedTextColor.GREEN)
                .append(Component.text(command, style)));
  }

  public static Component link(Style style, String url) {
    return Component.text(url, style)
        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url))
        .hoverEvent(
            Component.text("Click to visit ", NamedTextColor.YELLOW)
                .append(Component.text(url, style)));
  }

  static String toArgument(String input) {
    if (input == null) return null;
    return input.replace(" ", "┈");
  }

  public static Component matchResult(TextColor color) {
    return text(MATCH_ICON, color);
  }
}
