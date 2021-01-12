package rip.bolt.ingame.api;

import java.util.Map;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rip.bolt.ingame.api.definitions.BoltMatch;

public interface APIService {

  @GET("match/{server}")
  BoltMatch getMatch(@Path("server") String serverId);

  @POST("match/{match}/start")
  Void postMatchStart(@Path("match") String matchId, @Body BoltMatch user);

  @POST("match/{match}/finish")
  Void postMatchEnd(@Path("match") String matchId, @Body BoltMatch user);

  @POST("user/{uuid}/ban")
  Void postPlayerAbandon(@Path("uuid") String uuid, @Body Map<String, Object> data);
}
