package rip.bolt.ingame.api;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltResponse;
import rip.bolt.ingame.api.definitions.Punishment;

public interface APIService {

  @GET("ranked/servers/{server}/match")
  BoltMatch getMatch(@Path("server") String serverId);

  @POST("ranked/matches/{match}")
  BoltMatch postMatch(@Path("match") String matchId, @Body BoltMatch match);

  @POST("users/{uuid}/punishments")
  Void postPlayerPunishment(@Path("uuid") String uuid, @Body Punishment punishment);

  @POST("users/{uuid}/requeue")
  BoltResponse postPlayerRequeue(@Path("uuid") String uuid);
}
