package com.hrachovhistoryservice.microserviceforhistory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.stream.Consumer;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;



@Configuration
public class RabbitMQHistory {
    @Bean
    public Queue historyStreamQueue() {
        return QueueBuilder.durable("history.stream")
                .stream()
                .build();
    }

    @Bean
    public RabbitAdmin rabbitAdminHistory(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareQueue(historyStreamQueue()); // Явно декларируем стрим при старте
        return admin;
    }

    // Остальная часть конфигурации остается без изменений
    @Bean
    public RabbitStreamTemplate streamTemplate(Environment env, ObjectMapper mapper) {
        RabbitStreamTemplate template = new RabbitStreamTemplate(env, "history.stream");
        //template.setMessageConverter(new Jackson2JsonMessageConverter(mapper));
        template.setProducerCustomizer((name, builder) -> builder.name("prod-1"));
        return template;
    }

    // Simple factory for stream listeners
    @Bean("nativeFactory")
    public RabbitListenerContainerFactory<StreamListenerContainer> nativeFactory(Environment env) {
        StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(env);

        factory.setConsumerCustomizer((id, builder) -> {
            builder.name("history-consumer")
                    .offset(OffsetSpecification.first());
        });

        return factory;
    }

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

    //@Bean
    //public RabbitAdmin rabbitAdminHistory(ConnectionFactory connectionFactory) {
    //    return new RabbitAdmin(connectionFactory);
    //}

    @Bean
    public RabbitTemplate rabbitTemplateHistory(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //TODO не уверен что ответ ожидать обязательно
        rabbitTemplate.setMandatory(false);
        return rabbitTemplate;

    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}