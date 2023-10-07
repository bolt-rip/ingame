package rip.bolt.ingame.pugs;

import java.util.List;
import rip.bolt.ingame.api.definitions.BoltTournamentTeam;
import rip.bolt.ingame.api.definitions.Team;
import rip.bolt.ingame.api.definitions.pug.PugPlayer;
import rip.bolt.ingame.api.definitions.pug.PugTeam;

public class ManagedTeam implements BoltTournamentTeam {
  private final String pugTeamId;

  private PugTeam pugTeam;
  private Team boltTeam;
  private tc.oc.pgm.teams.Team pgmTeam;

  public ManagedTeam(String teamId) {
    this.pugTeamId = teamId;
  }

  public String getId() {
    return pugTeamId;
  }

  public PugTeam getPugTeam() {
    return pugTeam;
  }

  public void setPugTeam(PugTeam pugTeam) {
    this.pugTeam = pugTeam;
  }

  public Team getBoltTeam() {
    return boltTeam;
  }

  public void setBoltTeam(Team boltTeam) {
    this.boltTeam = boltTeam;
  }

  public tc.oc.pgm.teams.Team getPgmTeam() {
    return pgmTeam;
  }

  public void setPgmTeam(tc.oc.pgm.teams.Team pgmTeam) {
    this.pgmTeam = pgmTeam;
  }

  public void clean() {
    this.pugTeam = null;
    this.boltTeam = null;
    this.pgmTeam = null;
  }

  @Override
  public String getName() {
    return pugTeam.getName();
  }

  @Override
  public List<PugPlayer> getPlayers() {
    return pugTeam.getPlayers();
  }

  @Override
  public Integer getTeamId() {
    return boltTeam.getTeamId();
  }
}
