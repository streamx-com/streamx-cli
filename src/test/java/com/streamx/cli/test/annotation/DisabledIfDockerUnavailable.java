package com.streamx.cli.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/** Docker doesn't work with macOS arm64 at this moment.
Therefore, we can't run tests which rely on docker engine on this type of CI runner.
At the same time, it would be nice to keep other tests working.
*/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailableCondition.class)
public @interface DisabledIfDockerUnavailable {
}