package rip.bolt.ingame.config;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import java.time.Duration;
import rip.bolt.ingame.Ingame;

public class AppData {

  public static class API {

    public static String getURL() {
      return Ingame.get().getConfig().getString("api.url");
    }

    public static String getKey() {
      return Ingame.get().getConfig().getString("api.key");
    }

    public static String getServerName() {
      return Ingame.get()
          .getConfig()
          .getString("server-name", String.valueOf(System.getenv("SERVER_NAME")));
    }
  }

  public static class Web {

    public static String getMatch() {
      return Ingame.get().getConfig().getString("web.match", null);
    }

    public static String getProfile() {
      return Ingame.get().getConfig().getString("web.profile", null);
    }
  }

  public static class Socket {
    public static String getUrl() {
      return Ingame.get().getConfig().getString("socket.url");
    }
  }

  public static long absentSecondsLimit() {
    return Ingame.get().getConfig().getLong("absence-time-seconds", 120);
  }

  public static boolean fullTeamsRequired() {
    return Ingame.get().getConfig().getBoolean("full-teams-required", true);
  }

  public static boolean allowRequeue() {
    return Ingame.get().getConfig().getBoolean("allow-requeue", true);
  }

  public static boolean forfeitEnabled() {
    return Ingame.get().getConfig().getBoolean("forfeit.enabled", true);
  }

  public static Duration forfeitAfter() {
    return parseDuration(Ingame.get().getConfig().getString("forfeit.after", "300s"));
  }

  public static Duration autoCancelBefore() {
    return parseDuration(Ingame.get().getConfig().getString("auto-cancel.before", "60s"));
  }

  public static Duration autoCancelCountdown() {
    return parseDuration(Ingame.get().getConfig().getString("auto-cancel.countdown", "10s"));
  }

  public static Duration matchStartDuration() {
    return parseDuration(Ingame.get().getConfig().getString("match-start-duration", "180s"));
  }

  public static boolean customTabEnabled() {
    return Ingame.get().getConfig().getBoolean("custom-tab-enabled", true);
  }

  public static boolean publiclyLogPugs() {
    return Ingame.get().getConfig().getBoolean("publicly-log-pugs", false);
  }
}
