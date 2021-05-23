package rip.bolt.ingame.utils;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.team.TournamentTeam;
import java.util.Optional;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import rip.bolt.ingame.api.definitions.Team;
import tc.oc.pgm.tablist.TeamTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class RankedTeamTabEntry extends TeamTabEntry {

  @Nullable private Component mmrComponent;
  private final tc.oc.pgm.teams.Team team;

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
        EventsPlugin.get().getTeamManager().tournamentTeam(team);

    return tournamentTeam
        .filter(t -> t instanceof Team)
        .map(t -> (Team) t)
        .map(Team::getMmr)
        .filter(mmr -> !mmr.isEmpty() && !mmr.equals("0"))
        .map(this::getMmrComponent)
        .orElse(null);
  }

  private TextComponent getMmrComponent(String mmr) {
    return Component.text("  " + mmr, NamedTextColor.GRAY, TextDecoration.ITALIC);
  }
}
