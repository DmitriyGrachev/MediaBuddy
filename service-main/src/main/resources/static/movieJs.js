document.addEventListener("DOMContentLoaded", function () {
    // Получаем ссылки на оба контейнера
    // --- Ваш существующий код (начало) ---
    const movieContainer = document.getElementById("movieContainer");
    const movieHeaderSection = document.getElementById("movieHeaderSection");

    if (movieContainer && movieHeaderSection) {
        const pathParts = window.location.pathname.split('/');
        const movieIdFromUrl = pathParts[pathParts.length - 1]; // Переименовал для ясности
        if (!movieIdFromUrl || isNaN(movieIdFromUrl)) {
            movieContainer.innerHTML = '<div class="alert alert-danger">Некорректный ID фильма в URL</div>';
            movieHeaderSection.innerHTML = '';
            // Выход из функции, если ID некорректен
        } else {
            let currentPage = 0;
            let totalPages = 1;

            // --- Функции для API вызовов "Избранного" (вставьте сюда ваш код) ---
            async function checkIfFavorite(filmId) {
                const token = sessionStorage.getItem("token");
                try {
                    const response = await fetch(`/api/favorites/check?filmId=${filmId}`, {
                        method: "GET",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${token}`
                        }
                    });

                    if (!response.ok) {
                        console.error('Failed to check favorite status:', response.statusText);
                        return false;
                    }

                    const data = await response.json();
                    return data.isFavorite === 'true'; // ← исправлено

                } catch (error) {
                    console.error('Error checking favorite status:', error);
                    return false;
                }
            }


            async function addMovieToFavorites(filmId, type) {
                const token = sessionStorage.getItem("token");

                try {
                    const response = await fetch(`/api/favorites/add?filmId=${filmId}&type=${type}`, {
                        method: 'POST',
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${token}`
                        }
                    });
                    if (!response.ok) {
                        console.error('Failed to add to favorites:', response.statusText);
                        return false;
                    }
                    const data = await response.json();
                    return data.status === 'success';
                } catch (error) {
                    console.error('Error adding to favorites:', error);
                    return false;
                }
            }

            async function removeMovieFromFavorites(filmId) {
                const token = sessionStorage.getItem("token");
                try {
                    const response = await fetch(`/api/favorites/remove?filmId=${filmId}`, {
                        method: 'DELETE',
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${token}`
                        }
                    });
                    if (!response.ok) {
                        console.error('Failed to remove from favorites:', response.statusText);
                        return false;
                    }
                    const data = await response.json();
                    console.log(data)
                    return data.status === 'success';
                } catch (error) {
                    console.error('Error removing from favorites:', error);
                    return false;
                }
            }

            // --- Логика обновления UI для "Избранного" (вставьте сюда ваш код) ---
            let isFavorite = false; // Глобальная переменная для состояния "Избранного"
            const typeFilm = "movie"; // Или динамически, если нужно

            function updateHeartIcon(heartIconElement) { // Принимает элемент иконки как аргумент
                if (!heartIconElement) return;
                const favoriteText = document.getElementById("favoriteText");

                if (isFavorite) {
                    heartIconElement.classList.remove('far', 'fa-heart');
                    heartIconElement.classList.add('fas', 'fa-heart', 'text-danger');
                    if (favoriteText) favoriteText.textContent = "Убрать из избранного";
                } else {
                    heartIconElement.classList.remove('fas', 'fa-heart', 'text-danger');
                    heartIconElement.classList.add('far', 'fa-heart');
                    if (favoriteText) favoriteText.textContent = "В избранное";
                }
            }

            async function initializeFavoriteButton(currentMovieId) {
                const favoriteButton = document.getElementById('favoriteButton');
                if (!favoriteButton) {
                    console.error('Кнопка "favoriteButton" не найдена в DOM.');
                    return;
                }
                const heartIcon = favoriteButton.querySelector('i');
                if (!heartIcon) {
                    console.error('Иконка сердца не найдена внутри "favoriteButton".');
                    return;
                }

                // Начальная проверка статуса "Избранного"
                isFavorite = await checkIfFavorite(currentMovieId);
                updateHeartIcon(heartIcon);

                favoriteButton.addEventListener('click', async () => {
                    if (isFavorite) {
                        const success = await removeMovieFromFavorites(currentMovieId);
                        if (success) {
                            isFavorite = false;
                            updateHeartIcon(heartIcon);
                            console.log('Removed from favorites!');
                        } else {
                            console.error('Failed to remove from favorites.');
                        }
                    } else {
                        const success = await addMovieToFavorites(currentMovieId, typeFilm);
                        if (success) {
                            isFavorite = true;
                            updateHeartIcon(heartIcon);
                            console.log('Added to favorites!');
                        } else {
                            console.error('Failed to add to favorites.');
                        }
                    }
                });
            }

            async function fetchMovie() {
                try {
                    console.log(movieIdFromUrl + " movie id")
                    const response = await fetch(`/api/movie/${movieIdFromUrl}`); // Используем movieIdFromUrl
                    if (!response.ok) {
                        throw new Error(`HTTP error: status: ${response.status}`);
                    }
                    const movie = await response.json();
                    await fetchComments(movie.id, currentPage); // Используем movieIdFromUrl
                    await loadRecentComments();
                    renderMovie(movie); // Передаем объект movie

                    // !!! Инициализация кнопки "Избранное" ПОСЛЕ рендеринга фильма !!!
                    // Предполагается, что renderMovie добавляет кнопку favoriteButton в DOM
                    // или она уже есть в HTML статически.
                    await initializeFavoriteButton(movie.id); // Используем movie.id
                    bindReplyHandlers();
                } catch (error) {
                    console.error('Ошибка при получении данных:', error);
                    movieContainer.innerHTML = '<div class="alert alert-danger">Ошибка загрузки данных: ' + error.message + '</div>';
                    movieHeaderSection.innerHTML = '';
                }
            }

            async function fetchComments(movieId, page) {
                try {
                    const response = await fetch(`/api/comments?movieId=${movieId}&page=${page}&size=5&sortBy=time,desc`);
                    if (!response.ok) {
                        throw new Error(`HTTP error: status: ${response.status}`);
                    }
                    const data = await response.json();
                    window.comments = data.content;
                    totalPages = data.totalPages;
                    currentPage = page;
                    return data;
                } catch (error) {
                    console.error('Ошибка при получении комментариев:', error);
                    // Убедитесь, что находите commentError div, который теперь в правой колонке
                    const commentError = document.getElementById("commentError");
                    if (commentError) {
                        commentError.textContent = `Ошибка загрузки комментариев: ${error.message}`;
                        commentError.classList.remove("d-none");
                    }
                }
            }

            async function renderMovie(movie) {
                console.log('Movie data:', movie);
                if (!movie) {
                    // Обработка случая, когда фильм не найден
                    movieContainer.innerHTML = '<div class="alert alert-warning">Фильм не найден</div>';
                    movieHeaderSection.innerHTML = ''; // Очищаем заголовочную секцию
                    return;
                }

                // --- HTML for movie header (unchanged) ---
                const movieHeaderHtml = `
    <div class="row mb-4"> <div class="col-md-4"> <img src="${movie.poster || 'https://placehold.co/400x600/666/fff?text=No+Poster'}"
                 class="img-fluid rounded shadow" alt="${movie.title || 'Постер фильма'}"
                 title="${movie.title || 'Фильм'}"
                 onerror="this.onerror=null;this.src='https://placehold.co/400x600/666/fff?text=Poster+Error';">
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
                <h1 class="mb-0">${movie.title || 'Название фильма'}</h1>
                ${movie.rating ? `
                    <span class="badge bg-success fs-5">${movie.rating}</span>
                ` : ''}
                <button id="favoriteButton" class="btn btn-outline-danger">
                <i class="far fa-heart"></i> <span id="favoriteText">В избранное</span>
            </button>
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
                            ${renderGenres(movie)}
                        </div>
                        <div class="col-md-8">
                            <p>${movie.director || 'Не указан'}</p>
                            <p>${movie.releaseYear || 'Не указан'}</p>
                            <p>${movie.duration ? movie.duration + ' мин' : 'Не указана'}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    ${movie.trailerUrl ? `
         <div class="modal fade" id="trailerModal" tabindex="-1" aria-labelledby="trailerModalLabel" aria-hidden="true">
             <div class="modal-dialog modal-lg">
                 <div class="modal-content">
                     <div class="modal-header">
                         <h5 class="modal-title" id="trailerModalLabel">Трейлер "${movie.title || 'фильма'}"</h5>
                         <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                     </div>
                     <div class="modal-body">
                         <div class="ratio ratio-16x9">
                             <iframe src="${movie.trailerUrl}" title="Трейлер ${movie.title || 'фильма'}" allowfullscreen></iframe>
                         </div>
                     </div>
                 </div>
             </div>
         </div>
     ` : ''}
`;

                // --- HTML for main content (player, description, comments) ---
                const mainContentHtml = `
    <div>
        <h4>Описание</h4>
        <p class="lead">${movie.description || 'Описание отсутствует'}</p>
    </div>

    <div class="mt-4">
        <video
        id="my-video"
        class="video-js vjs-default-skin vjs-big-play-centered w-100"
        controls
        preload="auto"
        height="auto"
        data-setup='{"errorDisplay": true}'>
            <source src="http://localhost:8083/api/stream/${movie.source}" type="video/mp4">
            <p class="vjs-no-js">
                Для просмотра видео, пожалуйста, включите JavaScript и используйте браузер с поддержкой HTML5 видео
            </p>
        </video>
        <button id="resumeProgressBtn" class="btn btn-outline-primary mt-2" style="display: none;">
            <i class="bi bi-play-circle"></i> Продолжить с сохраненного места
        </button>
    </div>

    <div class="mt-5"> <h4 class="mb-3">Комментарии</h4>
        ${renderCommentForm(movie)}  <div id="commentsList" class="mt-4">
            ${renderComments()}          </div>
        ${renderPagination()}        </div>
`;

                movieHeaderSection.innerHTML = movieHeaderHtml;
                movieContainer.innerHTML = mainContentHtml;

                const videoElement = document.getElementById('my-video');
                const resumeBtn = document.getElementById('resumeProgressBtn');
                let lastSavedTime = 0;
                let saveInterval;
                const SAVE_INTERVAL_MS = 15000; // Save every 15 seconds

                // Helper function to format time (MM:SS)
                function formatTime(totalSeconds) {
                    if (isNaN(totalSeconds) || totalSeconds < 0) return "00:00";
                    const minutes = Math.floor(totalSeconds / 60);
                    const seconds = Math.floor(totalSeconds % 60);
                    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
                }

                // Function to fetch watch progress
                async function fetchProgress() {
                    const token = sessionStorage.getItem("token");
                    if (!token || !movie || !movie.id) {
                        console.log("Невозможно загрузить прогресс: отсутствует токен или ID фильма.");
                        return null;
                    }
                    try {
                        const response = await fetch(`/api/progress?filmId=${movie.id}`, {
                            headers: {"Authorization": `Bearer ${token}`}
                        });
                        if (response.ok) {
                            if (response.status === 204) { // No content
                                console.log("Прогресс не найден (204).");
                                return null;
                            }
                            const data = await response.json();
                            // Handle cases where backend returns { time: value } or just value
                            const time = data && data.time !== undefined ? data.time : (typeof data === 'number' ? data : null);
                            console.log("Загруженный прогресс:", time);
                            return time;
                        }
                        console.error("Ошибка при загрузке прогресса:", response.status, await response.text());
                        return null;
                    } catch (err) {
                        console.error("Исключение при загрузке прогресса:", err);
                        return null;
                    }
                }

                // Function to save watch progress
                async function saveProgress(currentTime,player) {
                    const token = sessionStorage.getItem("token");
                    if (!token || !movie || !movie.id) {
                        console.log("Невозможно сохранить прогресс: отсутствует токен или ID фильма.");
                        return;
                    }
                    // Avoid saving if currentTime is undefined or NaN
                    if (typeof currentTime !== 'number' || isNaN(currentTime)) {
                        console.warn("Попытка сохранить невалидное время:", currentTime);
                        return;
                    }

                    console.log(`Попытка сохранить прогресс: ${currentTime} для фильма ${movie.id}`);
                    try {
                        const response = await fetch(`/api/progress?filmId=${movie.id}&time=${currentTime}`, {
                            method: "POST",
                            headers: {
                                "Authorization": `Bearer ${token}`,
                                // Content-Type might not be strictly needed if params are in URL,
                                // but good practice if backend expects it or for future body additions.
                                // "Content-Type": "application/json"
                            }
                        });

                        if (response.ok) {
                            console.log(`Прогресс успешно сохранен: ${currentTime} сек`);
                            lastSavedTime = currentTime;
                            if (resumeBtn) {
                                if (currentTime > 0 && currentTime < player.duration() - 1) { // Show button if progress is meaningful
                                    resumeBtn.innerHTML = `<i class="bi bi-play-circle"></i> Продолжить с ${formatTime(currentTime)}`;
                                    resumeBtn.style.display = 'block';
                                } else {
                                    // Hide button if progress is 0 or video is fully watched
                                    // resumeBtn.style.display = 'none';
                                    // Or, if fully watched, you might want a "Пересмотреть" button or similar
                                    if (currentTime >= player.duration() - 1) {
                                        resumeBtn.innerHTML = `<i class="bi bi-check-circle"></i> Фильм просмотрен`;
                                        resumeBtn.style.display = 'block'; // Keep it visible to show status
                                    } else {
                                        resumeBtn.style.display = 'none';
                                    }
                                }
                            }
                        } else {
                            console.error("Ошибка при сохранении прогресса:", response.status, await response.text());
                        }
                    } catch (err) {
                        console.error("Исключение при сохранении прогресса:", err);
                    }
                }


                if (videoElement) {
                    const player = videojs(videoElement);

                    player.on('error', () => {
                        console.error('Ошибка видео:', player.error());
                        const error = player.error();
                        if (error && error.code === 4) { // MEDIA_ERR_SRC_NOT_SUPPORTED or network error
                            // Potentially display a user-friendly message here
                            console.error("Источник видео не может быть загружен или не поддерживается.");
                        }
                    });

                    // --- Watch History (existing logic) ---
                    let watchEventSent = false;
                    player.on('play', async () => {
                        if (watchEventSent) return;
                        watchEventSent = true; // Set immediately to prevent multiple sends

                        // Save initial watch event
                        try {
                            const token = sessionStorage.getItem("token");
                            if (!token || !movie || !movie.id) return;

                            await fetch(`/api/watch/${movie.id}`, {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/json",
                                    "Authorization": `Bearer ${token}`
                                },
                                body: JSON.stringify({filmId: movie.id}) // Ensure body is correct
                            });
                            console.log("История просмотра сохранена (первое воспроизведение)");
                        } catch (err) {
                            console.error("Ошибка при сохранении истории просмотра:", err);
                        }
                    });


                    // --- Resume Playback Logic ---
                    player.on('loadedmetadata', async () => { // 'loadedmetadata' is often better than 'ready' for duration/currentTime
                        console.log("Метаданные видео загружены.");
                        const fetchedTime = await fetchProgress();
                        if (fetchedTime !== null && fetchedTime > 0 && fetchedTime < player.duration() - 1) { // Check against duration
                            console.log(`Установка времени на сохраненное: ${fetchedTime}`);
                            player.currentTime(fetchedTime);
                            lastSavedTime = fetchedTime;
                            if (resumeBtn) {
                                resumeBtn.innerHTML = `<i class="bi bi-play-circle"></i> Продолжить с ${formatTime(lastSavedTime)}`;
                                resumeBtn.style.display = 'block';
                            }
                            // player.play(); // Optional: uncomment to auto-play from saved spot
                        } else if (fetchedTime !== null && fetchedTime >= player.duration() - 1) {
                            console.log("Фильм был просмотрен полностью.");
                            if (resumeBtn) {
                                resumeBtn.innerHTML = `<i class="bi bi-check-circle"></i> Фильм просмотрен`;
                                resumeBtn.style.display = 'block';
                            }
                        } else {
                            console.log("Нет сохраненного прогресса для возобновления или фильм не начат.");
                            if (resumeBtn) resumeBtn.style.display = 'none';
                        }
                    });

                    if (resumeBtn) {
                        resumeBtn.addEventListener('click', () => {
                            // Fetch the latest progress again in case it changed via another tab/device,
                            // or use lastSavedTime if confident it's up-to-date.
                            // For simplicity, using lastSavedTime which is updated on load and save.
                            if (lastSavedTime > 0 && lastSavedTime < player.duration() - 1) {
                                player.currentTime(lastSavedTime);
                                player.play();
                            } else {
                                // If no meaningful lastSavedTime, try fetching fresh.
                                fetchProgress().then(time => {
                                    if (time !== null && time > 0 && time < player.duration() - 1) {
                                        player.currentTime(time);
                                        player.play();
                                        lastSavedTime = time; // Update local cache
                                        resumeBtn.innerHTML = `<i class="bi bi-play-circle"></i> Продолжить с ${formatTime(lastSavedTime)}`;
                                        resumeBtn.style.display = 'block';
                                    } else if (time !== null && time >= player.duration() - 1) {
                                        resumeBtn.innerHTML = `<i class="bi bi-check-circle"></i> Фильм просмотрен`;
                                        resumeBtn.style.display = 'block';
                                    }
                                });
                            }
                        });
                    }

                    // Save progress periodically and on specific events
                    player.on('play', () => {
                        console.log("Видео начало воспроизведение. Запуск интервала сохранения.");
                        clearInterval(saveInterval); // Clear existing interval if any
                        saveInterval = setInterval(() => {
                            if (!player.paused()) {
                                const currentTime = player.currentTime();
                                // Only save if time is significant and has changed meaningfully
                                if (currentTime > 0 && Math.abs(currentTime - lastSavedTime) > 5) { // Check for at least 5s difference
                                    saveProgress(currentTime,player);
                                }
                            }
                        }, SAVE_INTERVAL_MS);
                    });

                    player.on('pause', () => {
                        console.log("Видео на паузе. Очистка интервала и сохранение текущей позиции.");
                        clearInterval(saveInterval);
                        const currentTime = player.currentTime();
                        // Don't save if position is 0 unless it's explicitly the start
                        // or if video has ended (duration might be equal to currentTime)
                        if (currentTime > 0) {
                            saveProgress(currentTime,player);
                        }
                    });

                    player.on('ended', () => {
                        console.log("Видео завершено. Очистка интервала и сохранение финальной позиции.");
                        clearInterval(saveInterval);
                        const duration = player.duration();
                        if (duration) {
                            saveProgress(duration,player); // Save duration to mark as fully watched
                        }
                    });

                    // Clean up interval when player is disposed (e.g., user navigates away)
                    player.on('dispose', () => {
                        console.log("Плеер уничтожен. Очистка интервала сохранения.");
                        clearInterval(saveInterval);
                        // Optionally, a final save attempt here, but 'pause' or 'ended' should cover most cases.
                        // const currentTime = player.currentTime(); // player object might be unusable here
                        // if (lastSavedTime > 0) saveProgress(lastSavedTime); // Save the last known good time
                    });

                } else {
                    console.error("Элемент video не найден!");
                }


                // Привязка обработчика отправки комментария
                const commentFormElement = document.getElementById('commentForm');
                if (commentFormElement) {
                    commentFormElement.addEventListener('submit', async (event) => {
                        event.preventDefault();
                        await submitComment(movie);
                    });
                }

                // Привязка обработчиков пагинации
                const prevButton = movieContainer.querySelector('#prevPage');
                const nextButton = movieContainer.querySelector('#nextPage');

                if (prevButton) {
                    prevButton.addEventListener('click', async () => {
                        if (currentPage > 0) {
                            await fetchComments(movieId, currentPage - 1);
                            renderMovie(movie);
                        }
                    });
                }
                if (nextButton) {
                    nextButton.addEventListener('click', async () => {
                        if (currentPage < totalPages - 1) {
                            await fetchComments(movieId, currentPage + 1);
                            renderMovie(movie);
                        }
                    });
                }

                bindReactionHandlers(movie);
            }

            function bindReactionHandlers(movie) {
                // Ищем кнопки лайков и дизлайков ВНУТРИ movieContainer
                document.querySelectorAll('#movieContainer .like-btn').forEach(button => {
                    button.addEventListener('click', async () => {
                        const commentId = button.dataset.commentId;
                        await submitReaction(commentId, 'LIKE');
                        // После реакции обновляем комментарии и перерисовываем секцию
                        await fetchComments(movieId, currentPage);
                        renderMovie(movie);
                    });
                });
                document.querySelectorAll('#movieContainer .dislike-btn').forEach(button => {
                    button.addEventListener('click', async () => {
                        const commentId = button.dataset.commentId;
                        await submitReaction(commentId, 'DISLIKE');
                        // После реакции обновляем комментарии и перерисовываем секцию
                        await fetchComments(movieId, currentPage);
                        renderMovie(movie);
                    });
                });
            }

            function renderCommentForm(movie) {
                // ... (функция renderCommentForm остается без изменений)
                const userId = sessionStorage.getItem('userId') || 1; // Получаем userId или используем 1 как fallback
                if (!userId) {
                    // Возвращаем сообщение для неавторизованных пользователей
                    return '<p class="text-muted">Войдите, чтобы оставить комментарий.</p>';
                }

                return `
                 <div class="card mb-4">
                     <div class="card-body">
                         <form id="commentForm">
                             <div class="mb-3">
                                 <label for="commentText" class="form-label">Ваш комментарий</label>
                                 <textarea class="form-control" id="commentText" name="text" rows="4" required></textarea>
                             </div>
                             <input type="hidden" name="filmId" value="${movie.id}">
                             <input type="hidden" name="userId" value="${userId}">
                             <button type="submit" class="btn btn-primary">Отправить</button>
                         </form>
                     </div>
                 </div>
             `;
            }

            // Функция для рендеринга комментариев
            function renderComments(comments = window.comments || [], depth = 0) {
                if (!comments || !Array.isArray(comments) || comments.length === 0) {
                    return depth === 0 ? '<p class="text-muted">Комментариев пока нет.</p>' : '';
                }

                return comments
                    .map(comment => {
                        const indent = depth * 20;
                        return `
                <div class="card mb-4" style="margin-left: ${indent}px;" data-comment-id="${comment.id}">
                    <div class="card-body">
                        <h6 class="card-title">${comment.username || comment.user || 'Аноним'}</h6>
                        <p class="card-text">${comment.text || 'Без текста'}</p>
                        <p class="card-text"><small class="text-muted">${comment.time || 'Без времени'}</small></p>
                        <div class="mt-2">
                            <button class="btn btn-sm btn-outline-success like-btn" data-comment-id="${comment.id}">
                                👍 ${comment.likes || 0}
                            </button>
                            <button class="btn btn-sm btn-outline-danger dislike-btn" data-comment-id="${comment.id}">
                                👎 ${comment.dislikes || 0}
                            </button>
                            <button class="btn btn-sm btn-outline-primary reply-btn" data-comment-id="${comment.id}">
                                Ответить
                            </button>
                        </div>
                        ${renderComments(comment.replies, depth + 1)}
                    </div>
                </div>
            `;
                    })
                    .join('');
            }

// Привязка обработчиков для кнопок "Reply"
            function bindReplyHandlers() {
                document.querySelectorAll('.reply-btn').forEach(button => {
                    button.addEventListener('click', () => {
                        const commentId = button.dataset.commentId;
                        showReplyForm(commentId);
                    });
                });
            }

// Показ формы для ответа
            function showReplyForm(parentId) {
                const existingForm = document.querySelector('.reply-form');
                if (existingForm) existingForm.remove();

                const formHtml = `
        <div class="reply-form card mb-4" style="margin-left: ${20}px;">
            <div class="card-body">
                <form id="replyForm">
                    <div class="mb-3">
                        <label for="replyText" class="form-label">Ваш ответ</label>
                        <textarea class="form-control" id="replyText" name="text" rows="3" required></textarea>
                    </div>
                    <input type="hidden" name="parentId" value="${parentId}">
                    <button type="submit" class="btn btn-primary">Отправить</button>
                    <button type="button" class="btn btn-secondary cancel-reply">Отмена</button>
                </form>
            </div>
        </div>
    `;

                const commentElement = document.querySelector(`[data-comment-id="${parentId}"]`);
                commentElement.insertAdjacentHTML('afterend', formHtml);

                const replyForm = document.getElementById('replyForm');
                replyForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    await submitReply();
                });

                document.querySelector('.cancel-reply').addEventListener('click', () => {
                    document.querySelector('.reply-form').remove();
                });
            }

// Отправка ответа на сервер
            async function submitReply() {
                const form = document.getElementById('replyForm');
                const formData = new FormData(form);
                const replyText = formData.get('text');
                const parentId = formData.get('parentId');

                const token = sessionStorage.getItem('token');
                if (!token) {
                    showNotification('Только авторизованные пользователи могут отвечать.', 'warning');
                    return;
                }

                if (!replyText || replyText.trim() === '') {
                    showNotification('Ответ не может быть пустым', 'danger');
                    return;
                }

                const replyData = { text: replyText.trim() };

                try {
                    const response = await fetch(`/api/comments/reply?parentId=${parentId}`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${token}`
                        },
                        body: JSON.stringify(replyData)
                    });

                    if (response.status === 401) {
                        sessionStorage.removeItem('token');
                        showNotification('Сессия истекла. Войдите снова.', 'warning');
                        return;
                    } else if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(`Ошибка: ${response.status}. ${errorText}`);
                    }

                    showNotification('Ответ добавлен успешно!', 'success');
                    document.querySelector('.reply-form').remove();
                    //await fetchComments(); // Предполагается, что эта функция обновляет комментарии
                } catch (error) {
                    console.error('Ошибка:', error);
                    showNotification('Ошибка при отправке ответа', 'danger');
                }
            }
            // Уведомления
            function showNotification(message, type) {
                const notification = document.createElement('div');
                notification.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x m-3`;
                notification.style.zIndex = '1050';
                notification.textContent = message;
                document.body.appendChild(notification);
                setTimeout(() => notification.remove(), 5000);
            }
            function renderGenres(movie) {
                // ... (функция renderGenres остается без изменений)
                console.log('Genres data:', movie.genres);
                if (!movie.genres || !Array.isArray(movie.genres) || movie.genres.length === 0) {
                    return '<p>Жанры не указаны</p>';
                }
                return '<p><strong>Жанры:</strong></p><p>' +
                    movie.genres.map(genre => genre.name).join(', ') +
                    '</p>';
            }

            function renderPagination() {
                // ... (функция renderPagination остается без изменений)
                return `
                 <nav aria-label="Comment pagination">
                     <ul class="pagination justify-content-center mt-3">
                         <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                             <button class="page-link" id="prevPage">Предыдущая</button>
                         </li>
                         <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                             <button class="page-link" id="nextPage">Следующая</button>
                         </li>
                     </ul>
                 </nav>
             `;
            }

            async function submitComment(movie) {
                const form = document.getElementById("commentForm");
                const formData = new FormData(form);
                const commentText = formData.get("text");
                const commentError = document.getElementById("commentError");
                const commentResult = document.getElementById("commentResult");
                const loading = document.getElementById("loading"); // For the main comment form, not the sidebar

                // ... (token check, validation logic as before) ...
                const token = sessionStorage.getItem("token");
                if (!token) {
                    // ... (show "login to comment" notification) ...
                    const notification = document.createElement('div');
                    notification.className = 'alert alert-warning position-fixed top-0 start-50 translate-middle-x m-3';
                    notification.style.zIndex = "1050";
                    notification.innerHTML = `
            <div>Только авторизованные пользователи могут публиковать комментарии.</div>
            <div class="mt-2">
                <a href="/authfront/login" class="btn btn-primary btn-sm">Войти</a>
            </div>
        `;
                    document.body.appendChild(notification);
                    setTimeout(() => {
                        notification.remove();
                    }, 5000);
                    return;
                }

                if (!commentText || commentText.trim() === "") {
                    if (commentError) {
                        commentError.textContent = "Комментарий не может быть пустым";
                        commentError.className = "alert alert-danger";
                        commentError.classList.remove("d-none");
                    }
                    return;
                }

                const commentData = {
                    filmId: Number(formData.get("filmId")),
                    text: commentText
                };

                const button = form.querySelector("button[type='submit']"); // More specific selector

                if (loading) loading.classList.remove("d-none");
                if (button) button.disabled = true;
                if (commentError) commentError.classList.add("d-none");

                try {
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5s timeout

                    const response = await fetch("/api/comments", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${token}`
                        },
                        body: JSON.stringify(commentData),
                        signal: controller.signal
                    });

                    clearTimeout(timeoutId);

                    if (response.status === 401) {
                        sessionStorage.removeItem("token");
                        if (commentError) {
                            commentError.textContent = "Срок действия сессии истек. Пожалуйста, войдите снова.";
                            commentError.className = "alert alert-warning";
                            commentError.classList.remove("d-none");
                        }
                        return;
                    } else if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(`HTTP error: ${response.status}. ${errorText}`);
                    }

                    // const data = await response.json(); // Assuming your POST /api/comments returns the new comment or a success message
                    if (commentResult) {
                        commentResult.innerHTML = `<div class="alert alert-success">Комментарий добавлен успешно!</div>`;
                    }
                    form.reset();
                    currentPage = 0; // Reset to first page for main comments
                    await fetchComments(movie.id, currentPage); // Reload comments for the current movie
                    renderMovie(movie); // Re-render the main movie section (including its comments)

                    // ***** KEY CHANGE: Refresh the recent comments sidebar *****
                    if (typeof loadRecentComments === 'function') {
                        await loadRecentComments();
                    } else {
                        console.warn('loadRecentComments function is not available to refresh the sidebar.');
                    }
                    // ***** END KEY CHANGE *****

                    setTimeout(() => {
                        if (commentResult) commentResult.innerHTML = "";
                    }, 3000);

                } catch (error) {
                    console.error("Ошибка при отправке комментария:", error);
                    if (commentError) {
                        commentError.textContent = error.name === "AbortError"
                            ? "Превышен лимит ожидания ответа от сервера"
                            : error.message.includes("Failed to fetch")
                                ? "Ошибка сети. Пожалуйста, проверьте подключение к интернету"
                                : `Ошибка: ${error.message}`;
                        commentError.className = "alert alert-danger mt-2";
                        commentError.classList.remove("d-none");
                    }
                } finally {
                    if (loading) loading.classList.add("d-none");
                    if (button) button.disabled = false;
                }
            }

            // Просто добавьте эти две переменные в начало функции submitReaction
            async function submitReaction(commentId, type) {
                // Получаем movieId из URL (как в начале вашего кода)
                const pathParts = window.location.pathname.split('/');
                const movieId = pathParts[pathParts.length - 1];

                // Получаем movie из скрытого поля формы
                const filmIdInput = document.querySelector('input[name="filmId"]');
                const currentMovieId = filmIdInput ? filmIdInput.value : movieId;

                const loading = document.getElementById("loading");
                const commentError = document.getElementById("commentError");

                if (loading) loading.classList.remove("d-none");
                if (commentError) commentError.classList.add("d-none");

                try {
                    const token = sessionStorage.getItem("token");

                    if (!token) {
                        const notification = document.createElement('div');
                        notification.className = 'alert alert-warning position-fixed top-0 start-50 translate-middle-x m-3';
                        notification.style.zIndex = "1050";
                        notification.innerHTML = `
                <div>Только авторизованные пользователи могут ставить реакции.</div>
                <div class="mt-2">
                    <a href="/authfront/login" class="btn btn-primary btn-sm">Войти</a>
                </div>
            `;
                        document.body.appendChild(notification);
                        setTimeout(() => {
                            notification.remove();
                        }, 5000);
                        return;
                    }

                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), 5000);

                    const response = await fetch(`/api/comments/${commentId}/reactions`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${token}`
                        },
                        body: JSON.stringify({type}),
                        signal: controller.signal
                    });

                    clearTimeout(timeoutId);

                    if (response.status === 401) {
                        sessionStorage.removeItem("token");
                        if (commentError) {
                            commentError.textContent = "Срок действия сессии истек. Пожалуйста, войдите снова.";
                            commentError.className = "alert alert-warning";
                            commentError.classList.remove("d-none");
                        }
                        return;
                    } else if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(`HTTP error: ${response.status}. ${errorText}`);
                    }

                    // Реакция успешно добавлена
                    console.log("Реакция успешно добавлена");

                } catch (error) {
                    console.error("Ошибка при отправке реакции:", error);
                    if (commentError) {
                        commentError.textContent = error.name === "AbortError"
                            ? "Превышен лимит ожидания ответа от сервера"
                            : error.message.includes("Failed to fetch")
                                ? "Ошибка сети. Пожалуйста, проверьте подключение к интернету"
                                : `Ошибка: ${error.message}`;
                        commentError.className = "alert alert-danger mt-2";
                        commentError.classList.remove("d-none");
                    }
                } finally {
                    if (loading) loading.classList.add("d-none");

                    // Просто перезагружаем страницу после реакции - самый простой способ
                    // Или используем существующие переменные из замыкания
                    //TODO
                    //window.location.reload();
                }
            }

            async function loadRecentComments() {
                const recentCommentsListDiv = document.getElementById('recentCommentsList');
                const recentCommentsOuterContainer = document.getElementById('recentCommentsOuterContainer');

                if (!recentCommentsListDiv || !recentCommentsOuterContainer) {
                    // console.warn('Recent comments container elements not found.');
                    return;
                }
                // Check visibility
                if (recentCommentsOuterContainer.offsetParent === null) {
                    // console.log('Recent comments container is not visible, skipping load.');
                    return;
                }

                // Show spinner before fetching (if it was cleared by a previous error/no comments message)
                recentCommentsListDiv.innerHTML = `
        <div class="d-flex justify-content-center">
            <div class="spinner-border spinner-border-sm text-primary" role="status">
                <span class="visually-hidden">Загрузка...</span>
            </div>
        </div>`;

                try {
                    const response = await fetch('/api/comments/recently', {cache: 'no-store'}); // Add cache: 'no-store' for freshness
                    if (!response.ok) {
                        recentCommentsListDiv.innerHTML = '<div class="alert alert-warning small p-2 text-center">Не удалось загрузить комментарии.</div>';
                        console.error(`Error fetching recent comments: ${response.status}, ${await response.text()}`);
                        return;
                    }
                    const comments = await response.json();

                    if (comments && comments.length > 0) {
                        renderRecentCommentsList(comments, recentCommentsListDiv);
                    } else {
                        recentCommentsListDiv.innerHTML = '<p class="text-muted small text-center">Пока нет недавних комментариев.</p>';
                    }
                } catch (error) {
                    console.error('Failed to load recent comments:', error);
                    recentCommentsListDiv.innerHTML = '<div class="alert alert-danger small p-2 text-center">Ошибка загрузки комментариев.</div>';
                }
            }

            function renderRecentCommentsList(comments, containerElement) {
                containerElement.innerHTML = ''; // Clear loading spinner or previous content

                const ul = document.createElement('ul');
                ul.className = 'list-unstyled mb-0';

                comments.forEach(comment => {
                    const li = document.createElement('li');
                    li.className = 'recent-comment-item';
                    const filmPageLink = `/api/movie/${comment.filmId}`; // Or your correct frontend movie URL pattern

                    li.innerHTML = `
            <div class="comment-author">
                <strong>${comment.username || 'Аноним'}</strong>
                <a href="${filmPageLink}" title="Перейти к фильму, где оставлен комментарий">про фильм</a>
            </div>
            <p class="comment-text">${comment.text || 'Без текста'}</p>
            ${comment.time ? `<small class="text-muted d-block mt-1">${new Date(comment.time).toLocaleString()}</small>` : ''} 
        `; // Optionally display time
                    ul.appendChild(li);
                });
                containerElement.appendChild(ul);
            }

            fetchMovie();
        }
    }
});
