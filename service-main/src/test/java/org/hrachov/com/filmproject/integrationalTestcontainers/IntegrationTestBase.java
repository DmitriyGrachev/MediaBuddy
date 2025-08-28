package org.hrachov.com.filmproject.integrationalTestcontainers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
public abstract class IntegrationTestBase {

    // 1) Оставляем статические поля контейнеров, но убираем @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("mediabuddy")
                    .withUsername("user")
                    .withPassword("password");

    static final MongoDBContainer mongo =
            new MongoDBContainer("mongo:7.0");

    static final GenericContainer<?> redis =
            new GenericContainer<>("redis/redis-stack:latest")
                    .withExposedPorts(6379);

    static final RabbitMQContainer rabbitmq =
            new RabbitMQContainer("rabbitmq:3-management")
                    .withExposedPorts(5672, 15672, 61613)
                    .withPluginsEnabled("rabbitmq_stomp")
                    .withAdminPassword("secret");

    static final GenericContainer<?> elasticsearch =
            new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:8.13.4")
                    .withExposedPorts(9200)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");

    // 2) Стартуем всё один раз при загрузке класса
    static {
        postgres.start();
        mongo.start();
        redis.start();
        rabbitmq.start();
        elasticsearch.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.stomp.port", () -> rabbitmq.getMappedPort(61613));

        registry.add("spring.elasticsearch.uris",
                () -> "http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200));


        registry.add("spring.rabbitmq.relay.host",         rabbitmq::getHost);
        registry.add("spring.rabbitmq.relay.port",         () -> rabbitmq.getMappedPort(61613));
        registry.add("spring.rabbitmq.relay.username",     rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.relay.password",     rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.relay.virtual-host", () -> "/");
// при необходимости system‑логин/пароль можно тоже прокинуть
        registry.add("spring.rabbitmq.relay.system-login",    rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.relay.system-passcode", rabbitmq::getAdminPassword);

    }
}
