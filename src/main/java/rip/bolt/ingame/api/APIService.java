package rip.bolt.ingame.api;

import java.util.Map;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rip.bolt.ingame.api.definitions.BoltMatch;

public interface APIService {

  @GET("ranked/servers/{server}/match")
  BoltMatch getMatch(@Path("server") String serverId);

  @POST("ranked/matches/{match}")
  Void postMatch(@Path("match") String matchId, @Body BoltMatch match);

  @POST("users/{uuid}/ban")
  Void postPlayerAbandon(@Path("uuid") String uuid, @Body Map<String, Object> data);
}
