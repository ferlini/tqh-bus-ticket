package com.tqh.bus.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TqhBusTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(TqhBusTicketApplication.class, args);
    }
}
