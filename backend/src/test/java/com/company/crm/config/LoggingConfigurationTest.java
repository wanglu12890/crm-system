package com.company.crm.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigurationTest {

    private static final String LOG_IMPL = "mybatis-plus.configuration.log-impl";
    private static final String NO_LOGGING = "org.apache.ibatis.logging.nologging.NoLoggingImpl";

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void shouldDisableMyBatisSqlLoggingByDefault() throws IOException {
        Object configuredLogImpl = property("application.yml", LOG_IMPL);

        assertThat(configuredLogImpl)
                .asString()
                .contains(NO_LOGGING)
                .doesNotContain("StdOutImpl");
        assertThat(property("application.yml", "logging.level.org.apache.ibatis")).isEqualTo("INFO");
        assertThat(property("application.yml", "logging.level.com.baomidou.mybatisplus")).isEqualTo("INFO");
        assertThat(property("application.yml", "logging.level.com.company.crm.mapper")).isEqualTo("INFO");
    }

    @Test
    void shouldKeepProductionMyBatisLoggingDisabled() throws IOException {
        assertThat(property("application-prod.yml", LOG_IMPL)).isEqualTo(NO_LOGGING);
    }

    @Test
    void shouldKeepMapperXmlAvailable() {
        assertThat(new ClassPathResource("mapper/CustomerMapper.xml").exists()).isTrue();
    }

    private Object property(String resourceName, String key) throws IOException {
        List<PropertySource<?>> sources = loader.load(resourceName, new ClassPathResource(resourceName));
        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}
