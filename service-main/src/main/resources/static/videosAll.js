document.addEventListener("DOMContentLoaded", function (){
    const videosContainer = document.getElementById("videosContainer");
    const errorVideosContainer = document.getElementById("videosError");
    const addDirectoryModal = document.getElementById("addDirectoryModal");
    const newDirectoryForm = document.getElementById("newDirectoryForm");
    const newDirectoryNameInput = document.getElementById("description");
    const addDirectoryButtonContainer = document.getElementById("addDirectoryButton");
    const token = sessionStorage.getItem("token");

    // Переменная для предотвращения повторной отправки
    let isSubmitting = false;

    function fetchVideoDirectories() {
        videosContainer.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="visually-hidden">Загрузка...</span></div></div>';

        // Очищаем предыдущие сообщения об ошибках
        errorVideosContainer.innerHTML = '';

        fetch(`/api/videos`, {
            headers: {
                'Accept': 'application/json',
                "Authorization": `Bearer ${token}`
            }
        })
            .then(response => {
                console.log('Response status:', response.status);
                console.log('Response headers:', response.headers.get('Content-Type'));
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        sessionStorage.removeItem("token");
                        window.location.href = "/authfront/login";
                        return null;
                    }
                    throw new Error(`Error with video directories: ${response.status}`);
                }
                return response.json();
            })
            .then(videoDirectories => {
                console.log('Fetched directories:', videoDirectories);
                renderVideoDirectories(videoDirectories);
            })
            .catch(error => {
                console.error('Ошибка при получении данных:', error);
                errorVideosContainer.innerHTML = '<div class="alert alert-danger">Ошибка загрузки данных: ' + error.message + '</div>';
            });
    }

    function renderAddButton() {
        addDirectoryButtonContainer.innerHTML = `
        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addDirectoryModal">Add new directory</button>`;
    }

    // Устанавливаем обработчик событий для формы
    newDirectoryForm.addEventListener('submit', function(event) {
        event.preventDefault();

        if (isSubmitting) {
            console.log('Form submission blocked: already in progress');
            return; // Блокируем повторную отправку
        }

        const newDirectoryName = newDirectoryNameInput.value.trim();
        console.log('Form submitted with description:', newDirectoryName);

        if (newDirectoryName !== "") {
            isSubmitting = true; // Устанавливаем флаг отправки

            // Показываем индикатор загрузки в форме
            const submitButton = newDirectoryForm.querySelector('button[type="submit"]');
            const originalButtonText = submitButton.innerHTML;
            submitButton.disabled = true;
            submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Загрузка...';

            fetch('/api/videos', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ description: newDirectoryName })
            })
                .then(response => {
                    if (response.status !== 201) {
                        throw new Error(`Ошибка при создании директории: ${response.status}`);
                    }
                    return response; // Сервер не возвращает тело, поэтому просто возвращаем response
                })
                .then(() => {
                    console.log('Директория успешно создана');
                    fetchVideoDirectories();
                    const modal = bootstrap.Modal.getInstance(addDirectoryModal);
                    modal.hide();
                    newDirectoryNameInput.value = '';
                })
                .catch(error => {
                    console.error('Ошибка при отправке POST-запроса:', error);
                    errorVideosContainer.innerHTML = '<div class="alert alert-danger">Ошибка создания директории: ' + error.message + '</div>';
                })
                .finally(() => {
                    isSubmitting = false; // Сбрасываем флаг после завершения
                    submitButton.disabled = false;
                    submitButton.innerHTML = originalButtonText;
                });
        } else {
            errorVideosContainer.innerHTML = '<div class="alert alert-warning">Пожалуйста, введите название директории.</div>';
        }
    });

    function renderVideoDirectories(videoDirectories) {
        videosContainer.innerHTML = '';

        const directories = videoDirectories || [];

        if (directories.length === 0) {
            videosContainer.innerHTML = '<p class="text-muted">Видео не найдены.</p>';
            return;
        }

        directories.forEach(directory => {
            const directoryCard = `
            <div class="col-sm-6 col-md-4 mb-3">
                <a href="/videos/${directory.id}" class="card-link">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title">Directory ${directory.id}</h5>
                            <p class="card-text">${
                directory.videoFrames && directory.videoFrames.length > 0
                    ? directory.videoFrames[0].title || 'Без названия'
                    : 'Видео отсутствуют'
            }</p>
                            <h5 class="card-title">${directory.description || 'Без описания'}</h5>
                        </div>
                    </div>
                </a>
            </div>
        `;
            videosContainer.insertAdjacentHTML('beforeend', directoryCard);
        });
    }

    renderAddButton();
    fetchVideoDirectories();
});
