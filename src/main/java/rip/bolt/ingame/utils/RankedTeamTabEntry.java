package rip.bolt.ingame.utils;

import dev.pgm.events.Tournament;
import dev.pgm.events.team.TournamentTeam;
import java.util.Optional;
import javax.annotation.Nullable;
import rip.bolt.ingame.api.definitions.Team;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.tablist.TeamTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class RankedTeamTabEntry extends TeamTabEntry {

  @Nullable private Component mmrComponent;
  private tc.oc.pgm.teams.Team team;

  public RankedTeamTabEntry(tc.oc.pgm.teams.Team team) {
    super(team);

    this.team = team;
  }

  @Override
  public Component getContent(TabView view) {
    Component base = super.getContent(view);

    if (mmrComponent == null) mmrComponent = getMmrComponent();
    if (mmrComponent != null) return base.append(mmrComponent);

    return base;
  }

  private Component getMmrComponent() {
    Optional<TournamentTeam> tournamentTeam =
        Tournament.get().getTeamManager().tournamentTeam(team);

    return tournamentTeam
        .filter(t -> t instanceof Team)
        .map(t -> (Team) t)
        .map(t -> Component.text("  " + t.getMmr(), NamedTextColor.GRAY, TextDecoration.ITALIC))
        .orElse(null);
  }
}
