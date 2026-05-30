package kz.halyk.maqsat.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;

@Configuration
public class KafkaConfig {

    /**
     * Lets each @KafkaListener receive a different event type — the converter maps the JSON bytes
     * to the listener method's parameter type, so one consumer can handle several topics.
     */
    @Bean
    public ByteArrayJsonMessageConverter jsonMessageConverter() {
        return new ByteArrayJsonMessageConverter();
    }
}
