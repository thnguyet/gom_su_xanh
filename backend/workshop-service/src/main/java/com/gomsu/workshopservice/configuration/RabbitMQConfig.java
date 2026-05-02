package com.gomsu.workshopservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REGISTRATION_QUEUE = "workshop_registration_queue";
    public static final String WORKSHOP_EXCHANGE = "workshop_exchange";
    public static final String REGISTRATION_ROUTING_KEY = "registration_routing_key";

    @Bean
    public Queue registrationQueue() {
        // Queue bền vững (durable = true) để không mất tin nhắn khi RabbitMQ restart
        return new Queue(REGISTRATION_QUEUE, true);
    }

    @Bean
    public TopicExchange workshopExchange() {
        return new TopicExchange(WORKSHOP_EXCHANGE);
    }

    @Bean
    public Binding registrationBinding(Queue registrationQueue, TopicExchange workshopExchange) {
        return BindingBuilder.bind(registrationQueue)
                .to(workshopExchange)
                .with(REGISTRATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Giúp Jackson xử lý được kiểu LocalDateTime (rất quan trọng cho Workshop)
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}