package rip.bolt.ingame.api.definitions;

import static rip.bolt.ingame.utils.Components.match;

import javax.annotation.Nonnull;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.text.ComponentLike;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;

public enum MatchResult implements ComponentLike {
  WIN(match(true, NamedTextColor.GREEN)),
  LOSS(match(true, NamedTextColor.RED)),
  TIE(match(true, NamedTextColor.YELLOW)),
  UNKNOWN(match(false, NamedTextColor.DARK_GRAY));

  private final Component component;

  MatchResult(Component component) {
    this.component = component;
  }

  @Nonnull
  public Component asComponent() {
    return component;
  }
}
