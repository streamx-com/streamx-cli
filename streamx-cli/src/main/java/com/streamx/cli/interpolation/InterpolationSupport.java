package com.streamx.cli.interpolation;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static io.smallrye.common.expression.Expression.Flag;

import com.streamx.cli.framework.CliException;
import io.smallrye.common.expression.Expression;
import jakarta.enterprise.context.Dependent;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@Dependent
public class InterpolationSupport {

  /**
   * Adapted from {@link io.smallrye.config.ExpressionConfigSourceInterceptor}
   */
  String expand(final String rawValue) {
    Objects.requireNonNull(rawValue, msg.expressionCannotBeNull());

    // Avoid extra StringBuilder allocations from Expression
    if (rawValue.indexOf('$') == -1) {
      return rawValue;
    }

    final Config config = ConfigProviderResolver.instance().getConfig();
    final Expression expression = Expression.compile(
        escapeDollarIfExists(rawValue),
        Flag.LENIENT_SYNTAX,
        Flag.NO_TRIM,
        Flag.NO_SMART_BRACES
    );
    return expression.evaluate((resolveContext, stringBuilder) -> {
      Optional<String> resolve = config.getOptionalValue(resolveContext.getKey(), String.class);

      // Fallback to system properties if not found in config
      if (resolve.isEmpty()) {
        String systemProperty = System.getProperty(resolveContext.getKey());
        if (systemProperty != null) {
          resolve = Optional.of(systemProperty);
        }
      }

      if (resolve.isPresent()) {
        String value = resolve.get();
        // Recursively expand if the value contains variables
        if (value.indexOf('$') != -1) {
          value = expand(value);
        }
        stringBuilder.append(value);
      } else if (resolveContext.hasDefault()) {
        resolveContext.expandDefault();
      } else {
        String errMessage = msg.couldNotExpandValueInExpression(resolveContext.getKey(), rawValue);
        throw new CliException(errMessage);
      }
    });
  }

  private String escapeDollarIfExists(final String value) {
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
