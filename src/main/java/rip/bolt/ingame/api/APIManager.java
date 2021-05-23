package rip.bolt.ingame.api;

import com.fasterxml.jackson.databind.MapperFeature;
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
  public final ObjectMapper objectMapper;

  public APIManager() {
    serverId = AppData.API.getServerName();
    objectMapper = new ObjectMapper().registerModule(new DateModule());

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new DateModule());
    objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

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
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
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
    int retries = 40;

    final int PRECONDITION_FAILED = 412;

    for (int i = 0; i < retries; ) {
      try {
        return apiService.postMatch(match.getId(), match);
      } catch (APIException ex) {
        ex.printStackTrace();
        if (ex.getCode() == PRECONDITION_FAILED && i > 2) return match;
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      i += 1;
      System.out.println(
          "[Ingame] Failed to report match end, retrying in "
              + (i * 5)
              + "s ("
              + i
              + "/"
              + retries
              + ")");
      try {
        Thread.sleep(i * 5000L);
      } catch (InterruptedException ignore) {
      }
    }
    return null;
  }
}
