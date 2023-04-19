package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ranking {

  private Integer rating;
  private Double score;
  private Double sigma;

  private BoltRank rank;

  public @Nullable Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Double getSigma() {
    return sigma;
  }

  public void setSigma(Double sigma) {
    this.sigma = sigma;
  }

  public BoltRank getRank() {
    return rank;
  }

  public void setRank(BoltRank rank) {
    this.rank = rank;
  }
}
