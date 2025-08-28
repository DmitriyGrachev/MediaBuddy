document.addEventListener("DOMContentLoaded", function () {
    const videosInRepository = document.getElementById("videosInRepository");
    const videosErrorInRepo = document.getElementById("videosErrorInRepo");

    if (videosInRepository && videosErrorInRepo) {
        const pathParts = window.location.pathname.split('/');
        const directoryId = pathParts[pathParts.length - 1];

        async function fetchVideosFromRepo() {
            videosInRepository.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="visually-hidden">Загрузка...</span></div></div>';

            try {
                const response = await fetch(`/api/videos/${directoryId}`);
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error(`Error occurred: ${response.status}`);
                }
                const directory = await response.json();
                console.log('Fetched directory:', directory);
                renderVideos(directory);
            } catch (error) {
                console.error('Error in getting videos:', error);
                videosErrorInRepo.innerHTML = '<div class="alert alert-danger">Ошибка загрузки данных: ' + error.message + '</div>';
            }
        }

        function renderVideos(directory) {
            videosInRepository.innerHTML = '';

            const videos = directory.videoFrames || [];

            if (videos.length === 0) {
                videosInRepository.innerHTML = '<p class="text-muted">Видео не найдены.</p>';
                return;
            }

            videos.forEach(video => {
                const videoCard = `
                    <div class="row mb-4">
                        <!-- Левая колонка: видео и описание -->
                        <div class="col-md-8">
                            <h5>${video.title || 'Без названия'}</h5>
                            <div class="ratio ratio-16x9">
                                <iframe class="embed-responsive-item" src="${video.url}" allowfullscreen></iframe>
                            </div>
                            <p>${video.description || 'Описание отсутствует'}</p>
                        </div>
                        <!-- Правая колонка: поле для текста и кнопка -->
                        <div class="col-md-4">
                            <div id="summary-${video.id}" class="border p-3 mb-3" style="min-height: 100px;">
                                <p class="text-muted">Нажмите кнопку, чтобы загрузить резюме.</p>
                            </div>
                            <button class="btn btn-primary" onclick="fetchSummary(${video.id}, ${directoryId})">Загрузить резюме</button>
                        </div>
                    </div>
                `;
                videosInRepository.insertAdjacentHTML('beforeend', videoCard);
            });
        }

        // Функция для загрузки резюме
        window.fetchSummary = async function(videoId, directoryId) {
            const summaryContainer = document.getElementById(`summary-${videoId}`);
            summaryContainer.innerHTML = '<div class="text-center"><div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">Загрузка...</span></div></div>';

            try {
                const response = await fetch(`/api/videos/summary?videoId=${videoId}`);
                if (!response.ok) {
                    throw new Error(`Error fetching summary: ${response.status}`);
                }
                const summary = await response.text();
                summaryContainer.innerHTML = `<p>${summary || 'Резюме отсутствует'}</p>`;
            } catch (error) {
                console.error('Error fetching summary:', error);
                summaryContainer.innerHTML = `<p class="text-danger">Ошибка: ${error.message}</p>`;
            }
        };

        fetchVideosFromRepo();

        const addForm = document.getElementById("addVideoForm");
        const resultBox = document.getElementById("videoAddResult");

        if (addForm) {
            addForm.addEventListener("submit", async (e) => {
                e.preventDefault();

                const title = document.getElementById("videoTitle").value;
                const description = document.getElementById("videoDescription").value;
                const url = document.getElementById("videoUrl").value;

                try {
                    const response = await fetch(`/api/videos/${directoryId}/frames`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({ title, description, url })
                    });

                    if (!response.ok) {
                        throw new Error("Ошибка добавления видео");
                    }

                    resultBox.innerHTML = '<div class="alert alert-success">Видео добавлено!</div>';
                    addForm.reset();
                    fetchVideosFromRepo(); // обновляем список
                } catch (err) {
                    resultBox.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
                }
            });
        }

    }
});
