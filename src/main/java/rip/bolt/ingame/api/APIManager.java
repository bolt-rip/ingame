package rip.bolt.ingame.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltResponse;
import rip.bolt.ingame.api.definitions.Punishment;
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
                    .header("Authorization", "Bearer " + AppData.API.getKey())
                    .build()));

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(AppData.API.getURL())
            .addConverterFactory(
                JacksonConverterFactory.create(new ObjectMapper().registerModule(new DateModule())))
            .addCallAdapterFactory(new DefaultCallAdapterFactory<>())
            .client(httpClient.build())
            .build();

    apiService = retrofit.create(APIService.class);
  }

  public BoltMatch fetchMatchData() {
    return apiService.getMatch(this.serverId);
  }

  public void postPlayerPunishment(Punishment punishment) {
    apiService.postPlayerPunishment(punishment.getTarget().toString(), punishment);
  }

  public BoltResponse postPlayerRequeue(UUID uuid) {
    return apiService.postPlayerRequeue(uuid.toString());
  }

  public BoltMatch postMatch(BoltMatch match) {
    return apiService.postMatch(match.getId(), match);
  }
}
