package rip.bolt.ingame.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class ReflectionUtils {
  public static final Logger LOGGER = Logger.getLogger(ReflectionUtils.class.getName());

  public static final List<String> CONFIG_CLASSES = Arrays.asList(
      "org.github.paperspigot.PaperSpigotConfig", "com.destroystokyo.paper.PaperConfig");

  private static final Class<?> configClass;
  private static final Object configObject;

  static {
    configClass = findConfigClass();
    if (configClass == null) {
      LOGGER.warning("No configuration class found");
    }
    configObject = getConfigObject();
  }

  @Nullable
  private static Class<?> findConfigClass() {
    for (String className : CONFIG_CLASSES) {
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  @Nullable
  private static Object getConfigObject() {
    if (configClass == null) return null;

    try {
      Field configField = configClass.getDeclaredField("config");
      return configField.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      LOGGER.log(Level.WARNING, "Could not access config field", e);
      return null;
    }
  }

  public static double getConfigValue(String name, double defaultValue) {
    if (configObject == null) return defaultValue;

    try {
      Method getDoubleMethod =
          configObject.getClass().getMethod("getDouble", String.class, double.class);
      Object result = getDoubleMethod.invoke(configObject, name, defaultValue);
      return (double) result;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Could not get config value for: " + name, e);
      return defaultValue;
    }
  }

  public static void setConfigValue(String fieldName, double value) {
    if (configClass == null) return;

    try {
      Field field = configClass.getDeclaredField(fieldName);
      field.set(null, value);
    } catch (NoSuchFieldException e) {
      LOGGER.log(Level.WARNING, "Field not found: " + fieldName, e);
    } catch (IllegalAccessException e) {
      LOGGER.log(Level.SEVERE, "Cannot access field: " + fieldName, e);
    }
  }
}
