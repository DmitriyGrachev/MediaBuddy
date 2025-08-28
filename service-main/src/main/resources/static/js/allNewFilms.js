/**
 * New films loader and display functionality
 */
document.addEventListener("DOMContentLoaded", function () {
    const newFilmsContainer = document.getElementById("newFilmsContainer");

    // Exit if container doesn't exist on current page
    if (!newFilmsContainer) return;

    /**
     * Load new films from API
     */
    async function loadNewFilms() {
        try {
            const response = await fetch('/api/films/newFilms');

            if (!response.ok) {
                throw new Error(`HTTP error: ${response.status}`);
            }

            const films = await response.json();
            renderFilms(films);
        } catch (error) {
            console.error("Error loading films:", error);
            newFilmsContainer.innerHTML = '<div class="alert alert-danger">Failed to load new films</div>';
        }
    }

    /**
     * Render films in the container
     * @param {Array} films - Array of film objects
     */
    function renderFilms(films) {
        // Clear container
        newFilmsContainer.innerHTML = "";

        // Create row for cards
        const row = document.createElement("div");
        row.className = "card-row";

        // Add each film
        films.forEach(film => {
            // Create card wrapper
            const col = document.createElement("div");
            col.className = "card-wrapper";

            // Determine correct link based on content type
            const contentLink = getContentLink(film);

            // Create card
            const card = document.createElement("div");
            card.className = "card h-100 shadow-sm";
            card.style.cursor = "pointer";
            card.onclick = () => { window.location.href = contentLink; };

            // Set card content
            card.innerHTML = `
        <img src="${film.poster}" class="card-img-top" alt="${film.title}">
        <div class="card-body">
          <h5 class="card-title">${film.title}</h5>
          <p class="card-text">Rating: ${film.rating}</p>
        </div>
      `;

            // Add card to row
            col.appendChild(card);
            row.appendChild(col);
        });

        // Add completed row to container
        newFilmsContainer.appendChild(row);
    }

    /**
     * Get the correct link for content based on type
     * @param {Object} content - Content object with type and id
     * @returns {string} URL for the content
     */
    function getContentLink(content) {
        switch(content.type) {
            case "movie":
                return `/movie/${content.id}`;
            case "serial":
                return `/serial/${content.id}`;
            default:
                return `/content/${content.id}`;
        }
    }

    // Load films when page loads
    loadNewFilms();
});
