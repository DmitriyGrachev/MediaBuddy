package org.hrachov.com.filmproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfiguration {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Сериализатор для обычных ключей
        template.setKeySerializer(stringSerializer);
        // Сериализатор для обычных значений
        // Если значения могут быть сложными объектами, лучше использовать Jackson2JsonRedisSerializer
        // В вашем случае BlockService сохраняет Map<String, Object>, где значения - строки.
        // Но для универсальности Object, Jackson может быть лучше. Для строк - StringRedisSerializer.
        // Для простоты и если значения в основном строки или примитивы, StringRedisSerializer для hashValue подойдет.
        template.setValueSerializer(stringSerializer); // или Jackson2JsonRedisSerializer

        // Сериализатор для ключей полей хеша (hash keys)
        template.setHashKeySerializer(stringSerializer);
        // Сериализатор для значений полей хеша (hash values)
        template.setHashValueSerializer(stringSerializer); // или Jackson2JsonRedisSerializer, если значения сложнее строк

        template.afterPropertiesSet();
        return template;
    }
}
