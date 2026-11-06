package org.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

class RedisConfigurationTest {

    @Test
    void packagedDefaultsUseVerifiedOperationalEndpointAndDefaultAclUser() {
        context(Map.of("REDIS_PASSWORD", "synthetic-test-password"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.getHostName()).isEqualTo("moving-goat-89109.upstash.io");
                    assertThat(factory.getPort()).isEqualTo(6379);
                    assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("default");
                    assertThat(factory.getClientConfiguration().isUseSsl()).isTrue();
                });
    }

    private ApplicationContextRunner context(Map<String, Object> variables) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
                .withInitializer(context -> {
                    try {
                        context.getEnvironment().getPropertySources().addLast(
                                new ResourcePropertySource("classpath:application.properties"));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                    context.getEnvironment().getPropertySources().addFirst(
                            new SystemEnvironmentPropertySource("redis-test-environment", variables));
                });
    }

    @Test
    void localAliasesConfigureAclUserAndPreservePasswordExactlyWithTls() {
        context(Map.of("REDIS_HOST", "redis.test", "REDIS_PORT", "6380",
                "REDIS_USERNAME", "test-acl", "REDIS_PASSWORD", " synthetic-test-password "))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.getHostName()).isEqualTo("redis.test");
                    assertThat(factory.getPort()).isEqualTo(6380);
                    assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("test-acl");
                    assertThat(factory.getStandaloneConfiguration().getPassword().get())
                            .isEqualTo(" synthetic-test-password ".toCharArray());
                    assertThat(factory.getClientConfiguration().isUseSsl()).isTrue();
                });
    }

    @Test
    void springEnvironmentOverridesLocalAliases() {
        context(Map.of("REDIS_HOST", "legacy.test", "REDIS_USERNAME", "legacy",
                "REDIS_PASSWORD", "legacy-synthetic", "SPRING_DATA_REDIS_HOST", "spring.test",
                "SPRING_DATA_REDIS_USERNAME", "spring-acl", "SPRING_DATA_REDIS_PASSWORD", "spring-synthetic"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.getHostName()).isEqualTo("spring.test");
                    assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("spring-acl");
                    assertThat(factory.getStandaloneConfiguration().getPassword().get())
                            .isEqualTo("spring-synthetic".toCharArray());
                    assertThat(factory.getClientConfiguration().isUseSsl()).isTrue();
                });
    }

    @Test
    void explicitTlsUrlOverridesIndividualConnectionProperties() {
        context(Map.of("REDIS_PASSWORD", "unused-synthetic", "SPRING_DATA_REDIS_HOST", "ignored.test",
                "SPRING_DATA_REDIS_USERNAME", "ignored", "SPRING_DATA_REDIS_PASSWORD", "ignored-synthetic",
                "SPRING_DATA_REDIS_URL", "rediss://url-acl:url-synthetic@url.test:6381"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.getHostName()).isEqualTo("url.test");
                    assertThat(factory.getPort()).isEqualTo(6381);
                    assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("url-acl");
                    assertThat(factory.getStandaloneConfiguration().getPassword().get())
                            .isEqualTo("url-synthetic".toCharArray());
                    assertThat(factory.getClientConfiguration().isUseSsl()).isTrue();
                });
    }
}
