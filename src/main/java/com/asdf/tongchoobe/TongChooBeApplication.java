package com.asdf.tongchoobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TongChooBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TongChooBeApplication.class, args);
    }

}
