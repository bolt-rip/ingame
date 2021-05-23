package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugMessage {

  private PugPlayer player;
  private String[] message;
  private Type type;

  public String[] getMessage() {
    return message;
  }

  public void setMessage(String[] message) {
    this.message = message;
  }

  public PugPlayer getPlayer() {
    return player;
  }

  public void setPlayer(PugPlayer player) {
    this.player = player;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public enum Type {
    PLAYER_WEB,
    PLAYER_INGAME,
    SYSTEM,
    SYSTEM_WEB,
    SYSTEM_KO
  }
}
