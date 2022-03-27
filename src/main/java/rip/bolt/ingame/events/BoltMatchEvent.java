package rip.bolt.ingame.events;

import org.bukkit.event.Event;
import rip.bolt.ingame.api.definitions.BoltMatch;

abstract class BoltMatchEvent extends Event {

  private final BoltMatch boltMatch;

  public BoltMatchEvent(BoltMatch boltMatch) {
    this.boltMatch = boltMatch;
  }

  public BoltMatch getBoltMatch() {
    return boltMatch;
  }
}
