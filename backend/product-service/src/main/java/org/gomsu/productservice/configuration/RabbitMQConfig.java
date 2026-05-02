package org.gomsu.productservice.configuration;

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

    // --- 2. CẤU HÌNH CHO REVIEW (REVIEW UPDATE) ---
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_QUEUE = "product-review-queue";
    public static final String REVIEW_ROUTING_KEY = "review.update.key";

    @Bean
    public DirectExchange exchange() { return new DirectExchange(EXCHANGE); }
    @Bean public Queue queue() { return new Queue(QUEUE); }
    @Bean public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_KEY);
    }

    // Quan trọng: Để gửi/nhận dạng JSON thay vì Byte khó đọc
    @Bean public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- BEANS CHO REVIEW (PHẢI THÊM CÁI NÀY) ---
    @Bean
    public DirectExchange reviewExchange() { return new DirectExchange(REVIEW_EXCHANGE); }

    @Bean
    public Queue reviewQueue() { return new Queue(REVIEW_QUEUE); }

    @Bean
    public Binding reviewBinding() {
        // Gọi trực tiếp hàm để đảm bảo lấy đúng Bean "reviewQueue" và "reviewExchange"
        return BindingBuilder.bind(reviewQueue()).to(reviewExchange()).with(REVIEW_ROUTING_KEY);
    }

}
