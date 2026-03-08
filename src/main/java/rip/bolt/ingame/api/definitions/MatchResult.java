package rip.bolt.ingame.api.definitions;

import static rip.bolt.ingame.utils.Components.matchResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

public enum MatchResult implements ComponentLike {
  WIN(matchResult(NamedTextColor.GREEN)),
  LOSS(matchResult(NamedTextColor.RED)),
  TIE(matchResult(NamedTextColor.YELLOW)),
  UNKNOWN(matchResult(NamedTextColor.GRAY));

  private final Component component;

  MatchResult(Component component) {
    this.component = component;
  }

  @NonNull
  public Component asComponent() {
    return component;
  }
}
