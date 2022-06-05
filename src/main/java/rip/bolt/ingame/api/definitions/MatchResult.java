package rip.bolt.ingame.api.definitions;

import static rip.bolt.ingame.utils.Components.matchResult;

import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;

public enum MatchResult implements ComponentLike {
  WIN(matchResult(NamedTextColor.GREEN)),
  LOSS(matchResult(NamedTextColor.RED)),
  TIE(matchResult(NamedTextColor.YELLOW)),
  UNKNOWN(matchResult(NamedTextColor.GRAY));

  private final Component component;

  MatchResult(Component component) {
    this.component = component;
  }

  @Nonnull
  public Component asComponent() {
    return component;
  }
}
