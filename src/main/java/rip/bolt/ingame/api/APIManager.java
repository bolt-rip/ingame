package rip.bolt.ingame.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.logging.Level;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rip.bolt.ingame.Ingame;
import rip.bolt.ingame.api.definitions.BoltMatch;
import rip.bolt.ingame.api.definitions.BoltResponse;
import rip.bolt.ingame.api.definitions.Punishment;
import rip.bolt.ingame.config.AppData;

public class APIManager {

  private final String serverId;

  public final ObjectMapper objectMapper;

  public final APIService apiService;
  public final QueueAPIService queueAPIService;

  public APIManager() {
    serverId = AppData.getServerName();
    objectMapper = new ObjectMapper().registerModule(new DateModule());
    objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

    apiService = createService(APIService.class, AppData.API.getURL(), AppData.API.getKey());
    queueAPIService =
        createService(QueueAPIService.class, AppData.QueueAPI.getURL(), AppData.QueueAPI.getKey());
  }

  private <T> T createService(Class<T> clazz, String url, String key) {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    httpClient.addInterceptor(chain -> chain.proceed(chain
        .request()
        .newBuilder()
        .header("Authorization", "Bearer " + key)
        .addHeader("x-api-key", key)
        .build()));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .addCallAdapterFactory(new DefaultCallAdapterFactory<>())
        .client(httpClient.build())
        .build();

    return retrofit.create(clazz);
  }

  public BoltMatch fetchMatchData() {
    return apiService.getMatch(this.serverId);
  }

  public void postPlayerPunishment(Punishment punishment) {
    apiService.postPlayerPunishment(punishment.getTarget().toString(), punishment);
  }

  public BoltResponse postPlayerRequeue(UUID uuid) {
    if (AppData.QueueAPI.isEnabled()) {
      return queueAPIService.postPlayerRequeue(uuid.toString());
    }

    return apiService.postPlayerRequeue(uuid.toString());
  }

  public BoltMatch postMatch(BoltMatch match) {
    int retries = 40;

    final int PRECONDITION_FAILED = 412;

    boolean interrupted = false;
    Exception lastError = null;
    try {
      for (int attempt = 1; attempt <= retries; attempt++) {
        try {
          return apiService.postMatch(match.getId(), match);
        } catch (Exception ex) {
          if (ex instanceof APIException apiEx
              && apiEx.getCode() == PRECONDITION_FAILED
              && attempt > 3) {
            Ingame.get()
                .getLogger()
                .info("Match " + match.getId() + " was already updated on the API (412) when"
                    + " reporting " + match.getStatus() + ", keeping local state");
            return match;
          }
          if (Thread.interrupted()) interrupted = true;
          lastError = ex;
        }

        if (attempt == retries) break;

        String message = "Failed to report " + match.getStatus() + " for match " + match.getId()
            + ", retrying in " + (attempt * 5) + "s (" + attempt + "/" + retries + ")";
        if (attempt == 1) Ingame.get().getLogger().log(Level.WARNING, message, lastError);
        else Ingame.get().getLogger().warning(message + ": " + lastError);

        try {
          Thread.sleep(attempt * 5000L);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }

      Ingame.get()
          .getLogger()
          .log(
              Level.SEVERE,
              "Failed to report " + match.getStatus() + " for match " + match.getId() + " after "
                  + retries + " attempts, giving up",
              lastError);
      return null;
    } finally {
      if (interrupted) Thread.currentThread().interrupt();
    }
  }
}
