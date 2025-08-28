/**
 * Navigation menu search functionality
 */
document.addEventListener("DOMContentLoaded", function () {
    console.log('menuNav.js loaded');

    // Get search elements
    const searchInput = document.getElementById('searchInput');
    const searchSuggestions = document.getElementById('searchSuggestions');

    // Check if elements exist
    if (!searchInput || !searchSuggestions) {
        console.error('Search elements not found');
        return;
    }

    let searchTimeout;

    // Handle input in search box
    searchInput.addEventListener('input', function(e) {
        e.preventDefault();
        clearTimeout(searchTimeout);

        // Add debounce to prevent excessive API calls
        searchTimeout = setTimeout(async () => {
            const query = this.value.trim();

            // Only search if query has at least 2 characters
            if (query.length < 2) {
                hideSearchSuggestions();
                return;
            }

            try {
                //TODO MAYBE REPLACE WITH ELASTICSEARCH

                // Fetch search results
                const response = await fetch(`/api/movies/fastsearch?query=${encodeURIComponent(query)}`, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                });

                if (!response.ok) {
                    throw new Error(`HTTP error: ${response.status}`);
                }

                const movies = await response.json();

                // Display results
                updateSearchSuggestions(movies);
            } catch (error) {
                console.error('Search error:', error.message);
                hideSearchSuggestions();
            }
        }, 300);
    });

    // Close search suggestions when clicking outside
    document.addEventListener('click', function(e) {
        if (!searchInput.contains(e.target)) {
            hideSearchSuggestions();
        }
    });

    function updateSearchSuggestions(movies) {
        searchSuggestions.innerHTML = movies.map(movie => `
      <a class="dropdown-item" href="/movie/${movie.id}">${movie.title}</a>
    `).join('');

        if (movies.length) {
            searchSuggestions.classList.add('show');
        } else {
            hideSearchSuggestions();
        }
    }

    function hideSearchSuggestions() {
        searchSuggestions.classList.remove('show');
        searchSuggestions.innerHTML = '';
    }
});
