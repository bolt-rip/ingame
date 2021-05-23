package rip.bolt.ingame.api.definitions.pug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PugResponse {

  private JsonNode lobby;
  private PugMessage chat;

  public JsonNode getLobby() {
    return lobby;
  }

  public void setLobby(JsonNode lobby) {
    this.lobby = lobby;
  }

  public PugMessage getChat() {
    return chat;
  }

  public void setChat(PugMessage chat) {
    this.chat = chat;
  }

  @Override
  public String toString() {
    return lobby.toString();
  }
}
