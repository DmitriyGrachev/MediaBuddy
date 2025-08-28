
-- 1. Insert Users (MUST be done first to satisfy foreign key constraints)
INSERT INTO users (username, email, password, first_name, last_name, registration_date) VALUES
                                                                                                ( 'user1', 'user1@example.com', 'password123', 'John', 'Doe', CURRENT_TIMESTAMP),
                                                                                                ( 'user2', 'user2@example.com', 'password123', 'Jane', 'Smith', CURRENT_TIMESTAMP),
                                                                                                ( 'user3', 'user3@example.com', 'password123', 'Michael', 'Johnson', CURRENT_TIMESTAMP),
                                                                                                ( 'user4', 'user4@example.com', 'password123', 'Emily', 'Williams', CURRENT_TIMESTAMP),
                                                                                                ( 'user5', 'user5@example.com', 'password123', 'David', 'Brown', CURRENT_TIMESTAMP),
                                                                                                ('admin','admin','$2a$12$iOcpR7wcN9E/rpsA91feeusA9PPBdsugpUqXbeddmEWeFqOgMgak.','admin','admin',CURRENT_TIMESTAMP);
--Добавление роли ROLE_ADMIN для пользователя admin
INSERT INTO user_roles (USER_ID, ROLES)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin';

--Добавление роли ROLE_REGULAR для всех остальных пользователей
INSERT INTO user_roles (USER_ID, ROLES)
SELECT id, 'ROLE_REGULAR' FROM users WHERE username != 'admin';

-- 2. Insert Genres
INSERT INTO genres (name) VALUES
                              ('Action'),
                              ('Drama'),
                              ('Sci-Fi'),
                              ('Thriller'),
                              ('Comedy'),
                              ('Romance'),
                              ('Fantasy'),
                              ('Crime');

-- 3. Insert Films
INSERT INTO films (title, release_year, description, director, rating, popularity) VALUES
-- Movies
('Inception',               2010, 'A thief who steals corporate secrets through dream infiltration.',     'Christopher Nolan',            8.8, 1580000),
('The Matrix',              1999, 'A hacker discovers the reality is a simulation.',                     'Wachowski Sisters',            8.7, 1750000),
('Interstellar',            2014, 'A team travels through a wormhole to ensure humanitys survival.',    'Christopher Nolan',            8.6, 1700000),
('The Dark Knight',         2008, 'Batman faces the Joker in Gotham City.',                              'Christopher Nolan',            9.0, 2300000),
('Fight Club',              1999, 'An insomniac meets a soap salesman and forms an underground club.',  'David Fincher',                8.8, 1600000),
('Blade Runner 2049',       2017, 'A new blade runner unearths a long-buried secret.',                  'Denis Villeneuve',             8.0, 1200000),
('Dune',                    2021, 'A noble family fights for control of the desert planet Arrakis.',     'Denis Villeneuve',             8.1, 1500000),
('The Prestige',            2006, 'Two stage magicians engage in a battle to create the ultimate illusion.', 'Christopher Nolan',         8.5, 1100000),
('Mad Max: Fury Road',      2015, 'In a post-apocalyptic wasteland, Max teams up with Furiosa.',        'George Miller',                8.1, 1300000),
('Parasite',                2019, 'Greed and class discrimination threaten a newly formed symbiosis.', 'Bong Joon-ho',                 8.6, 1900000),
('Avengers: Endgame',       2019, 'The Avengers assemble once more to undo Thanoss actions.',          'Anthony & Joe Russo',          8.4, 2400000),
('John Wick',               2014, 'An ex-hitman comes out of retirement to track down the gangsters.',  'Chad Stahelski',               7.4, 1650000),
('The Shawshank Redemption',1994, 'Two imprisoned men bond over years, finding solace and redemption.', 'Frank Darabont',               9.3, 2100000),
('The Godfather',           1972, 'The aging patriarch of a crime dynasty transfers control to his son.', 'Francis Ford Coppola',      9.2, 2300000),
('Pulp Fiction',            1994, 'The lives of two mob hitmen, a boxer, a gangster and his wife intertwine.', 'Quentin Tarantino',        8.9, 1950000),
-- Serials
('Breaking Bad',            2008, 'A chemistry teacher turns to drug production.',                        'Vince Gilligan',            9.5, 2250000),
('Stranger Things',         2016, 'Kids uncover supernatural mysteries in their small town.',             'Duffer Brothers',           8.7, 2100000),
('The Office',              2005, 'A mockumentary about office workers at Dunder Mifflin.',               'Greg Daniels',             9.0, 2200000),
('Friends',                 1994, 'Six friends navigate life and love in New York City.',                 'David Crane & Marta Kauffman', 8.9, 2500000),
('House of the Dragon',     2022, 'The Targaryen civil war set 200 years before Game of Thrones.',        'Ryan Condal',              8.5, 1750000),
('The Last of Us',          2023, 'A smuggler escorts a girl across post-apocalyptic America.',            'Craig Mazin',              9.1, 1850000),
('Sherlock',                2010, 'A modern adaptation of Sherlock Holmes.',                              'Mark Gatiss & Steven Moffat', 9.1, 2000000),
('True Detective',          2014, 'Anthology crime drama with dark and twisted stories.',                 'Nic Pizzolatto',           9.0, 1700000),
('Narcos',                  2015, 'Chronicles the rise of drug kingpin Pablo Escobar.',                   'Chris Brancato',           8.8, 1500000),
('Chernobyl',               2019, 'A dramatization of the 1986 nuclear disaster.',                        'Craig Mazin',              9.4, 1650000);

