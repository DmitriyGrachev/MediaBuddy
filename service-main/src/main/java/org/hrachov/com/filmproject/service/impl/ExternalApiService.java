package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.dto.OmdbDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class ExternalApiService {

    private final RestTemplate restTemplate;
    private static final String OMDB_API_URL = "http://www.omdbapi.com/?t={title}&plot=full&apikey=33046913";

    /**
     * Получает информацию о фильме из OMDB API
     */
    public OmdbDTO getFilmFromOmdb(String title) {
        try {
            return restTemplate.getForObject(OMDB_API_URL, OmdbDTO.class, title);
        } catch (RestClientException e) {
            throw new RuntimeException("Ошибка при обращении к OMDB API: " + e.getMessage(), e);
        }
    }
}