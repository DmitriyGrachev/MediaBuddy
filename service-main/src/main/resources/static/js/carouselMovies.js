document.addEventListener("DOMContentLoaded", function () {
    const container = document.getElementById("carouselMoviesContainer");
    const carouselDotContainer = document.getElementById("carouselDotsContainer");
    let currentIndex = 0;
    const visibleCount = 5;
    let totalMovies = 0;
    let movies = [];

    async function loadMovies() {
        try {
            const response = await fetch("/api/movies/carousel"); // Твой эндпоинт
            movies = await response.json();
            totalMovies = movies.length;
            renderMovies(movies);
        } catch (error) {
            console.error("Ошибка при загрузке фильмов", error);
        }
    }

    function renderMovies(movies) {
        container.innerHTML = "";
        carouselDotContainer.innerHTML = "";
        movies.forEach((movie) => {
            const card = document.createElement("div");
            card.className = "movie-card";
            card.onclick = () => window.location.href = `/movie/${movie.id}`;
            card.style.cursor = "pointer";
            card.innerHTML = `
                <div class="card h-100">
                    <div class="card-img-top-wrapper">
                        <img src="${movie.poster}" class="card-img-top" alt="${movie.title}">
                    </div>
                    <div class="card-body">
                        <h5 class="card-title">${movie.title}</h5>
                        <p class="card-text">Рейтинг: ${movie.rating}</p>
                    </div>
                </div>
            `;
            container.appendChild(card);
        });
        const pageCount = Math.ceil(totalMovies / visibleCount);
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
        const maxIndex = totalMovies - visibleCount;
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

    loadMovies();
});