-- 4. Insert Movie Records
INSERT INTO movie (film_id, duration, source, poster) VALUES
                                                              (1,  148, 'video.mp4', '/images/inception.jpg'),
                                                              (2,  136, 'video.mp4', '/images/matrix.jpg'),
                                                              (3,  169, 'video.mp4', '/images/interstellar.jpg'),
                                                              (4,  152, 'video.mp4', '/images/thedarkknight.jpg'),
                                                              (5,  139, 'video.mp4', '/images/fightclub.jpg'),
                                                              (6,  163, 'video.mp4', '/images/bladerunner2049.jpg'),
                                                              (7,  155, 'video.mp4', '/images/dune.jpg'),
                                                              (8,  130, 'video.mp4', '/images/theprestige.jpg'),
                                                              (9,  120, 'video.mp4', '/images/madmaxfuryroad.jpg'),
                                                              (10, 132, 'video.mp4', '/images/parasite.jpg'),
                                                              (11, 181, 'video.mp4', '/images/avengersendgame.jpg'),
                                                              (12, 101, 'video.mp4', '/images/johnwick.jpg'),
                                                              (13, 142, 'video.mp4', '/images/theshawshankredemption.jpg'),
                                                              (14, 175, 'video.mp4', '/images/thegodfather.jpg'),
                                                              (15, 154, 'video.mp4', '/images/pulpfiction.jpg');

-- 3. Insert Serial-specific metadata (две сезонов и четыре эпизода у каждого)
INSERT INTO serial (film_id, seasons, episodes, poster)
VALUES
    (16, 2, 4, '/images/breakingbad.jpg'),
    (17, 2, 4, '/images/strangerthings.jpg'),
    (18, 2, 4, '/images/theoffice.jpg'),
    (19, 2, 4, '/images/friends.jpg'),
    (20, 2, 4, '/images/houseofthedragon.jpg'),
    (21, 2, 4, '/images/thelastofus.jpg'),
    (22, 2, 4, '/images/sherlock.jpg'),
    (23, 2, 4, '/images/truedetective.jpg'),
    (24, 2, 4, '/images/narcos.jpg'),
    (25, 2, 4, '/images/chernobyl.jpg');
-- 4. Insert Seasons: по 2 сезона на каждый сериал
INSERT INTO seasons (season_number, title, serial_film_id) VALUES
                                                               -- для каждого film_id от 16 до 25
                                                               (1, 'Season 1', 16),
                                                               (2, 'Season 2', 16),
                                                               (1, 'Season 1', 17),
                                                               (2, 'Season 2', 17),
                                                               (1, 'Season 1', 18),
                                                               (2, 'Season 2', 18),
                                                               (1, 'Season 1', 19),
                                                               (2, 'Season 2', 19),
                                                               (1, 'Season 1', 20),
                                                               (2, 'Season 2', 20),
                                                               (1, 'Season 1', 21),
                                                               (2, 'Season 2', 21),
                                                               (1, 'Season 1', 22),
                                                               (2, 'Season 2', 22),
                                                               (1, 'Season 1', 23),
                                                               (2, 'Season 2', 23),
                                                               (1, 'Season 1', 24),
                                                               (2, 'Season 2', 24),
                                                               (1, 'Season 1', 25),
                                                               (2, 'Season 2', 25);


