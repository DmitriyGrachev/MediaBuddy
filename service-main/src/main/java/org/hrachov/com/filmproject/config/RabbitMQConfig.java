package org.hrachov.com.filmproject.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String PREPROCESSOR_EXCHANGE = "preprocessor.exchange";
    public static final String PREPROCESSOR_QUEUE = "preprocessor.queue";
    public static final String PREPROCESSOR_ROUTING_KEY = "preprocessor.routing.key";
    // Для этого шага Retry не нужен, т.к. ошибки (неверный файл) обычно фатальны.
    // Просто отправляем сбойные сообщения в DLQ для анализа.
    private static final String PREPROCESSOR_DLX = "preprocessor.dlx";
    private static final String PREPROCESSOR_DLQ = "preprocessor.dlq";

    @Bean
    DirectExchange preprocessorExchange() {
        return new DirectExchange(PREPROCESSOR_EXCHANGE);
    }

    @Bean
    Queue preprocessorQueue() {
        return QueueBuilder.durable(PREPROCESSOR_QUEUE)
                .withArgument("x-dead-letter-exchange", PREPROCESSOR_DLX)
                .withArgument("x-message-ttl",  60_000)    // сообщения старше 1 минуты уйдут в DLX
                .withArgument("x-expires",      120_000)
                .withArgument("x-max-priority",10)
                .build();
    }

    @Bean
    Binding preprocessorBinding() {
        return BindingBuilder.bind(preprocessorQueue()).to(preprocessorExchange()).with(PREPROCESSOR_ROUTING_KEY);
    }

    @Bean
    DirectExchange preprocessorDlqExchange() {
        return new DirectExchange(PREPROCESSOR_DLX);
    }

    @Bean
    Queue preprocessorDeadLetterQueue() {
        return new Queue(PREPROCESSOR_DLQ);
    }

    @Bean
    Binding preprocessorDlqBinding() {
        return BindingBuilder.bind(preprocessorDeadLetterQueue()).to(preprocessorDlqExchange()).with(PREPROCESSOR_ROUTING_KEY);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReplyTimeout(300000);
        template.setMandatory(true); // обязательно

        // Confirm Callback
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("✅ Сообщение успешно доставлено до брокера. CorrelationData: {}", correlationData);
            } else {
                log.error("❌ Сообщение НЕ доставлено до брокера. Причина: {}", cause);
            }
        });

        // Return Callback
        template.setReturnsCallback(returnedMessage -> {
            log.error("❗ Сообщение возвращено брокером. Message: {}, ReplyCode: {}, ReplyText: {}, Exchange: {}, RoutingKey: {}",
                    returnedMessage.getMessage(),
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText(),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
        });

        return template;
    }
}