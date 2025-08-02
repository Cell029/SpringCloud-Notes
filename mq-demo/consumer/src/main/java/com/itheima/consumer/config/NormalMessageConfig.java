package com.itheima.consumer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class NormalMessageConfig {

    @Bean
    public DirectExchange normalMessageExchange(){
        return new DirectExchange("normal.direct");
    }

    @Bean
    public Queue normalQueue(){
        return QueueBuilder
                .durable("normal.queue")
                .deadLetterExchange("dlx.direct")
                .build();
    }

    @Bean
    public Binding normalExchangeBinding(Queue normalQueue, DirectExchange normalMessageExchange){
        return BindingBuilder.bind(normalQueue).to(normalMessageExchange).with("normal");
    }

}
