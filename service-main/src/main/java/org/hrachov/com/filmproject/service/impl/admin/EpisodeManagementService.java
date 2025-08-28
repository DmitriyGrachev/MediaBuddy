package org.hrachov.com.filmproject.service.impl.admin;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.EpisodeFoundException;
import org.hrachov.com.filmproject.model.Episode;
import org.hrachov.com.filmproject.model.dto.EpisodeDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.BlockService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.ExternalApiService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class EpisodeManagementService {
    private final FilmService filmService;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public EpisodeDTO createEpisode(Long seasonId, EpisodeDTO episodeDTO) {
        episodeDTO.setSeasonId(seasonId);
        Episode savedEpisode = filmService.saveEpisodeFromDTO(episodeDTO);
        return filmService.convertToEpisodeDTO(savedEpisode);
    }

    public EpisodeDTO getEpisodeById(Long id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() ->  new EpisodeFoundException(id));
        return filmService.convertToEpisodeDTO(episode);
    }

    @Transactional
    public EpisodeDTO updateEpisode(Long id, EpisodeDTO episodeDTO) {
        Episode updatedEpisode = filmService.updateEpisodeFromDTO(id, episodeDTO);
        return filmService.convertToEpisodeDTO(updatedEpisode);
    }

    @Transactional
    public void deleteEpisode(Long id) {
        filmService.deleteEpisodeById(id);
    }
}
