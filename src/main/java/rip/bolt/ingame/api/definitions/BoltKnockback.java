package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.github.paperspigot.PaperSpigotConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoltKnockback {
  private double friction;
  private double horizontal;
  private double vertical;
  private double verticalLimit;
  private double extraHorizontal;
  private double extraVertical;

  public BoltKnockback() {}

  public BoltKnockback(
      double friction,
      double horizontal,
      double vertical,
      double verticalLimit,
      double extraHorizontal,
      double extraVertical) {
    this.friction = friction;
    this.horizontal = horizontal;
    this.vertical = vertical;
    this.verticalLimit = verticalLimit;
    this.extraHorizontal = extraHorizontal;
    this.extraVertical = extraVertical;
  }

  public double getFriction() {
    return friction;
  }

  public void setFriction(double friction) {
    this.friction = friction;
  }

  public double getHorizontal() {
    return horizontal;
  }

  public void setHorizontal(double horizontal) {
    this.horizontal = horizontal;
  }

  public double getVertical() {
    return vertical;
  }

  public void setVertical(double vertical) {
    this.vertical = vertical;
  }

  public double getVerticalLimit() {
    return verticalLimit;
  }

  public void setVerticalLimit(double verticalLimit) {
    this.verticalLimit = verticalLimit;
  }

  public double getExtraHorizontal() {
    return extraHorizontal;
  }

  public void setExtraHorizontal(double extraHorizontal) {
    this.extraHorizontal = extraHorizontal;
  }

  public double getExtraVertical() {
    return extraVertical;
  }

  public void setExtraVertical(double extraVertical) {
    this.extraVertical = extraVertical;
  }

  @Override
  public String toString() {
    return "BoltKnockback{"
        + "friction="
        + friction
        + ", horizontal="
        + horizontal
        + ", vertical="
        + vertical
        + ", verticalLimit="
        + verticalLimit
        + ", extraHorizontal="
        + extraHorizontal
        + ", extraVertical="
        + extraVertical
        + '}';
  }

  public static BoltKnockback defaults() {
    double friction = PaperSpigotConfig.config.getDouble("paper.knockback.friction", 2.0D);
    double horizontal = PaperSpigotConfig.config.getDouble("paper.knockback.horizontal", 0.4D);
    double vertical = PaperSpigotConfig.config.getDouble("paper.knockback.vertical", 0.4D);
    double verticalLimit =
        PaperSpigotConfig.config.getDouble("paper.knockback.vertical-limit", 0.4D);
    double extraHorizontal =
        PaperSpigotConfig.config.getDouble("paper.knockback.extra-horizontal", 0.5D);
    double extraVertical =
        PaperSpigotConfig.config.getDouble("paper.knockback.extra-vertical", 0.1D);

    return new BoltKnockback(
        friction, horizontal, vertical, verticalLimit, extraHorizontal, extraVertical);
  }
}
