package kz.halyk.maqsat.parsebudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ParseBudgetPlanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParseBudgetPlanServiceApplication.class, args);
    }
}
