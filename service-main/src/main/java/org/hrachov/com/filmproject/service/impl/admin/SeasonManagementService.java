package org.hrachov.com.filmproject.service.impl.admin;

import lombok.AllArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.hrachov.com.filmproject.exception.SeasonNotFoundException;
import org.hrachov.com.filmproject.model.Season;
import org.hrachov.com.filmproject.model.dto.SeasonDTO;
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
public class SeasonManagementService {
    private final FilmService filmService;
    private final SeasonRepository seasonRepository;

    @Transactional
    public SeasonDTO createSeason(Long serialId, SeasonDTO seasonDTO) {
        seasonDTO.setSerialFilmId(serialId);
        Season savedSeason = filmService.saveSeasonFromDTO(seasonDTO);
        return filmService.convertToSeasonDTO(savedSeason);
    }

    public SeasonDTO getSeasonById(Long id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new SeasonNotFoundException(id));
        return filmService.convertToSeasonDTO(season);
    }

    @Transactional
    public SeasonDTO updateSeason(Long id, SeasonDTO seasonDTO) {
        Season updatedSeason = filmService.updateSeasonFromDTO(id, seasonDTO);
        return filmService.convertToSeasonDTO(updatedSeason);
    }

    @Transactional
    public void deleteSeason(Long id) {
        filmService.deleteSeasonById(id);
    }
}
