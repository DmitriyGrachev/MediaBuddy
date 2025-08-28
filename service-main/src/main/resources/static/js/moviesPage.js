document.addEventListener('DOMContentLoaded', () => {
    const moviesListContainer = document.getElementById('moviesListContainer');
    const pageTitle = document.getElementById('pageTitle');
    const loadingIndicator = document.getElementById('loadingIndicator');
    const paginationContainer = document.getElementById('paginationContainer');

    let currentPage = 0;
    const pageSize = 10; // Количество фильмов на странице, можно сделать настраиваемым

    const urlParams = new URLSearchParams(window.location.search);
    const genre = urlParams.get('genre');

    function updatePageTitle() {
        if (genre) {
            pageTitle.textContent = `Фильмы: ${decodeURIComponent(genre)}`;
        } else {
            pageTitle.textContent = 'Все фильмы';
        }
    }

    async function fetchMovies(page = 0) {
        loadingIndicator.style.display = 'block';
        moviesListContainer.innerHTML = ''; // Очищаем предыдущие фильмы
        paginationContainer.innerHTML = ''; // Очищаем пагинацию

        let apiUrl = `/api/movies?page=${page}&size=${pageSize}&sortBy=releaseYear&sortDir=desc`; // Сортировка по году выпуска, новые сверху
        if (genre) {
            apiUrl += `&genre=${encodeURIComponent(genre)}`;
        }

        try {
            const response = await fetch(apiUrl);
            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }
            const pagedResponse = await response.json();
            renderMovies(pagedResponse.content);
            renderPagination(pagedResponse.totalPages, pagedResponse.number);
            currentPage = pagedResponse.number;
        } catch (error) {
            console.error('Не удалось загрузить фильмы:', error);
            moviesListContainer.innerHTML = `<div class="col"><p class="text-danger">Не удалось загрузить фильмы. Пожалуйста, попробуйте позже.</p><p>${error.message}</p></div>`;
        } finally {
            loadingIndicator.style.display = 'none';
        }
    }

    function renderMovies(movies) {
        if (!movies || movies.length === 0) {
            moviesListContainer.innerHTML = '<div class="col"><p>Фильмы не найдены.</p></div>';
            return;
        }

        movies.forEach(movie => {
            const movieCard = `
                <div class="col">
                    <div class="card movie-card h-100 shadow-sm">
                        <a href="/movie/${movie.id}" class="text-decoration-none text-dark">
                            <div class="card-img-top-wrapper">
                                <img src="${movie.poster ? movie.poster : '/images/placeholder.png'}" class="card-img-top" alt="${movie.title || 'Без названия'}">
                            </div>
                            <div class="card-body">
                                <h5 class="card-title">${movie.title || 'Без названия'}</h5>
                                <div class="card-text">
                                    <small class="text-muted">Год: ${movie.releaseYear || 'N/A'}</small>
                                    <small class="text-muted">Рейтинг: ${movie.rating ? movie.rating.toFixed(1) : 'N/A'}</small>
                                    <small class="text-muted">Длительность: ${movie.duration ? movie.duration + ' мин' : 'N/A'}</small>
                                    ${movie.genres && movie.genres.length > 0 ?
                `<div class="genres mt-2">
                                            ${movie.genres.map(g => `<span class="badge bg-secondary">${g.name}</span>`).join(' ')}
                                        </div>` : ''
            }
                                </div>
                            </div>
                        </a>
                    </div>
                </div>
            `;
            moviesListContainer.insertAdjacentHTML('beforeend', movieCard);
        });
    }

    function renderPagination(totalPages, currentPage) {
        if (totalPages <= 1) {
            return; // Не отображать пагинацию, если всего одна страница или нет страниц
        }

        // Кнопка "Назад"
        const prevDisabled = currentPage === 0 ? 'disabled' : '';
        const prevItem = `
            <li class="page-item ${prevDisabled}">
                <a class="page-link" href="#" data-page="${currentPage - 1}" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>`;
        paginationContainer.insertAdjacentHTML('beforeend', prevItem);

        // Номера страниц (можно добавить логику для отображения ограниченного числа страниц, если их много)
        for (let i = 0; i < totalPages; i++) {
            const active = i === currentPage ? 'active' : '';
            const pageItem = `
                <li class="page-item ${active}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>`;
            paginationContainer.insertAdjacentHTML('beforeend', pageItem);
        }

        // Кнопка "Вперед"
        const nextDisabled = currentPage === totalPages - 1 ? 'disabled' : '';
        const nextItem = `
            <li class="page-item ${nextDisabled}">
                <a class="page-link" href="#" data-page="${currentPage + 1}" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>`;
        paginationContainer.insertAdjacentHTML('beforeend', nextItem);

        // Добавляем обработчики событий для кнопок пагинации
        document.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const page = parseInt(event.currentTarget.dataset.page);
                if (!isNaN(page) && page !== currentPage) {
                    fetchMovies(page);
                    window.scrollTo(0, 0); // Прокрутка вверх страницы
                }
            });
        });
    }

    // Инициализация
    updatePageTitle();
    fetchMovies(currentPage);
});
