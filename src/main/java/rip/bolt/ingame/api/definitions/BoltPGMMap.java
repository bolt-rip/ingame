package rip.bolt.ingame.api.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoltPGMMap {

  private Integer id;
  private String name;
  private String slug;

  public BoltPGMMap() {}

  public BoltPGMMap(Integer mapId) {
    this.id = mapId;
  }

  public BoltPGMMap(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  @Override
  public String toString() {
    return "BoltPGMMap{" + "id=" + id + ", name='" + name + '\'' + ", slug='" + slug + '\'' + '}';
  }
}
