package rip.bolt.ingame.utils;

import rip.bolt.ingame.managers.BattlepassManager;

public class BattlepassUtils {

  public static BattlepassManager createManager() {
    try {
      Class.forName("tc.oc.occ.dispense.events.UpdateObjectiveTrackingEvent");
      return new BattlepassManager();
    } catch (ClassNotFoundException ignored) {
      return null;
    }
  }
}
