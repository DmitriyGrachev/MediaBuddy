package com.hrachovcompressionservice.microserviceforvideocompression.config;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class VideoProcessingQueueConfig {

    // =======================================================================================
    // == ОБЩИЕ КОНФИГУРАЦИОННЫЕ БИНЫ
    // =======================================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

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

    public static final String COMPRESSION_EXCHANGE = "compression.exchange";
    public static final String COMPRESSION_QUEUE = "compression.queue";
    public static final String COMPRESSION_ROUTING_KEY = "compression.routing.key";
    // Для компрессии Retry крайне важен, т.к. это долгий и ресурсоемкий процесс.
    private static final String COMPRESSION_RETRY_QUEUE = "compression.queue.retry";
    private static final String COMPRESSION_DLX = "compression.exchange.dlx";
    private static final String COMPRESSION_DLQ = "compression.queue.dlq";
    private static final int COMPRESSION_RETRY_DELAY_MS = 30000; // 30 секунд

    @Bean
    DirectExchange compressionExchange() {
        return new DirectExchange(COMPRESSION_EXCHANGE);
    }

    @Bean
    Queue compressionQueue() {
        return QueueBuilder.durable(COMPRESSION_QUEUE)
                .withArgument("x-dead-letter-exchange", COMPRESSION_DLX)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding compressionBinding() {
        return BindingBuilder.bind(compressionQueue()).to(compressionExchange()).with(COMPRESSION_ROUTING_KEY);
    }

    @Bean
    DirectExchange compressionDlx() {
        return new DirectExchange(COMPRESSION_DLX);
    }

    @Bean
    Queue compressionRetryQueue() {
        return QueueBuilder.durable(COMPRESSION_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", COMPRESSION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", COMPRESSION_ROUTING_KEY)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding compressionRetryBinding() {
        return BindingBuilder.bind(compressionRetryQueue()).to(compressionDlx()).with(COMPRESSION_ROUTING_KEY);
    }

    @Bean
    Queue compressionDlq() {
        return new Queue(COMPRESSION_DLQ);
    }

    @Bean
    Binding compressionDlqBinding() {
        // У DLQ будет свой ключ, чтобы отделить от логики retry.
        return BindingBuilder.bind(compressionDlq()).to(compressionDlx()).with("dlq-key");
    }

    public static final String THUMBNAIL_EXCHANGE = "thumbnail.exchange";
    public static final String THUMBNAIL_QUEUE = "thumbnail.queue";
    public static final String THUMBNAIL_ROUTING_KEY = "thumbnail.routing.key";
    // У этого шага тоже есть Retry на случай кратковременных сбоев.
    private static final String THUMBNAIL_RETRY_QUEUE = "thumbnail.queue.retry";
    private static final String THUMBNAIL_DLX = "thumbnail.exchange.dlx";
    private static final String THUMBNAIL_DLQ = "thumbnail.queue.dlq";
    private static final int THUMBNAIL_RETRY_DELAY_MS = 10000; // 10 секунд

    @Bean
    DirectExchange thumbnailExchange() {
        return new DirectExchange(THUMBNAIL_EXCHANGE);
    }

    @Bean
    Queue thumbnailQueue() {
        return QueueBuilder.durable(THUMBNAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", THUMBNAIL_DLX)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding thumbnailBinding() {
        return BindingBuilder.bind(thumbnailQueue()).to(thumbnailExchange()).with(THUMBNAIL_ROUTING_KEY);
    }

    @Bean
    DirectExchange thumbnailDlx() {
        return new DirectExchange(THUMBNAIL_DLX);
    }

    @Bean
    Queue thumbnailRetryQueue() {
        return QueueBuilder.durable(THUMBNAIL_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", THUMBNAIL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", THUMBNAIL_ROUTING_KEY)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding thumbnailRetryBinding() {
        return BindingBuilder.bind(thumbnailRetryQueue()).to(thumbnailDlx()).with(THUMBNAIL_ROUTING_KEY);
    }

    @Bean
    Queue thumbnailDlq() {
        return new Queue(THUMBNAIL_DLQ);
    }

    @Bean
    Binding thumbnailDlqBinding() {
        return BindingBuilder.bind(thumbnailDlq()).to(thumbnailDlx()).with("dlq-key");
    }

    public static final String METADATA_EXCHANGE = "metadata.exchange";
    public static final String METADATA_QUEUE = "metadata.queue";
    public static final String METADATA_ROUTING_KEY = "metadata.routing.key";
    // Финальный шаг также имеет свой механизм Retry.
    private static final String METADATA_RETRY_QUEUE = "metadata.queue.retry";
    private static final String METADATA_DLX = "metadata.exchange.dlx";
    private static final String METADATA_DLQ = "metadata.queue.dlq";
    private static final int METADATA_RETRY_DELAY_MS = 10000; // 10 секунд

    @Bean
    DirectExchange metadataExchange() {
        return new DirectExchange(METADATA_EXCHANGE);
    }

    @Bean
    Queue metadataQueue() {
        return QueueBuilder.durable(METADATA_QUEUE)
                .withArgument("x-dead-letter-exchange", METADATA_DLX)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding metadataBinding() {
        return BindingBuilder.bind(metadataQueue()).to(metadataExchange()).with(METADATA_ROUTING_KEY);
    }

    @Bean
    DirectExchange metadataDlx() {
        return new DirectExchange(METADATA_DLX);
    }

    @Bean
    Queue metadataRetryQueue() {
        return QueueBuilder.durable(METADATA_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", METADATA_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", METADATA_ROUTING_KEY)
                .withArgument("x-message-ttl",  30_000)
                .withArgument("x-expires",      60_000)
                .build();
    }

    @Bean
    Binding metadataRetryBinding() {
        return BindingBuilder.bind(metadataRetryQueue()).to(metadataDlx()).with(METADATA_ROUTING_KEY);
    }

    @Bean
    Queue metadataDlq() {
        return new Queue(METADATA_DLQ);
    }

    @Bean
    Binding metadataDlqBinding() {
        return BindingBuilder.bind(metadataDlq()).to(metadataDlx()).with("dlq-key");
    }
}
