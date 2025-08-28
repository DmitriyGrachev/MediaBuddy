package com.hrachovblackblaze.blackblazestreamingmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BlackBlazeStreamingMicroServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlackBlazeStreamingMicroServiceApplication.class, args);
    }

}
