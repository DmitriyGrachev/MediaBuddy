-- Добавляем новые поля в таблицу films
ALTER TABLE films
    ADD COLUMN user_rating_sum BIGINT DEFAULT 0,
    ADD COLUMN user_rating_count BIGINT DEFAULT 0;

-- Создаем таблицу user_ratings
CREATE TABLE user_ratings (
                              id SERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              film_id BIGINT NOT NULL,
                              rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 10),

                              CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                              CONSTRAINT fk_film FOREIGN KEY (film_id) REFERENCES films (id) ON DELETE CASCADE,
                              CONSTRAINT uq_user_film UNIQUE (user_id, film_id)
);
