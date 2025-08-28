package org.hrachov.com.filmproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;



@SpringBootApplication
// Сканируем entities только в вашем пакете с моделями
/*@EntityScan("org.hrachov.com.filmproject.model")
@EnableJpaRepositories(
        basePackages = "org.hrachov.com.filmproject.repository.jpa",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
// Отдельно подключаем Mongo и Redis, если нужно
@EnableMongoRepositories(basePackages = "org.hrachov.com.filmproject.repository.mongo")
*/
public class FilmProjectApplication {


    public static void main(String[] args) {
        SpringApplication.run(FilmProjectApplication.class, args);
    }

}
