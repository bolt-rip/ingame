package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltPGMMap;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.map.MapInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugCommand {

  public static final int JOIN_OBS = 0;
  public static final int JOIN_TEAM = 1;
  public static final int USE_CHAT = 2;

  public static final int SET_MAP = 3;
  public static final int SET_TEAM_NAME = 4;
  public static final int MOVE_PLAYER = 5;

  public static final int START_MATCH = 6;
  public static final int CYCLE_MATCH = 7;

  public static final int SET_VISIBILITY = 8;
  public static final int MANAGE_TEAMS = 9;
  public static final int LOAD_PREMADE_TEAM = 10;
  public static final int CHANGE_NAME = 11;
  public static final int SET_TEAM_SIZE = 12;
  public static final int SET_PUG_STATUS = 13;

  public static final int SET_PERMISSIONS = 16;
  public static final int SET_MODERATOR = 17;

  public static final int SET_PLAYER_STATUS = 20;
  public static final int SET_MATCH_STATUS = 21;

  private final int cmd;
  private final Object data;

  public PugCommand(int cmd, Object data) {
    this.cmd = cmd;
    this.data = data;
  }

  public int getCmd() {
    return cmd;
  }

  public Object getData() {
    return data;
  }

  public static PugCommand joinObs(Player player) {
    return PugCommand.of(JOIN_OBS, player);
  }

  public static PugCommand joinTeam(Player player, PugTeam team) {
    return PugCommand.of(JOIN_TEAM, player, "id", team.getId());
  }

  public static PugCommand sendMessage(Player player, String message) {
    return PugCommand.of(USE_CHAT, player, "message", message);
  }

  public static PugCommand setMap(Player player, BoltPGMMap map) {
    return PugCommand.of(SET_MAP, player, "id", map.getId());
  }

  public static PugCommand setMap(Player player, MapInfo map) {
    return PugCommand.of(SET_MAP, player, "name", map.getName());
  }

  public static PugCommand setTeamName(Player player, PugTeam team, String name) {
    return new Builder(SET_TEAM_NAME, player).set("id", team.getId()).set("name", name).build();
  }

  public static PugCommand movePlayer(Player sender, Player player, @Nullable PugTeam team) {
    String id = team == null ? null : team.getId();
    return new Builder(MOVE_PLAYER, sender).set("uuid", player.getUniqueId()).set("id", id).build();
  }

  public static PugCommand startMatch(Player sender, Duration time) {
    if (time == null) time = Duration.ofSeconds(20);
    return PugCommand.of(START_MATCH, sender, "duration", time.toMillis() / 1000);
  }

  public static PugCommand cycleMatch(Player sender) {
    return PugCommand.of(CYCLE_MATCH, sender);
  }

  public static PugCommand cycleMatch(Player player, BoltPGMMap map) {
    return PugCommand.of(CYCLE_MATCH, player, "id", map.getId());
  }

  public static PugCommand cycleMatch(Player sender, MapInfo map) {
    return PugCommand.of(CYCLE_MATCH, sender, "name", map.getName());
  }

  private static PugCommand setVisibility() {
    // Intentionally private. Not usable in-game.
    return new PugCommand(SET_VISIBILITY, null);
  }

  public static PugCommand shuffle(Player sender) {
    return PugCommand.of(MANAGE_TEAMS, sender, "action", ManageTeamOption.SHUFFLE);
  }

  public static PugCommand balance(Player sender) {
    return PugCommand.of(MANAGE_TEAMS, sender, "action", ManageTeamOption.BALANCE);
  }

  public static PugCommand clear(Player sender) {
    return PugCommand.of(MANAGE_TEAMS, sender, "action", ManageTeamOption.CLEAR);
  }

  public static PugCommand setPugName(Player sender, String name) {
    return PugCommand.of(CHANGE_NAME, sender, "name", name);
  }

  public static PugCommand setTeamSize(Player sender, int format) {
    return PugCommand.of(SET_TEAM_SIZE, sender, "format", format);
  }

  private static PugCommand setPugStatus() {
    // Intentionally private. Not usable in-game.
    return PugCommand.of(SET_PUG_STATUS, null);
  }

  private static PugCommand setPermissions() {
    // Intentionally private. Not usable in-game.
    return PugCommand.of(SET_PERMISSIONS, null);
  }

  private static PugCommand setModerator() {
    // Intentionally private. Not usable in-game.
    return PugCommand.of(SET_MODERATOR, null);
  }

  public static PugCommand setPlayerStatus(PugPlayer player, boolean online) {
    return new Builder(SET_PLAYER_STATUS)
        .set("username", player.getUsername())
        .set("uuid", player.getUuid())
        .set("game", online)
        .build();
  }

  public static PugCommand setMatchStatus(BoltMatch match) {
    return new Builder(SET_MATCH_STATUS)
        .set("match_id", match.getId())
        .set("server", AppData.API.getServerName())
        .set("status", match.getStatus().name())
        .build();
  }

  private enum ManageTeamOption {
    CLEAR,
    SHUFFLE,
    BALANCE
  }

  // Helper methods
  private static PugCommand of(int cmd, Player sender) {
    return new Builder(cmd, sender).build();
  }

  private static PugCommand of(int cmd, Player sender, String k1, Object v1) {
    return new Builder(cmd, sender).set(k1, v1).build();
  }

  private static class Builder {
    private final int cmd;
    private final Map<String, Object> data = new HashMap<>();

    public Builder(int cmd) {
      this(cmd, null);
    }

    public Builder(int cmd, Player player) {
      this.cmd = cmd;
      if (player != null) data.put("user", new PugPlayer(player.getUniqueId(), player.getName()));
    }

    public PugCommand.Builder set(String key, Object obj) {
      data.put(key, obj);
      return this;
    }

    public PugCommand build() {
      return new PugCommand(cmd, data);
    }
  }
}
