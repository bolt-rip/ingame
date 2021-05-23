package rip.bolt.ingame.api;

public class APIException extends RuntimeException {
  private final int code;

  public APIException(String message, int code) {
    super(message + " (" + code + ")");
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
