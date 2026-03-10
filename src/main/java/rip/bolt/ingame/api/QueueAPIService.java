package rip.bolt.ingame.api;

import retrofit2.http.POST;
import retrofit2.http.Path;
import rip.bolt.ingame.api.definitions.BoltResponse;

public interface QueueAPIService {

  @POST("users/{uuid}/requeue")
  BoltResponse postPlayerRequeue(@Path("uuid") String uuid);
}
