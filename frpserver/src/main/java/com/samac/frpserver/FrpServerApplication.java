package com.samac.frpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value= "com.samac")
public class FrpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrpServerApplication.class, args);
    }

}
