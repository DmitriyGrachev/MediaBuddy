// Ждем, пока вся страница загрузится
document.addEventListener('DOMContentLoaded', function() {

    // Получаем ID фильма из URL
    const pathParts = window.location.pathname.split('/');
    const filmId = pathParts[pathParts.length - 1];

    // Элементы для отображения данных
    const filmAverageRatingEl = document.getElementById('filmAverageRating');
    const filmVoteCountEl = document.getElementById('filmVoteCount');
    const userRatingStarsContainer = document.getElementById('userRatingStars');
    const userRatingValueEl = document.getElementById('userRatingValue');
    const ratingErrorEl = document.getElementById('ratingError');
    const ratingSuccessEl = document.getElementById('ratingSuccess');

    let currentUserRating = 0; // Сохраняем текущую оценку пользователя
    const token = sessionStorage.getItem("token");
    // --- 1. Функция для отрисовки звезд (ИЗМЕНЕНА) ---
    function renderStars(container, selectedValue, isUserRated = false) {
        container.innerHTML = ''; // Очищаем контейнер
        for (let i = 1; i <= 10; i++) {
            const star = document.createElement('span');
            star.classList.add('star', 'bi', 'bi-star-fill');
            star.dataset.value = i;

            if (i <= selectedValue) {
                star.classList.add('selected');
                // Добавляем специальный класс для оценки пользователя
                if (isUserRated) {
                    star.classList.add('user-rated');
                    console.log(`Star ${i} marked as user-rated`);
                }
            }
            container.appendChild(star);
        }
    }

    // --- 2. Функция для загрузки данных о рейтинге (ИЗМЕНЕНА) ---
    async function fetchRatingData() {
        if (!filmId) return;

        try {
            const response = await fetch(`/api/rating/${filmId}`, {
                headers: {
                    'Authorization': 'Bearer ' + token // Добавляем токен в заголовок
                }
            });

            if (!response.ok) {
                filmAverageRatingEl.textContent = 'N/A';
                filmVoteCountEl.textContent = '0';
                renderStars(userRatingStarsContainer, 0, false);
                return;
            }
            const data = await response.json();
            console.log('Received rating data:', data);

            filmAverageRatingEl.textContent = data.filmRating ? data.filmRating.toFixed(1) : 'N/A';
            filmVoteCountEl.textContent = data.votes || '0';
            currentUserRating = data.userRating || 0;

            console.log('Current user rating:', currentUserRating);

            if (currentUserRating > 0) {
                userRatingValueEl.textContent = `Ваша оценка: ${currentUserRating}/10`;
                // Отображаем звезды с индикацией, что пользователь уже голосовал
                renderStars(userRatingStarsContainer, currentUserRating, true);
            } else {
                userRatingValueEl.textContent = 'Вы еще не голосовали';
                renderStars(userRatingStarsContainer, 0, false);
            }
        } catch (error) {
            console.error('Error fetching rating data:', error);
            ratingErrorEl.textContent = 'Не удалось загрузить данные о рейтинге.';
            ratingErrorEl.classList.remove('d-none');
        }
    }

    // --- 3. Функция для отправки рейтинга (ИЗМЕНЕНА) ---
    async function postRating(ratingValue) {
        ratingErrorEl.classList.add('d-none');
        ratingSuccessEl.classList.add('d-none');
        const token = sessionStorage.getItem("token");
        if (!token) {
            ratingErrorEl.textContent = 'Для оценки необходимо авторизоваться.';
            ratingErrorEl.classList.remove('d-none');
            return;
        }
        try {
            const response = await fetch(`/api/rating/${filmId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify({ rating: ratingValue })
            });
            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                const errorMessage = errorData?.message || `Ошибка ${response.status}. Возможно, вы не авторизованы.`;
                throw new Error(errorMessage);
            }

            console.log('Rating submitted successfully:', ratingValue);

            // Обновляем текущую оценку пользователя
            currentUserRating = ratingValue;

            ratingSuccessEl.textContent = `Вы успешно оценили фильм на ${ratingValue}! Рейтинг обновлен.`;
            ratingSuccessEl.classList.remove('d-none');

            // Перезагружаем данные, чтобы увидеть обновленный средний рейтинг
            await fetchRatingData();
        } catch (error) {
            console.error('Error submitting rating:', error);
            ratingErrorEl.textContent = error.message;
            ratingErrorEl.classList.remove('d-none');
            // В случае ошибки возвращаем звезды к последней сохраненной оценке
            renderStars(userRatingStarsContainer, currentUserRating, currentUserRating > 0);
        }
    }

    // --- 4. Добавляем обработчики событий на звезды (ИЗМЕНЕНА ЛОГИКА) ---

    // Клик работает так же
    userRatingStarsContainer.addEventListener('click', (event) => {
        if (event.target.classList.contains('star')) {
            const ratingValue = parseInt(event.target.dataset.value, 10);
            postRating(ratingValue);
        }
    });

    // При наведении мы временно подсвечиваем звезды
    userRatingStarsContainer.addEventListener('mouseover', (event) => {
        if (event.target.classList.contains('star')) {
            const hoverValue = parseInt(event.target.dataset.value, 10);
            // Подсвечиваем звезды до той, на которую навели курсор
            userRatingStarsContainer.querySelectorAll('.star').forEach(star => {
                const starValue = parseInt(star.dataset.value, 10);
                star.classList.toggle('selected', starValue <= hoverValue);
                // Убираем класс user-rated при наведении
                star.classList.remove('user-rated');
            });
        }
    });

    // Когда курсор уходит, возвращаем отображение к сохраненной оценке
    userRatingStarsContainer.addEventListener('mouseout', () => {
        // Перерисовываем звезды с последним сохраненным значением
        renderStars(userRatingStarsContainer, currentUserRating, currentUserRating > 0);
    });

    // --- 5. Первоначальная загрузка данных ---
    fetchRatingData();
});
