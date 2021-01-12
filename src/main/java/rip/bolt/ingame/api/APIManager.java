package rip.bolt.ingame.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.config.AppData;

public class APIManager {

  private final String serverId;
  public final APIService apiService;

  public APIManager() {

    serverId = AppData.API.getServerName();

    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    httpClient.addInterceptor(
        chain ->
            chain.proceed(
                chain
                    .request()
                    .newBuilder()
                    .header("Authorization", AppData.API.getKey())
                    .build()));

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(AppData.API.getURL())
            .addConverterFactory(JacksonConverterFactory.create())
            .addCallAdapterFactory(new DefaultCallAdapterFactory<>())
            .client(httpClient.build())
            .build();

    apiService = retrofit.create(APIService.class);
  }

  public BoltMatch fetchMatchData() {
    return apiService.getMatch(this.serverId);
  }

  public void postMatchPlayerAbandon(UUID uuid, Duration duration) {
    Map<String, Object> data = new HashMap<>();
    data.put("duration", duration.getSeconds());

    apiService.postPlayerAbandon(uuid.toString(), data);
  }

  public void postMatchStart(BoltMatch match) {
    apiService.postMatchStart(match);
  }

  public void postMatchEnd(BoltMatch match) {
    apiService.postMatchEnd(match);
  }
}
