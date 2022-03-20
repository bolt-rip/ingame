package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** A player punishment sent to the API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Punishment {

  private final UUID target;
  private Integer seriesId;

  private UUID issuer;
  private String reason;

  public Punishment(UUID target, Integer seriesId) {
    this.target = target;
    this.seriesId = seriesId;
  }

  public Punishment(UUID target, @Nullable CommandSender issuer, @Nullable String reason) {
    this.target = target;
    setIssuer(issuer);
    this.reason = reason;
  }

  public UUID getTarget() {
    return target;
  }

  public Integer getSeriesId() {
    return seriesId;
  }

  public UUID getIssuer() {
    return issuer;
  }

  public void setIssuer(UUID issuer) {
    this.issuer = issuer;
  }

  public void setIssuer(CommandSender issuer) {
    if (issuer instanceof Player) {
      this.issuer = ((Player) issuer).getUniqueId();
    }
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
