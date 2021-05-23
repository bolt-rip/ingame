package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The stats for the participation of a user */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stats {

  private int kills;
  private int deaths;
  private int killstreak;
  private double damageDealt;
  private double damageDealtBow;
  private double damageReceived;
  private double damageReceivedBow;
  private int arrowsHit;
  private int arrowsShot;
  private double score;

  public Stats() {}

  public Stats(
      int kills,
      int deaths,
      int killstreak,
      double damageDealt,
      double damageDealtBow,
      double damageReceived,
      double damageReceivedBow,
      int arrowsHit,
      int arrowsShot,
      double score) {
    this.kills = kills;
    this.deaths = deaths;
    this.killstreak = killstreak;
    this.damageDealt = damageDealt;
    this.damageDealtBow = damageDealtBow;
    this.damageReceived = damageReceived;
    this.damageReceivedBow = damageReceivedBow;
    this.arrowsHit = arrowsHit;
    this.arrowsShot = arrowsShot;
    this.score = score;
  }

  public int getKills() {
    return kills;
  }

  public void setKills(int kills) {
    this.kills = kills;
  }

  public int getDeaths() {
    return deaths;
  }

  public void setDeaths(int deaths) {
    this.deaths = deaths;
  }

  public int getKillstreak() {
    return killstreak;
  }

  public void setKillstreak(int killstreak) {
    this.killstreak = killstreak;
  }

  public double getDamageDealt() {
    return damageDealt;
  }

  public void setDamageDealt(double damageDealt) {
    this.damageDealt = damageDealt;
  }

  public double getDamageDealtBow() {
    return damageDealtBow;
  }

  public void setDamageDealtBow(double damageDealtBow) {
    this.damageDealtBow = damageDealtBow;
  }

  public double getDamageReceived() {
    return damageReceived;
  }

  public void setDamageReceived(double damageReceived) {
    this.damageReceived = damageReceived;
  }

  public double getDamageReceivedBow() {
    return damageReceivedBow;
  }

  public void setDamageReceivedBow(double damageReceivedBow) {
    this.damageReceivedBow = damageReceivedBow;
  }

  public int getArrowsHit() {
    return arrowsHit;
  }

  public void setArrowsHit(int arrowsHit) {
    this.arrowsHit = arrowsHit;
  }

  public int getArrowsShot() {
    return arrowsShot;
  }

  public void setArrowsShot(int arrowsShot) {
    this.arrowsShot = arrowsShot;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }
}
