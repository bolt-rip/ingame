package rip.bolt.ingame.utils;

public enum CancelReason {
  MANUAL_CANCEL, // An admin manually used /ingame cancel
  AUTOMATED_CANCEL, // Players left, the match automatically cancels
  SYNC_STATE // Server said it's over, just finish it
}
