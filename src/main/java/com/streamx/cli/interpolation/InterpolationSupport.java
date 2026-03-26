package com.streamx.cli.interpolation;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static io.smallrye.common.expression.Expression.Flag;

import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;
import io.smallrye.common.expression.Expression;
import jakarta.enterprise.context.Dependent;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

@Dependent
public class InterpolationSupport {

  String expand(String rawValue) {
    Objects.requireNonNull(rawValue, msg.expressionCannotBeNull());

    if (rawValue.indexOf('$') == -1) {
      return rawValue;
    }

    Properties streamxConfig = loadStreamxConfig();
    return expand(rawValue, streamxConfig);
  }

  private String expand(String rawValue, Properties streamxConfig) {
    Expression expression = Expression.compile(
        escapeDollarIfExists(rawValue),
        Flag.LENIENT_SYNTAX,
        Flag.NO_TRIM,
        Flag.NO_SMART_BRACES
    );
    return expression.evaluate((resolveContext, stringBuilder) -> {
      String key = resolveContext.getKey();

      // Resolution order: env variables, system properties, StreamxHome config.
      String value = System.getenv(key);
      if (value == null) {
        value = System.getProperty(key);
      }
      if (value == null) {
        value = streamxConfig.getProperty(key);
      }

      if (value != null) {
        if (value.indexOf('$') != -1) {
          value = expand(value, streamxConfig);
        }
        stringBuilder.append(value);
      } else if (resolveContext.hasDefault()) {
        resolveContext.expandDefault();
      } else {
        throw new CliException(
            msg.couldNotExpandValueInExpression(key, rawValue));
      }
    });
  }

  private Properties loadStreamxConfig() {
    Properties props = new Properties();
    try {
      URL configUrl = StreamxHome.getConfigUrl();
      try (InputStream in = configUrl.openStream()) {
        props.load(in);
      }
    } catch (Exception e) {
      // Config file may not exist; proceed without it
    }
    return props;
  }

  private String escapeDollarIfExists(String value) {
    int index = value.indexOf("\\$");
    if (index != -1) {
      int start = 0;
      StringBuilder builder = new StringBuilder();
      while (index != -1) {
        builder.append(value, start, index).append("$$");
        start = index + 2;
        index = value.indexOf("\\$", start);
      }
      builder.append(value.substring(start));
      return builder.toString();
    }
    return value;
  }
}
