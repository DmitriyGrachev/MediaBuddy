document.addEventListener("DOMContentLoaded", function () {
    // Логика для списка фильмов (moviesPage.html)
    const moviesContainer = document.getElementById("moviesContainer");
    if (moviesContainer) {
        function fetchMovies() {
            fetch("/api/movies")
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error: status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(movies => {
                    renderMovies(movies);
                })
                .catch(error => {
                    console.error('Ошибка при получении данных:', error);
                    moviesContainer.innerHTML = '<div class="alert alert-danger">Ошибка загрузки данных.</div>';
                });
        }

        function renderMovies(movies) {
            moviesContainer.innerHTML = '';
            movies.forEach(movie => {
                const movieCard = `
                    <div class="col-sm-6 col-md-4 mb-3">
                        <a href="/movie/${movie.id}" class="card-link">
                            <div class="card h-100">
                                <img src="${movie.poster || 'https://via.placeholder.com/300x450'}" class="card-img-top" alt="${movie.title}">
                                <div class="card-body">
                                    <h5 class="card-title">${movie.title} <span class="badge bg-success">${movie.rating}</span></h5>
                                    <p class="card-text"><strong>Год:</strong> ${movie.releaseYear}</p>
                                    <p class="card-text"><strong>Режиссёр:</strong> ${movie.director}</p>
                                    <p class="card-text"><strong>Продолжительность:</strong> ${movie.duration} мин</p>
                                    <p class="card-text">${movie.description}</p>
                                    <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#trailerModal${movie.id}">Трейлер</button>
                                </div>
                            </div>
                        </a>
                    </div>
                    <div class="modal fade" id="trailerModal${movie.id}" tabindex="-1" aria-labelledby="trailerModalLabel${movie.id}" aria-hidden="true">
                        <div class="modal-dialog modal-lg">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title" id="trailerModalLabel${movie.id}">${movie.title}</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="ratio ratio-16x9">
                                        <iframe src="${movie.trailerUrl || 'https://www.youtube.com/embed/dQw4w9WgXcQ'}" title="Трейлер ${movie.title}" allowfullscreen></iframe>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
                moviesContainer.insertAdjacentHTML('beforeend', movieCard);
            });
        }

        fetchMovies();
    }

    // Логика для страницы фильма (moviePage.html)
    const movieContainer = document.getElementById("movieContainer");
    if (movieContainer) {
        const pathParts = window.location.pathname.split('/');
        const movieId = pathParts[pathParts.length - 1];

        function fetchMovie() {
            fetch(`/api/movie/${movieId}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error: status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(movie => {
                    renderMovie(movie);
                })
                .catch(error => {
                    console.error('Ошибка при получении данных:', error);
                    movieContainer.innerHTML = '<div class="alert alert-danger">Ошибка загрузки данных: ' + error.message + '</div>';
                });
        }

        function renderMovie(movie) {
            if (!movie) {
                movieContainer.innerHTML = '<div class="alert alert-warning">Фильм не найден</div>';
                return;
            }

            const movieContent = `
                <div class="row mt-4">
                    <div class="col-md-4">
                        <img src="${movie.poster || 'https://via.placeholder.com/300x450?text=' + encodeURIComponent(movie.title)}" 
                             class="img-fluid rounded shadow" alt="${movie.title}">
                        ${movie.trailerUrl ? `
                            <div class="mt-3">
                                <button class="btn btn-primary w-100" data-bs-toggle="modal" data-bs-target="#trailerModal">
                                    <i class="bi bi-play-fill"></i> Смотреть трейлер
                                </button>
                            </div>
                        ` : ''}
                    </div>
                    <div class="col-md-8">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h1 class="mb-0">${movie.title}</h1>
                            ${movie.rating ? `
                                <span class="badge bg-success fs-5">${movie.rating}</span>
                            ` : ''}
                        </div>
                        <div class="mb-4">
                            <span class="badge bg-secondary me-2">${movie.releaseYear || 'Год не указан'}</span>
                            ${movie.duration ? `<span class="badge bg-secondary me-2">${movie.duration} мин</span>` : ''}
                        </div>
                        <div class="card mb-4">
                            <div class="card-body">
                                <h5 class="card-title">О фильме</h5>
                                <div class="row">
                                    <div class="col-md-4">
                                        <p><strong>Режиссёр:</strong></p>
                                        <p><strong>Год выхода:</strong></p>
                                        <p><strong>Продолжительность:</strong></p>
                                    </div>
                                    <div class="col-md-8">
                                        <p>${movie.director || 'Не указан'}</p>
                                        <p>${movie.releaseYear || 'Не указан'}</p>
                                        <p>${movie.duration ? movie.duration + ' мин' : 'Не указана'}</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div>
                            <h4>Описание</h4>
                            <p class="lead">${movie.description || 'Описание отсутствует'}</p>
                        </div>
                    </div>
                </div>
                ${movie.trailerUrl ? `
                    <div class="modal fade" id="trailerModal" tabindex="-1" aria-labelledby="trailerModalLabel" aria-hidden="true">
                        <div class="modal-dialog modal-lg">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title" id="trailerModalLabel">Трейлер "${movie.title}"</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="ratio ratio-16x9">
                                        <iframe src="${movie.trailerUrl}" title="Трейлер ${movie.title}" allowfullscreen></iframe>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                ` : ''}
            `;
            movieContainer.innerHTML = movieContent;
            document.title = `${movie.title} (${movie.releaseYear || 'Н/Д'}) - Фильмы`;
        }

        fetchMovie();
    }
});