package rip.bolt.ingame.utils;

import dev.pgm.events.Tournament;
import dev.pgm.events.team.TournamentTeam;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import rip.bolt.ingame.api.definitions.Team;
import tc.oc.pgm.tablist.TeamTabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class RankedTeamTabEntry extends TeamTabEntry {

  @Nullable private BaseComponent mmrComponent;
  private tc.oc.pgm.teams.Team team;

  public RankedTeamTabEntry(tc.oc.pgm.teams.Team team) {
    super(team);

    this.team = team;
  }

  @Override
  public BaseComponent[] getContent(TabView view) {
    BaseComponent[] base = super.getContent(view);

    if (mmrComponent == null) mmrComponent = getMmrComponent();

    if (mmrComponent != null) {
      base = Arrays.copyOf(base, base.length + 1);
      base[base.length - 1] = mmrComponent;
    }

    return base;
  }

  private BaseComponent getMmrComponent() {
    Optional<TournamentTeam> tournamentTeam =
        Tournament.get().getTeamManager().tournamentTeam(team);

    return tournamentTeam
        .filter(t -> t instanceof Team)
        .map(t -> (Team) t)
        .map(t -> new TextComponent("  " + t.getMmr()))
        .map(
            c -> {
              c.setColor(ChatColor.GRAY);
              c.setItalic(true);
              return c;
            })
        .orElse(null);
  }
}