-- 5. Insert Episodes: по 2 эпизода в каждом сезоне
-- Предполагаем, что id сезонов присвоены в порядке вставки (например, от 1 до 20).
-- Если вы не знаете exact season_id, сначала можно сделать SELECT id, serial_film_id, season_number FROM seasons
-- и затем подставить реальные season_id. Здесь для примера возьмём подряд 1–20.
INSERT INTO episodes (episode_number, title, source, duration, season_id) VALUES
                                                                              -- Сериал 16, Season 1 (season_id = 1)
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 45, 1),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 50, 1),
                                                                              -- Сериал 16, Season 2 (season_id = 2)
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 47, 2),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 52, 2),

                                                                              -- Сериал 17, Season 1 (season_id = 3)
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 44, 3),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 49, 3),
                                                                              -- Сериал 17, Season 2 (season_id = 4)
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 46, 4),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 51, 4),

                                                                              -- И так далее для всех serial_film_id 18–25 и season_id 5–20
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 42, 5),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 48, 5),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 45, 6),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 50, 6),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 43, 7),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 47, 7),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 44, 8),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 49, 8),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 46, 9),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 51, 9),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 45,10),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 52,10),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 44,11),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 50,11),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 47,12),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 53,12),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 45,13),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 49,13),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 46,14),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 51,14),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 43,15),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 48,15),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 44,16),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 50,16),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 45,17),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 51,17),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 46,18),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 52,18),

                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 44,19),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 49,19),
                                                                              (1, 'Episode 1', '/static/videos/video.mp4', 47,20),
                                                                              (2, 'Episode 2', '/static/videos/video.mp4', 53,20);
-- 6. Connect Films with Genres
INSERT INTO film_genres (film_id, genre_id) VALUES
-- Movies
(1, 3),(1, 4),
(2, 1),(2, 3),
(3, 3),(3, 2),
(4, 1),(4, 4),
(5, 4),(5, 2),
(6, 3),(6, 4),
(7, 3),(7, 7),
(8, 4),(8, 2),
(9, 1),(9, 6),
(10,3),(10,2),
(11,1),(11,4),
(12,1),(12,4),
(13,2),(13,4),
(14,2),(14,4),
(15,1),(15,4),
-- Serials
(16,2),(16,4),
(17,3),(17,2),
(18,5),(18,2),
(19,5),(19,2),
(20,4),(20,8),
(21,2),(21,8),
(22,8),(22,4),
(23,2),(23,6),
(24,1),(24,4),
(25,2),(25,4);

-- 7. Insert Comments
INSERT INTO comments (user_id, film_id, text, time) VALUES
                                                        (1,  1, 'Absolutely mind-bending! Nolan at his best.', CURRENT_TIMESTAMP),
                                                        (2,  3, 'Interstellar blew my mind with its visuals.', CURRENT_TIMESTAMP),
                                                        (3,  5, 'Fight Club is a masterpiece of modern cinema.', CURRENT_TIMESTAMP),
                                                        (4,  7, 'Dune''s world-building is incredible.', CURRENT_TIMESTAMP),
                                                        (5, 11, 'Endgame was an epic conclusion to the saga.', CURRENT_TIMESTAMP),
                                                        (2, 16, 'Breaking Bad is the best series ever made.', CURRENT_TIMESTAMP),
                                                        (3, 17, 'Stranger Things has perfect 80s nostalgia.', CURRENT_TIMESTAMP),
                                                        (1, 18, 'The Office never gets old—hilarious every time.', CURRENT_TIMESTAMP),
                                                        (4, 19, 'Friends defined my childhood.', CURRENT_TIMESTAMP),
                                                        (5, 20, 'House of the Dragon is visually stunning.', CURRENT_TIMESTAMP),
                                                        (2, 21, 'The Last of Us captures the game''s emotion so well.', CURRENT_TIMESTAMP),
                                                        (3, 22, 'Sherlock''s writing and acting are top-notch.', CURRENT_TIMESTAMP),
                                                        (1, 23, 'True Detective''s first season was groundbreaking.', CURRENT_TIMESTAMP),
                                                        (4, 24, 'Narcos is both thrilling and informative.', CURRENT_TIMESTAMP),
                                                        (5, 25, 'Chernobyl gave me chills—so well done.', CURRENT_TIMESTAMP);
UPDATE comments SET time = CURRENT_TIMESTAMP WHERE time IS NULL;

-- 1. Insert the directory
INSERT INTO video_directory (description,user_id)  --  Added id
VALUES ('Motivational',1); --  Assuming an ID of 1, you can change this

-- 2. Insert the first video frame (video) and associate it with the directory iVnkwVv9dnk

INSERT INTO video_frame (video_directory_id, title, description, url)
VALUES (1, 'Video 1 Title', 'Description of Video 1', 'https://www.youtube.com/embed/yBrRpb8aLwk');

-- 3. Insert the second video frame (video) and associate it with the same directory
INSERT INTO video_frame (video_directory_id, title, description, url)
VALUES (1, 'Video 2 Title', 'Description of Video 2', 'https://www.youtube.com/embed/iow5V3Qlvwo');
