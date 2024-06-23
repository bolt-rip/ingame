package rip.bolt.ingame.commands;

import dev.pgm.events.EventsPlugin;
import java.time.Duration;
import java.util.Collection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.managers.MatchManager;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.injectors.AudienceProvider;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.injectors.PlayerProvider;
import tc.oc.pgm.command.injectors.TeamMatchModuleProvider;
import tc.oc.pgm.command.parsers.DurationParser;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.PlayerParser;
import tc.oc.pgm.command.parsers.TeamParser;
import tc.oc.pgm.command.parsers.TeamsParser;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.lib.io.leangen.geantyref.TypeFactory;
import tc.oc.pgm.lib.org.incendo.cloud.minecraft.extras.MinecraftHelp;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;

public class IngameCommandGraph extends CommandGraph<Ingame> {

  public IngameCommandGraph(Ingame ingame) throws Exception {
    super(ingame);
  }

  @Override
  protected MinecraftHelp<CommandSender> createHelp() {
    return MinecraftHelp.create("/ingame help", manager, Audience::get);
  }

  @Override
  protected void setupInjectors() {
    // PGM Injectors
    registerInjector(PGM.class, PGM::get);
    registerInjector(EventsPlugin.class, EventsPlugin::get);
    registerInjector(MapOrder.class, () -> PGM.get().getMapOrder());
    registerInjector(MatchManager.class, () -> Ingame.get().getMatchManager());

    registerInjector(Audience.class, new AudienceProvider());
    registerInjector(Match.class, new MatchProvider());
    registerInjector(MatchPlayer.class, new MatchPlayerProvider());
    registerInjector(Player.class, new PlayerProvider());

    registerInjector(TeamMatchModule.class, new TeamMatchModuleProvider());
  }

  @Override
  protected void setupParsers() {
    registerParser(Duration.class, new DurationParser());
    registerParser(MatchPlayer.class, new MatchPlayerParser());
    registerParser(MapInfo.class, MapInfoParser::new);
    registerParser(Player.class, new PlayerParser());
    registerParser(Party.class, PartyParser::new);
    registerParser(Team.class, TeamParser::new);
    registerParser(TypeFactory.parameterizedClass(Collection.class, Team.class), TeamsParser::new);
  }

  @Override
  public void registerCommands() {
    register(new RequeueCommands());
    register(new ForfeitCommands());
    register(new AdminCommands());
    PugCommands pugCommands = new PugCommands(plugin.getMatchManager());
    register(pugCommands);
    register(pugCommands.getTeamCommands());

    PugCommands.setupSubCommands(manager.commandTree().getNamedNode("pug"));
  }
}
