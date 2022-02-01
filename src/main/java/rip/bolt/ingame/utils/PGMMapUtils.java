package rip.bolt.ingame.utils;

import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltPGMMap;
import tc.oc.pgm.api.match.Match;

public class PGMMapUtils {

  public static BoltPGMMap getBoltPGMMap(BoltMatch boltMatch, Match match) {
    String pgmMapName = match.getMap().getName();

    if (boltMatch.getMap() != null && boltMatch.getMap().getName().equals(pgmMapName)) {
      return boltMatch.getMap();
    }

    return new BoltPGMMap(pgmMapName);
  }
}
