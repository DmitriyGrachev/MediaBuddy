// /js/filmSearch.js

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('advancedSearchForm');
    const resultsContainer = document.getElementById('resultsContainer');
    const genresContainer = document.getElementById('genresContainer');

    // Динамические блоки опций
    const movieOptions = document.getElementById('movieOptions');
    const serialOptions = document.getElementById('serialOptions');

    // Список жанров (можно получать с бэкенда, но для примера используем ваш список)
    //const genres = ['Action', 'Drama', 'Sci-Fi', 'Thriller', 'Comedy', 'Romance', 'Fantasy', 'Crime'];
    const token = sessionStorage.getItem("token");

    (async () => {
        const genres = await getGenres();
        populateGenres(genres);
    })();

    // Fetch genres from backend
    async function getGenres(){
        try {
            const response = await fetch("/api/search/films/genres", {
                method: "GET",
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });
            const data = await response.json();
            // if the API returns { genres: […] } unwrap it, otherwise
            // if it's already an array, just use it
            if (Array.isArray(data)) {
                return data;
            } else if (Array.isArray(data.genres)) {
                return data.genres;
            } else {
                console.warn("Unexpected genres payload", data);
                return [];
            }
        } catch (error) {
            console.error('Ошибка при получении жанров:', error);
            return [];
        }
    }

    // Populate checkbox list
    function populateGenres(genres) {
        if (!Array.isArray(genres) || genres.length === 0) {
            genresContainer.innerHTML =
                '<p class="text-muted">Жанры не найдены.</p>';
            return;
        }

        genres.forEach(genre => {
            const div = document.createElement('div');
            div.classList.add('form-check', 'form-check-inline');
            div.innerHTML = `
      <input class="form-check-input"
             type="checkbox"
             name="genres"
             id="genre_${genre}"
             value="${genre}">
      <label class="form-check-label"
             for="genre_${genre}">
        ${genre}
      </label>
    `;
            genresContainer.appendChild(div);
        });
    }


    // --- 2. ОБРАБОТЧИКИ СОБЫТИЙ ---

    // Обработчик отправки формы
    form.addEventListener('submit', async function (event) {
        event.preventDefault(); // Предотвращаем стандартную отправку формы
        await handleSearch();
    });

    // Обработчик сброса формы
    form.addEventListener('reset', function() {
        // При сбросе также скрываем все доп. опции
        movieOptions.style.display = 'none';
        serialOptions.style.display = 'none';
        resultsContainer.innerHTML = '<div class="col"><p class="text-muted">Фильтры сброшены. Задайте новые параметры для поиска.</p></div>';
    });

    // Обработчик изменения типа "Фильм/Сериал"
    form.querySelectorAll('input[name="filmType"]').forEach(radio => {
        radio.addEventListener('change', function (event) {
            toggleSpecificOptions(event.target.value);
        });
    });

    // --- 3. ОСНОВНАЯ ЛОГИКА ---

    // Функция для показа/скрытия доп. опций
    function toggleSpecificOptions(type) {
        if (type === 'movie') {
            movieOptions.style.display = 'block';
            serialOptions.style.display = 'none';
        } else if (type === 'serial') {
            movieOptions.style.display = 'none';
            serialOptions.style.display = 'block';
        } else { // для "Все"
            movieOptions.style.display = 'none';
            serialOptions.style.display = 'none';
        }
    }

    // Главная функция поиска
    async function handleSearch() {
        const formData = new FormData(form);
        const searchCriteria = {};

        // Собираем данные из формы в объект, который ожидает бэкенд
        formData.forEach((value, key) => {
            // Пропускаем пустые значения
            if (!value) return;

            // Специальная обработка для чекбоксов жанров
            if (key === 'genres') {
                if (!searchCriteria[key]) {
                    searchCriteria[key] = [];
                }
                searchCriteria[key].push(value);
            } else {
                searchCriteria[key] = value;
            }
        });

        resultsContainer.innerHTML = '<div class="col"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';

        try {
            // Укажите правильный URL вашего эндпоинта
            const response = await fetch('/api/search/films', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(searchCriteria),
            });

            if (!response.ok) {
                throw new Error(`Ошибка сети: ${response.statusText}`);
            }

            const films = await response.json();
            renderResults(films);

        } catch (error) {
            console.error('Ошибка при поиске:', error);
            resultsContainer.innerHTML = '<div class="col"><p class="text-danger">Произошла ошибка при выполнении поиска. Пожалуйста, попробуйте еще раз.</p></div>';
        }
    }

    // Функция для отрисовки результатов
    function renderResults(films) {
        resultsContainer.innerHTML = ''; // Очищаем предыдущие результаты

        if (!films || films.length === 0) {
            resultsContainer.innerHTML = '<div class="col"><p class="text-muted">Ничего не найдено. Попробуйте изменить параметры поиска.</p></div>';
            return;
        }

        films.forEach(film => {
            // Динамически определяем URL в зависимости от типа
            const detailUrl = film.type === 'movie' ? `/movie/${film.id}` : `/serial/${film.id}`;
            const posterUrl = film.poster ? film.poster : '/images/placeholder.png'; // Заглушка, если нет постера

            const cardHTML = `
                <div class="col">
                    <div class="card h-100 shadow-sm">
                        <a href="${detailUrl}" class="text-decoration-none text-dark">
                            <img src="${posterUrl}" class="card-img-top" alt="${film.title}" style="height: 400px; object-fit: cover;">
                            <div class="card-body">
                                <h5 class="card-title">${film.title}</h5>
                                <p class="card-text text-muted">${film.releaseYear}</p>
                                <div class="d-flex justify-content-between align-items-center">
                                    <span class="badge bg-primary">${film.type === 'movie' ? 'Фильм' : 'Сериал'}</span>
                                    <span class="fw-bold">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-star-fill text-warning" viewBox="0 0 16 16"><path d="M3.612 15.443c-.386.198-.824-.149-.746-.592l.83-4.73L.173 6.765c-.329-.314-.158-.888.283-.95l4.898-.696L7.538.792c.197-.39.73-.39.927 0l2.184 4.327 4.898.696c.441.062.612.636.282.95l-3.522 3.356.83 4.73c.078.443-.36.79-.746.592L8 13.187l-4.389 2.256z"/></svg>
                                        ${film.rating}
                                    </span>
                                </div>
                            </div>
                        </a>
                    </div>
                </div>
            `;
            resultsContainer.insertAdjacentHTML('beforeend', cardHTML);
        });
    }

    // --- 4. ЗАПУСК ---
     populateGenres(); // Заполняем чекбоксы жанров при загрузке страницы
});
