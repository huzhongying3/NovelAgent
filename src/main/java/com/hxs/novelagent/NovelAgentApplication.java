package com.hxs.novelagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = {"com.hxs.novelagent"})
public class NovelAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelAgentApplication.class, args);
    }

}
