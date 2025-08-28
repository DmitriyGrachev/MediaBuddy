package org.hrachov.com.filmproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.stream.Environment;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;


@Configuration
public class RabbitMQHistory {

    @Bean
    public Queue historyStreamQueue() {
        //TODO Закоментировал ttl потому что streams не поддерживают ttl
        return QueueBuilder.durable("history.stream")
                //.withArgument("x-message-ttl",  600_000)    // сообщения старше 1 минуты уйдут в DLX
                .stream()
                .build();
    }

    @Bean
    public RabbitStreamTemplate streamTemplate(Environment env, ObjectMapper mapper) {
        RabbitStreamTemplate template = new RabbitStreamTemplate(env, "history.stream");
        //template.setMessageConverter(new Jackson2JsonMessageConverter(mapper));
        template.setProducerCustomizer((name, builder) -> builder.name("prod-1"));
        return template;
    }


    //streamTemplate.convertAndSend(myDto); // автосериализация

    public static final String QUEUE_NAME = "history.queue";
    public static final String EXCHANGE_NAME = "history.exchange";
    public static final String ROUTING_KEY = "history.routing.key";

    @Bean
    public Queue historyQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange historyExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding historyBinding() {
        return BindingBuilder.bind(historyQueue()).to(historyExchange()).with(ROUTING_KEY);
    }

    @Bean
    public RabbitAdmin rabbitAdminHistory(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(historyStreamQueue()); // Явно декларируем стрим при старте
        return admin;    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplateHistory(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}