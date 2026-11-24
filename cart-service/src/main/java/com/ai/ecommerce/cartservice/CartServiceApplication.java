package com.ai.ecommerce.cartservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.ai.ecommerce")

public class CartServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
        logger.info("Cart Service Started Successfully!");
    }
}
