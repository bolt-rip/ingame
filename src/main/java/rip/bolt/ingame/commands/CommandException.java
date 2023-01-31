package rip.bolt.ingame.commands;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.jetbrains.annotations.Nullable;

public class CommandException extends RuntimeException implements ComponentMessageThrowable {

  public CommandException(String s) {
    super(s);
  }

  @Override
  public @Nullable Component componentMessage() {
    return text(this.getMessage());
  }
}
