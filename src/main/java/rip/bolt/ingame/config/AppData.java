package rip.bolt.ingame.config;

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
      return String.valueOf(System.getenv("SERVER_NAME"));
    }
  }

  public static long absentSecondsLimit() {
    return Ingame.get().getConfig().getLong("absence-time-seconds", 120);
  }
}
