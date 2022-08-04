package rip.bolt.ingame.pugs;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.craftbukkit.libs.joptsimple.internal.Strings;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.pug.PugMessage;
import rip.bolt.ingame.api.definitions.pug.PugResponse;
import rip.bolt.ingame.config.AppData;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;

public class BoltWebSocket extends WebSocketClient {

  public static final Component CONSOLE_NAME =
      translatable("misc.console", NamedTextColor.DARK_AQUA)
          .decoration(TextDecoration.ITALIC, true);

  private final ObjectMapper objectMapper;
  private final PugManager manager;

  public BoltWebSocket(URI serverUri, PugManager manager) {
    super(serverUri);

    this.manager = manager;
    this.objectMapper = manager.getObjectMapper();
  }

  @Override
  public void send(String text) {
    super.send(text);
  }

  @Override
  public void onOpen(ServerHandshake serverHandshake) {
    Ingame.newSharedChain("pug-sync").sync(manager::reset).execute();
  }

  @Override
  public void onMessage(String s) {
    try {
      PugResponse pugResponse = objectMapper.readValue(s, PugResponse.class);

      Ingame.newSharedChain("pug-sync")
          .sync(() -> handleMessageSync(manager, pugResponse))
          .execute();

    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private void handleMessageSync(PugManager manager, PugResponse pugResponse) {
    try {
      manager.syncPugLobby(pugResponse.getLobby());
    } catch (IOException e) {
      e.printStackTrace();
    }

    PugMessage chat = pugResponse.getChat();

    if (chat == null) return;

    Match pgmMatch = Ingame.get().getMatchManager().getPGMMatch();

    MatchPlayer sender =
        chat.getPlayer() != null ? pgmMatch.getPlayer(chat.getPlayer().getUuid()) : null;
    Component senderName =
        sender != null
            ? sender.getName(NameStyle.VERBOSE)
            : chat.getPlayer() != null && chat.getPlayer().getUsername() != null
                ? PlayerComponent.player(
                    (UUID) null, chat.getPlayer().getUsername(), NameStyle.VERBOSE)
                : CONSOLE_NAME;

    Component body = text(Strings.join(chat.getMessage(), ", "));

    switch (chat.getType()) {
      case PLAYER_INGAME:
        return; // No-op, message was already sent by pgm
      case PLAYER_WEB:
        pgmMatch.sendMessage(
            text()
                .append(text("<", NamedTextColor.WHITE))
                .append(senderName)
                .append(text(">: ", NamedTextColor.WHITE))
                .append(body)
                .build());
        break;
      case SYSTEM_KO:
        // If it is not specific to a player, it will fall-thru to the bottom case
        if (chat.getPlayer() != null) {
          if (sender != null) sender.sendWarning(body);
          break;
        }
      case SYSTEM:
        Component message =
            text()
                .append(text("[", NamedTextColor.WHITE))
                .append(text("PUG", NamedTextColor.GOLD))
                .append(text("] ", NamedTextColor.WHITE))
                .append(senderName)
                .append(text(" » ", NamedTextColor.GRAY))
                .append(body)
                .build();

        if (AppData.publiclyLogPugs() && chat.getType() == PugMessage.Type.SYSTEM) {
          pgmMatch.sendMessage(message);
        } else {
          // SYSTEM_KO messages not from a player fall in here too
          for (MatchPlayer player : pgmMatch.getPlayers()) {
            if (player.getBukkit().isOp()) player.sendMessage(message);
          }
        }

        break;
    }
  }

  @Override
  public void onClose(int i, String s, boolean b) {
    System.out.println("[Ingame] Closed socket " + i + " - " + b + " " + s + "");

    // Try to reconnect if failed.
    if (i == CloseFrame.NEVER_CONNECTED || i == CloseFrame.ABNORMAL_CLOSE) {
      Ingame.newSharedChain("pug-sync").sync(manager::reconnect).execute();
    }
  }

  @Override
  public void onError(Exception e) {
    System.out.println(e.getMessage());
  }
}
