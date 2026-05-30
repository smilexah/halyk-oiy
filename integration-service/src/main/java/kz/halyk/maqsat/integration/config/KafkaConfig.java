package kz.halyk.maqsat.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Bean
    public ByteArrayJsonMessageConverter jsonMessageConverter() {
        return new ByteArrayJsonMessageConverter();
    }
}
