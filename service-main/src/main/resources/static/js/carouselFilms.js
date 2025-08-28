document.addEventListener("DOMContentLoaded", function () {
    const container = document.getElementById("carouselMoviesContainer");
    const carouselDotContainer = document.getElementById("carouselDotsContainer");
    let currentIndex = 0;
    const visibleCount = 5;
    let totalFilms = 0;
    let films = [];

    async function loadFilms() {
        try {
            const response = await fetch("/api/films/carousel"); // Новый эндпоинт
            films = await response.json();
            totalFilms = films.length;
            renderFilms(films);
        } catch (error) {
            console.error("Ошибка при загрузке фильмов", error);
        }
    }

    function renderFilms(films) {
        container.innerHTML = "";
        carouselDotContainer.innerHTML = "";
        films.forEach((film) => {
            const card = document.createElement("div");
            card.className = "movie-card";
            card.onclick = () => {
                let baseUrl;
                if (film.type === "movie") {
                    baseUrl = "/movie/";
                } else if (film.type === "serial") {
                    baseUrl = "/serial/";
                } else {
                    baseUrl = "/film/";
                }
                window.location.href = baseUrl + film.id;
            };
            card.style.cursor = "pointer";
            card.innerHTML = `
                <div class="card h-100">
                    <img src="${film.poster}" class="card-img-top" alt="${film.title}">
                    <div class="card-body">
                        <h5 class="card-title">${film.title}</h5>
                        <p class="card-text">Рейтинг: ${film.rating}</p>
                    </div>
                </div>
            `;
            container.appendChild(card);
        });
        const pageCount = Math.ceil(totalFilms / visibleCount);
        for (let i = 0; i < pageCount; i++) {
            const dot = document.createElement("button");
            dot.className = "carousel-dot";
            dot.dataset.index = i * visibleCount;
            dot.addEventListener("click", () => {
                goToSlide(i * visibleCount);
                updateDots(i);
            });
            carouselDotContainer.appendChild(dot);
        }
        if (carouselDotContainer.children.length > 0) {
            carouselDotContainer.children[0].classList.add("active");
        }
    }

    function goToSlide(index) {
        currentIndex = index;
        updateCarousel();
    }

    function updateDots(activePageIndex) {
        const dots = document.querySelectorAll(".carousel-dot");
        dots.forEach((dot) => dot.classList.remove("active"));
        if (dots[activePageIndex]) {
            dots[activePageIndex].classList.add("active");
        }
    }

    function updateCarousel() {
        const card = container.querySelector(".movie-card");
        if (!card) return;
        const cardWidth = card.offsetWidth;
        const maxIndex = totalFilms - visibleCount;
        if (currentIndex < 0) currentIndex = maxIndex;
        if (currentIndex > maxIndex) currentIndex = 0;
        const offset = currentIndex * cardWidth;
        container.style.transform = `translateX(-${offset}px)`;
        const pageIndex = Math.floor(currentIndex / visibleCount);
        updateDots(pageIndex);
    }

    document.getElementById("nextBtn").addEventListener("click", () => {
        currentIndex += 1;
        updateCarousel();
    });

    document.getElementById("prevBtn").addEventListener("click", () => {
        currentIndex -= 1;
        updateCarousel();
    });

    loadFilms();
});
