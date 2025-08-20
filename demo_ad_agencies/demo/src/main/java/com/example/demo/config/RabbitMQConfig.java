package com.example.demo.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;


@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "post-exchange";
    public static final String QUEUE_NAME = "ad-queue-2";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true); // Durable queue
    }

    @Bean
    public Binding binding(FanoutExchange fanoutExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }
}

