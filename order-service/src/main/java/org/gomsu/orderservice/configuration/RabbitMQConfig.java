package org.gomsu.orderservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "order-exchange";
    public static final String QUEUE = "product-restock-queue";
    public static final String ROUTING_KEY = "order.cancel";

    @Bean
    public DirectExchange exchange() { return new DirectExchange(EXCHANGE); }
    @Bean public Queue queue() { return new Queue(QUEUE); }
    @Bean public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // Quan trọng: Để gửi/nhận dạng JSON thay vì Byte khó đọc
    @Bean public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
