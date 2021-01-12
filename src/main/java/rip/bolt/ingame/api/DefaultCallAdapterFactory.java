package rip.bolt.ingame.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public final class DefaultCallAdapterFactory<T> extends CallAdapter.Factory {

  @Override
  public CallAdapter<T, ?> get(
      final Type returnType, final Annotation[] annotations, final Retrofit retrofit) {
    if (returnType.getTypeName().startsWith(Call.class.getName())) {
      return null;
    }

    return new InstanceCallAdapter(returnType);
  }

  private static String getErrorMessage(final retrofit2.Response<?> response) {
    try (ResponseBody errorBody = response.errorBody()) {
      return Objects.isNull(errorBody) ? response.message() : errorBody.string();
    } catch (IOException e) {
      throw new RuntimeException("could not read error body", e);
    }
  }

  /** Call adapter factory for instances. */
  private class InstanceCallAdapter implements CallAdapter<T, Object> {

    private static final int NOT_FOUND = 404;

    private final Type returnType;

    InstanceCallAdapter(final Type returnType) {
      this.returnType = returnType;
    }

    @Override
    public Type responseType() {
      return returnType;
    }

    @Override
    public Object adapt(final Call<T> call) {
      final retrofit2.Response<T> response;
      try {
        response = call.execute();
      } catch (IOException e) {
        throw new RuntimeException("Could not get request body", e);
      }
      if (!response.isSuccessful()) {
        if (response.code() == NOT_FOUND) {
          return null;
        }
        throw new RuntimeException(getErrorMessage(response));
      }
      return response.body();
    }
  }
}
