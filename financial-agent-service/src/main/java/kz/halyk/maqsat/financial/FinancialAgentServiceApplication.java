package kz.halyk.maqsat.financial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class FinancialAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialAgentServiceApplication.class, args);
    }
}
