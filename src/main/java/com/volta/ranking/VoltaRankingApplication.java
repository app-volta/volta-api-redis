package com.volta.ranking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VoltaRankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoltaRankingApplication.class, args);
    }
}
