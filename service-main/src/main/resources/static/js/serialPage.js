document.addEventListener("DOMContentLoaded", function () {
    // --- Глобальные переменные и константы ---
    const serialHeaderSection = document.getElementById("serialHeaderSection");
    const serialContainer = document.getElementById("serialContainer");
    const recentCommentsList = document.getElementById("recentCommentsList");
    const pathParts = window.location.pathname.split('/');
    const serialId = pathParts[pathParts.length - 1];
    const token = sessionStorage.getItem('token');

    let serialDataCache = null;
    let player = null;
    let commentsCurrentPage = 0;


    // --- Проверка на старте ---
    if (!serialContainer || !serialHeaderSection || !serialId || isNaN(serialId)) {
        console.error("Критическая ошибка: не найдены контейнеры для рендеринга или некорректный ID сериала.");
        if (serialContainer) serialContainer.innerHTML = '<div class="alert alert-danger">Некорректный ID сериала в URL.</div>';
        return;
    }
    if (!token) {
        console.warn("Пользователь не авторизован. Часть функций будет недоступна.");
    }

    // #################################################################
    // ## ОСНОВНАЯ ЛОГИКА ЗАГРУЗКИ И РЕНДЕРИНГА
    // #################################################################

    async function initializePage() {
        try {
            const response = await fetch(`/api/serial/${serialId}`);
            if (!response.ok) throw new Error(`HTTP ошибка: ${response.status}`);

            serialDataCache = await response.json();
            document.title = `${serialDataCache.title || 'Сериал'} - Просмотр`;

            renderSerialHeader(serialDataCache);
            renderSerialPlayerAndContent();

            initializePlayerAndEpisodes(serialDataCache);
            initializeFavorites(serialId);
            initializeComments(serialId);
            loadRecentComments();

        } catch (error) {
            console.error('Ошибка при получении данных о сериале:', error);
            serialContainer.innerHTML = `<div class="alert alert-danger">Ошибка загрузки данных: ${error.message}</div>`;
        }
    }

    function renderSerialHeader(serial) {
        const genresHtml = serial.genres.map(g => `<span class="badge bg-info me-1">${g.name}</span>`).join(' ');
        serialHeaderSection.innerHTML = `
            <div class="row">
                <div class="col-md-3">
                <img 
  src="${serial.poster || 'https://placehold.co/400x600?text=No+Poster'}"
  onerror="this.onerror=null;this.src='https://placehold.co/400x600/666/fff?text=Poster+Error';"
  class="img-fluid rounded shadow"
  alt="Постер ${serial.title}"
  title="${serial.title}"
>
                </div>
                <div class="col-md-9">
                    <div class="d-flex justify-content-between align-items-start">
                        <h1 class="display-5">${serial.title}</h1>
                        <button id="favoriteButton" class="btn btn-outline-danger ms-3 flex-shrink-0" title="Добавить в избранное">
                            <i class="bi bi-heart"></i>
                        </button>
                    </div>
                    <p class="lead">${serial.description || 'Описание отсутствует.'}</p>
                    <hr>
                    <p><strong>Год:</strong> ${serial.releaseYear || 'н/д'} | <strong>Режиссёр:</strong> ${serial.director || 'н/д'}</p>
                    <p><strong>Жанры:</strong> ${genresHtml}</p>
                    <p><strong>Всего сезонов:</strong> ${serial.numberOfSeasons || 'н/д'}</p>
                </div>
            </div>`;
    }

    function renderSerialPlayerAndContent() {
        serialContainer.innerHTML = `
        <div class="player-section mb-4">
            <video id="serial-video-player" class="video-js vjs-default-skin vjs-big-play-centered w-100" controls preload="auto" data-setup='{}'>
                <p class="vjs-no-js">Для просмотра видео включите JavaScript.</p>
            </video>
            
            <div id="progressPrompt" class="progress-prompt-container"></div>

             <div id="currentEpisodeInfo" class="mt-2 text-white-50">
                <strong>Сейчас играет:</strong> <span id="episodeTitlePlaceholder">Выберите эпизод</span>
            </div>
        </div>
        <div class="row">
            <div class="col-md-4">
                <h5>Сезон</h5>
                <select id="seasonSelector" class="form-select"></select>
            </div>
            <div class="col-md-8">
                <h5>Эпизоды</h5>
                <div id="episodeListContainer" style="max-height: 250px; overflow-y: auto;">
                    <ul id="episodeList" class="list-group episode-list-group"></ul>
                </div>
            </div>
        </div>
        <hr class="my-4">
        <div id="commentsModule"></div>`;
    }

    // #################################################################
// ## МОДУЛЬ: ПЛЕЕР, ЭПИЗОДЫ И ПРОГРЕСС ПРОСМОТРА (ИСПРАВЛЕННАЯ ВЕРСИЯ)
// #################################################################

// Переименовали переменную для большей ясности
    let saveProgressInterval = null;

    function initializePlayerAndEpisodes(serialData) {
        player = videojs('serial-video-player');
        const seasonSelector = document.getElementById('seasonSelector');
        const episodeListEl = document.getElementById('episodeList');

        seasonSelector.innerHTML = serialData.seasonList.map(season =>
            `<option value="${season.seasonNumber}">${season.title || 'Сезон ' + season.seasonNumber}</option>`
        ).join('');

        const displayEpisodes = (seasonNumber) => {
            const season = serialData.seasonList.find(s => s.seasonNumber == seasonNumber);
            episodeListEl.innerHTML = season.episodeList.map(ep =>
                `<li class="list-group-item list-group-item-action" 
                 data-episode-id="${ep.id}" 
                 data-season-number="${season.seasonNumber}" 
                 data-source="${ep.source}" 
                 data-title="${ep.title}">
                Эпизод ${ep.episodeNumber}: ${ep.title}
            </li>`
            ).join('');
        };

        displayEpisodes(seasonSelector.value);
        seasonSelector.addEventListener('change', () => displayEpisodes(seasonSelector.value));
        episodeListEl.addEventListener('click', handleEpisodeClick);

        player.on('play', handlePlayEvent);
        player.on('pause', handlePauseOrEndEvent);
        player.on('ended', handlePauseOrEndEvent);

        loadInitialProgress();
    }

// --- НОВАЯ ЛОГИКА ЗАГРУЗКИ И ОТОБРАЖЕНИЯ ПРОГРЕССА ---

    async function loadInitialProgress() {
        if (!token) return;

        const progressData = await fetchSerialProgress(serialId);
        if (progressData && progressData.episodeId) {
            console.log("Найден прогресс, показываем уведомление:", progressData);
            // Вместо авто-запуска, показываем пользователю выбор
            showProgressPrompt(progressData);
        } else {
            console.log("Сохраненный прогресс не найден.");
        }
    }

    /**
     * Показывает уведомление с предложением продолжить просмотр.
     * @param {object} progressData - Данные о прогрессе { progress, episodeId, seasonId }
     */
    function showProgressPrompt(progressData) {
        const promptContainer = document.getElementById('progressPrompt');

        // Ищем информацию об эпизоде
        let episodeTitle = "Последний просмотренный эпизод";
        try {
            const season = serialDataCache.seasonList.find(s => s.seasonNumber == progressData.seasonId);
            const episode = season.episodeList.find(ep => ep.id == progressData.episodeId);
            episodeTitle = `Эпизод ${episode.episodeNumber}: ${episode.title}`;
        } catch (e) { /* оставим title по умолчанию */ }

        const minutes = Math.floor(progressData.progress / 60);
        const seconds = Math.floor(progressData.progress % 60).toString().padStart(2, '0');
        const savedTime = `${minutes}:${seconds}`;

        promptContainer.innerHTML = `
        <div class="p-3">
            <h4 class="mb-3">Продолжить просмотр?</h4>
            <p class="mb-1">${episodeTitle}</p>
            <p class="text-muted mb-3">Вы остановились на ${savedTime}</p>
            <button id="continueBtn" class="btn btn-primary me-2">Продолжить</button>
            <button id="startOverBtn" class="btn btn-secondary">Начать сначала</button>
        </div>
    `;
        promptContainer.classList.add('visible');

        // Находим эпизод, который нужно будет запустить
        const episodeToPlay = document.querySelector(`#episodeList li[data-episode-id='${progressData.episodeId}']`);
        if (!episodeToPlay) {
            hideProgressPrompt();
            return;
        }

        // Вешаем обработчики на кнопки
        document.getElementById('continueBtn').addEventListener('click', () => {
            activateAndLoadEpisode(episodeToPlay, progressData.progress);
            hideProgressPrompt();
        });

        document.getElementById('startOverBtn').addEventListener('click', () => {
            activateAndLoadEpisode(episodeToPlay, 0);
            hideProgressPrompt();
        });
    }

    /**
     * Скрывает уведомление о прогрессе.
     */
    function hideProgressPrompt() {
        const promptContainer = document.getElementById('progressPrompt');
        promptContainer.classList.remove('visible');
        // Очищаем содержимое после скрытия, чтобы не мешать кликам
        setTimeout(() => {
            promptContainer.innerHTML = '';
        }, 300); // 300ms - время анимации opacity
    }

    function activateAndLoadEpisode(liElement, startTime = 0) {
        document.querySelectorAll('#episodeList li').forEach(item => item.classList.remove('active'));
        liElement.classList.add('active');

        document.getElementById('episodeTitlePlaceholder').textContent = liElement.dataset.title;
        player.src({ type: 'video/mp4', src: `http://localhost:8083/api/stream/${liElement.dataset.source}` });

        player.one('loadedmetadata', () => {
            player.currentTime(startTime);
            player.play();
        });
    }

    let hasSentWatchRequest = false;

    function handleEpisodeClick(e) {
        if (!e.target || !e.target.matches('li.list-group-item')) return;
        hasSentWatchRequest = false; // Сбрасываем флаг при выборе нового эпизода
        activateAndLoadEpisode(e.target, 0);
    }

// --- ИЗМЕНЕННАЯ ЛОГИКА СОХРАНЕНИЯ ---

    /**
     * Запускает периодическое сохранение прогресса.
     */
    function handlePlayEvent() {
        console.log("Воспроизведение начато. Запускаем сохранение прогресса.");
        // Сначала останавливаем любой предыдущий интервал, чтобы избежать дублирования
        // Проверяем, был ли уже отправлен запрос на /api/watch
        if (!hasSentWatchRequest) {
            saveWatchToDB(serialId);
            hasSentWatchRequest = true; // Устанавливаем флаг, чтобы не отправлять повторно
        }
        clearInterval(saveProgressInterval);

        // Запускаем новый интервал, который будет сохранять прогресс каждые 10 секунд
        saveProgressInterval = setInterval(() => {
            const activeEpisode = document.querySelector('#episodeList li.active');
            if (!activeEpisode) return;

            const episodeId = activeEpisode.dataset.episodeId;
            const seasonId = activeEpisode.dataset.seasonNumber;
            const currentTime = player.currentTime();

            // Вызываем функцию сохранения
            saveEpisodeProgress(serialId, seasonId, episodeId, currentTime);

        }, 10000); // 10000 миллисекунд = 10 секунд
    }
    async function saveWatchToDB(serialId){
        console.log("Отправка запроса на отслеживание для serialId:", serialId);
        const response = await fetch(`/api/watch/${serialId}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({filmId: serialId})
        });
        if (!response.ok) {
            throw new Error(`Ошибка сервера: ${response.status}`);
        }
    }

    /**
     * Останавливает периодическое сохранение прогресса.
     */
    function handlePauseOrEndEvent() {
        console.log("Пауза или конец видео. Останавливаем сохранение прогресса.");
        clearInterval(saveProgressInterval);
    }


    async function fetchSerialProgress(serialFilmId) {
        if (!token) return null;
        try {
            console.log(`Запрашиваем прогресс для сериала с ID: ${serialFilmId}`);
            const response = await fetchWithAuth(`/api/progress/episode?filmId=${serialFilmId}`);
            if (response.ok && response.status !== 204) {
                return await response.json();
            }
            return null;
        } catch (error) {
            console.error("Ошибка загрузки прогресса сериала:", error);
            return null;
        }
    }

    async function saveEpisodeProgress(serialFilmId, seasonId, episodeId, time) {
        if (!token) return;

        // Добавили лог, чтобы видеть, что именно мы отправляем
        console.log(`Сохраняем прогресс: filmId=${serialFilmId}, seasonId=${seasonId}, episodeId=${episodeId}, time=${time.toFixed(2)}`);

        const url = `/api/progress/episode?filmId=${serialFilmId}&seasonId=${seasonId}&episodeId=${episodeId}&time=${time}`;

        try {
            await fetchWithAuth(url, { method: 'POST' });
            console.log("Запрос на сохранение отправлен успешно.");
        } catch (error) {
            console.error("Ошибка при отправке запроса на сохранение:", error);
        }
    }

// Старые функции fetchProgress, saveProgress и recordWatchHistory можно удалить,
// так как их заменили fetchSerialProgress и saveEpisodeProgress.

    // #################################################################
    // ## МОДУЛЬ: ИЗБРАННОЕ
    // #################################################################

    async function initializeFavorites(serialId) {
        const favButton = document.getElementById('favoriteButton');
        if (!favButton || !token) {
            if (favButton) favButton.style.display = 'none';
            return;
        }

        const isFav = await checkIfFavorite(serialId);
        updateFavoriteButtonUI(isFav);

        favButton.addEventListener('click', async () => {
            const currentState = favButton.dataset.favorite === 'true';
            const success = currentState ? await removeFavorite(serialId) : await addFavorite(serialId);
            if (success) {
                updateFavoriteButtonUI(!currentState);
            }
        });
    }

    function updateFavoriteButtonUI(isFavorite) {
        const favButton = document.getElementById('favoriteButton');
        const icon = favButton.querySelector('i');
        favButton.dataset.favorite = isFavorite;
        if (isFavorite) {
            icon.classList.replace('bi-heart', 'bi-heart-fill');
            favButton.classList.replace('btn-outline-danger', 'btn-danger');
            favButton.title = "Убрать из избранного";
        } else {
            icon.classList.replace('bi-heart-fill', 'bi-heart');
            favButton.classList.replace('btn-danger', 'btn-outline-danger');
            favButton.title = "Добавить в избранное";
        }
    }

    async function checkIfFavorite(serialId) {
        const response = await fetchWithAuth(`/api/favorites/check?filmId=${serialId}`);
        if (!response.ok) {
            // если что‑то пошло не так, считаем, что не в избранном
            return false;
        }
        const data = await response.json();           // ✅ теперь ждём JSON
        // data.isFavorite у тебя сейчас "true" или "false" (строки)
        return data.isFavorite === 'true';           // ✅ превращаем в boolean
    }

    async function addFavorite(serialId) {
        const response = await fetchWithAuth(`/api/favorites/add?filmId=${serialId}&type=serial`, { method: 'POST' });
        return response.ok;
    }

    async function removeFavorite(serialId) {
        const response = await fetchWithAuth(`/api/favorites/remove?filmId=${serialId}`, { method: 'DELETE' });
        return response.ok;
    }

    // #################################################################
    // ## МОДУЛЬ: КОММЕНТАРИИ (ИЗМЕНЕНИЯ ЗДЕСЬ)
    // #################################################################

    function initializeComments(serialId) {
        const commentsModule = document.getElementById('commentsModule');
        commentsModule.innerHTML = `
            <h4 class="mb-3">Комментарии</h4>
            ${token ? `
                <div class="card mb-4 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">Оставить комментарий</h5>
                        <form id="commentForm">
                            <div class="mb-3">
                                <textarea id="commentText" class="form-control" rows="3" placeholder="Введите ваш комментарий..." required></textarea>
                            </div>
                            <button type="submit" class="btn btn-primary">Отправить</button>
                        </form>
                    </div>
                </div>
            ` : '<div class="alert alert-info">Для добавления комментариев и реакций, пожалуйста, <a href="/login" class="alert-link">войдите в аккаунт</a>.</div>'}
            <div id="commentsListContainer"></div>
            <div id="commentsPaginationContainer" class="d-flex justify-content-center mt-3"></div>`;

        if (token) {
            document.getElementById('commentForm').addEventListener('submit', async (e) => {
                e.preventDefault();
                const text = document.getElementById('commentText').value;
                if (text.trim()) {
                    const success = await submitComment(serialId, text);
                    if (success) {
                        document.getElementById('commentText').value = '';
                        fetchComments(serialId, 0);
                    }
                }
            });
        }
        fetchComments(serialId, 0);
    }

    async function fetchComments(serialId, page) {
        try {
            commentsCurrentPage = page;
            const response = await fetch(`/api/comments?movieId=${serialId}&page=${page}&size=5&sortBy=time,desc`);
            if (!response.ok) throw new Error('Не удалось загрузить комментарии');
            const data = await response.json();
            renderComments(data);
        } catch (error) {
            document.getElementById('commentsListContainer').innerHTML = `<p class="text-danger">${error.message}</p>`;
        }
    }

    /**
     * Główna funkcja renderowania, która uruchamia rekurencyjne renderowanie dla każdego komentarza.
     */
    function renderComments(data) {
        const container = document.getElementById('commentsListContainer');
        if (data.content.length === 0) {
            container.innerHTML = '<p class="text-muted">Комментариев пока нет. Будьте первым!</p>';
            return;
        }
        // Запускаем рекурсивный рендеринг для каждого комментария верхнего уровня
        container.innerHTML = data.content.map(comment => renderCommentRecursive(comment, 0)).join('');

        renderPagination(data.totalPages, data.number);
        addCommentActionListeners();
    }

    /**
     * Рекурсивно рендерит комментарий и все его ответы, увеличивая отступ на каждом уровне.
     * @param {object} comment - Объект комментария.
     * @param {number} depth - Глубина вложенности (0 для комментариев верхнего уровня).
     */
    function renderCommentRecursive(comment, depth) {
        // Увеличиваем отступ на 25px за каждый уровень вложенности
        const marginStyle = `style="margin-left: ${depth * 25}px;"`;

        const commentHtml = `
            <div class="card mb-3" id="comment-${comment.id}" ${marginStyle}>
                <div class="card-body">
                    <p class="mb-1"><strong>${comment.username}</strong></p>
                    <p class="text-muted small mb-2">${new Date(comment.time).toLocaleString()}</p>
                    <p>${comment.text}</p>
                    <div class="comment-actions">
                        <button class="btn btn-sm btn-outline-success reaction-btn" data-comment-id="${comment.id}" data-reaction="LIKE" ${!token ? 'disabled' : ''}>
                            <i class="bi bi-hand-thumbs-up"></i> <span class="like-count">${comment.likes}</span>
                        </button>
                        <button class="btn btn-sm btn-outline-danger reaction-btn" data-comment-id="${comment.id}" data-reaction="DISLIKE" ${!token ? 'disabled' : ''}>
                            <i class="bi bi-hand-thumbs-down"></i> <span class="dislike-count">${comment.dislikes}</span>
                        </button>
                        <button class="btn btn-sm btn-link reply-btn ms-2" data-parent-id="${comment.id}" ${!token ? 'disabled' : ''}>Ответить</button>
                    </div>
                </div>
                <div class="reply-form-container ps-3 pe-3 pb-3" id="reply-form-for-${comment.id}" style="display:none;"></div>
                
                <div class="replies-container">${
            comment.replies && comment.replies.length > 0
                ? comment.replies.map(reply => renderCommentRecursive(reply, depth + 1)).join('')
                : ''
        }</div>
            </div>`;

        return commentHtml;
    }

    function addCommentActionListeners() {
        document.querySelectorAll('.reaction-btn').forEach(btn => btn.addEventListener('click', handleReactionClick));
        document.querySelectorAll('.reply-btn').forEach(btn => btn.addEventListener('click', handleReplyClick));
    }

    /**
     * Обрабатывает клик по кнопке реакции. После успешного запроса перезагружает все комментарии.
     */
    async function handleReactionClick(e) {
        const button = e.currentTarget;
        const commentId = button.dataset.commentId;
        const reaction = button.dataset.reaction;

        button.parentElement.querySelectorAll('.reaction-btn').forEach(btn => btn.disabled = true);

        try {
            const success = await submitReaction(commentId, reaction);
            if (success) {
                // Лучшее решение: перезагрузить комментарии, чтобы получить актуальные данные от сервера
                fetchComments(serialId, commentsCurrentPage);
            } else {
                button.parentElement.querySelectorAll('.reaction-btn').forEach(btn => btn.disabled = false);
            }
        } catch (error) {
            console.error("Ошибка реакции:", error);
            button.parentElement.querySelectorAll('.reaction-btn').forEach(btn => btn.disabled = false);
        }
    }

    function handleReplyClick(e) {
        const parentId = e.currentTarget.dataset.parentId;
        const formContainer = document.getElementById(`reply-form-for-${parentId}`);

        if (formContainer.style.display === 'block') {
            formContainer.style.display = 'none';
        } else {
            formContainer.style.display = 'block';
            formContainer.innerHTML = `
                <form class="replyForm" data-parent-id="${parentId}">
                    <textarea class="form-control form-control-sm mb-2" rows="2" placeholder="Ваш ответ..." required></textarea>
                    <button type="submit" class="btn btn-sm btn-primary">Отправить ответ</button>
                </form>`;
            formContainer.querySelector('.replyForm').addEventListener('submit', handleReplySubmit);
        }
    }

    async function handleReplySubmit(e) {
        e.preventDefault();
        const form = e.currentTarget;
        const parentId = form.dataset.parentId;
        const text = form.querySelector('textarea').value;
        if (text.trim()) {
            const success = await submitReply(parentId, text);
            if (success) {
                fetchComments(serialId, commentsCurrentPage);
            }
        }
    }

    function renderPagination(totalPages, currentPage) {
        const container = document.getElementById('commentsPaginationContainer');
        if (totalPages <= 1) {
            container.innerHTML = '';
            return;
        }
        let html = '<nav><ul class="pagination">';
        for (let i = 0; i < totalPages; i++) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
            </li>`;
        }
        html += '</ul></nav>';
        container.innerHTML = html;
        container.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                fetchComments(serialId, parseInt(e.target.dataset.page));
            });
        });
    }

    async function submitComment(serialId, text) {
        const response = await fetchWithAuth('/api/comments', {
            method: 'POST',
            body: JSON.stringify({ filmId: serialId, text: text })
        });
        return response.ok;
    }

    async function submitReply(parentId, text) {
        const response = await fetchWithAuth(`/api/comments/reply?parentId=${parentId}`, {
            method: 'POST',
            body: JSON.stringify({ text: text })
        });
        return response.ok;
    }

    async function submitReaction(commentId, reaction) {
        // Убедимся, что отправляем поле reactionType, как ожидает API
        const response = await fetchWithAuth(`/api/comments/${commentId}/reactions`, {
            method: 'POST',
            body: JSON.stringify({ reactionType: reaction })
        });
        return response.ok;
    }

    // #################################################################
    // ## МОДУЛЬ: НЕДАВНИЕ КОММЕНТАРИИ (САЙДБАР)
    // #################################################################

    async function loadRecentComments() {
        try {
            const response = await fetch('/api/comments/recently');
            if (!response.ok) return;
            const comments = await response.json();
            renderRecentComments(comments);
        } catch (error) {
            console.error("Ошибка загрузки недавних комментариев:", error);
        }
    }

    function renderRecentComments(comments) {
        if (!comments || comments.length === 0) {
            recentCommentsList.innerHTML = '<p class="text-muted small">Пока нет недавних комментариев.</p>';
            return;
        }
        recentCommentsList.innerHTML = comments.map(c => `
            <div class="recent-comment-item">
                <div class="comment-author">
                    <strong>${c.username}</strong> к <a href="/serials/${c.filmId}" title="${c.movieTitle}">${c.movieTitle}</a>
                </div>
                <p class="comment-text">${c.text.substring(0, 80)}...</p>
            </div>
        `).join('');
    }

    // #################################################################
    // ## ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ FETCH
    // #################################################################

    async function fetchWithAuth(url, options = {}) {
        if (!token) {
            return Promise.reject("No token found");
        }
        const headers = {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
            ...options.headers,
        };
        return fetch(url, { ...options, headers });
    }

    // --- ЗАПУСК ВСЕГО ПРИ ЗАГРУЗКЕ СТРАНИЦЫ ---
    initializePage();
});
