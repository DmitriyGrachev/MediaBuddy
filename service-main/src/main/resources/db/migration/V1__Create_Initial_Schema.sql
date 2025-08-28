-- Создание таблицы users
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       registration_date TIMESTAMP NOT NULL
);

-- Создание таблицы user_roles
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            roles VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, roles),
                            FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Создание таблицы genres
CREATE TABLE genres (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) UNIQUE NOT NULL
);

-- Создание таблицы films
CREATE TABLE films (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       release_year INTEGER,
                       description TEXT,
                       director VARCHAR(255),
                       rating DOUBLE PRECISION,
                       popularity DOUBLE PRECISION
);

-- Создание таблицы film_genres
CREATE TABLE film_genres (
                             film_id BIGINT NOT NULL,
                             genre_id BIGINT NOT NULL,
                             PRIMARY KEY (film_id, genre_id),
                             FOREIGN KEY (film_id) REFERENCES films(id),
                             FOREIGN KEY (genre_id) REFERENCES genres(id)
);

-- Создание таблицы movie
CREATE TABLE movie (
                       film_id BIGINT PRIMARY KEY,
                       duration INTEGER,
                       source VARCHAR(255),
                       poster VARCHAR(255),
                       FOREIGN KEY (film_id) REFERENCES films(id)
);

-- Создание таблицы serial
CREATE TABLE serial (
                        film_id BIGINT PRIMARY KEY,
                        seasons INTEGER,
                        episodes INTEGER,
                        poster VARCHAR(255),
                        FOREIGN KEY (film_id) REFERENCES films(id)
);

-- Создание таблицы seasons
CREATE TABLE seasons (
                         id BIGSERIAL PRIMARY KEY,
                         season_number INTEGER NOT NULL,
                         title VARCHAR(255),
                         serial_film_id BIGINT NOT NULL,
                         FOREIGN KEY (serial_film_id) REFERENCES serial(film_id)
);

-- Создание таблицы episodes
CREATE TABLE episodes (
                          id BIGSERIAL PRIMARY KEY,
                          episode_number INTEGER NOT NULL,
                          title VARCHAR(255) NOT NULL,
                          source VARCHAR(255) NOT NULL,
                          duration INTEGER,
                          season_id BIGINT NOT NULL,
                          FOREIGN KEY (season_id) REFERENCES seasons(id)
);

-- Создание таблицы comments
CREATE TABLE comments (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          film_id BIGINT NOT NULL,
                          text TEXT,
                          time TIMESTAMP,
                          parent_comment_id BIGINT,
                          FOREIGN KEY (user_id) REFERENCES users(id),
                          FOREIGN KEY (film_id) REFERENCES films(id),
                          FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
);

-- Создание таблицы reactions
CREATE TABLE reactions (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           comment_id BIGINT NOT NULL,
                           type VARCHAR(50) NOT NULL,
                           created_at TIMESTAMP NOT NULL,
                           FOREIGN KEY (user_id) REFERENCES users(id),
                           FOREIGN KEY (comment_id) REFERENCES comments(id)
);

-- Создание таблицы video_directory
CREATE TABLE video_directory (
                                 id BIGSERIAL PRIMARY KEY,
                                 description TEXT,
                                 user_id BIGINT NOT NULL,
                                 FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Создание таблицы video_frame
CREATE TABLE video_frame (
                             id BIGSERIAL PRIMARY KEY,
                             video_directory_id BIGINT NOT NULL,
                             title VARCHAR(255),
                             description TEXT,
                             url VARCHAR(255),
                             FOREIGN KEY (video_directory_id) REFERENCES video_directory(id)
);

-- Создание таблицы watch_history
CREATE TABLE watch_history (
                               id SERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               film_id BIGINT NOT NULL,
                               date TIMESTAMP,
                               last_position_in_seconds DOUBLE PRECISION,
                               FOREIGN KEY (user_id) REFERENCES users(id),
                               FOREIGN KEY (film_id) REFERENCES films(id)
);
