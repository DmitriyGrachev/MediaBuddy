package org.hrachov.com.filmproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hrachov.com.filmproject.model.Season;
import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.repository.jpa.SerialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SerialService {

    private final SerialRepository serialRepository;

    @Transactional(readOnly = true) // Транзакция обязательна для работы ленивой загрузки!
    public Optional<Serial> getSerialDetails(Long serialId) {
        // Шаг 1: Загружаем сериал с его сезонами
        Optional<Serial> serialOptional = serialRepository.findByIdWithSeasons(serialId);

        if (serialOptional.isPresent()) {
            Serial serial = serialOptional.get();
            // Шаг 2: Для каждого сезона инициализируем список его эпизодов
            // Аннотация @OrderBy в сущности Season на поле episodeList позаботится о сортировке эпизодов.
            for (Season season : serial.getSeasonList()) {
                Hibernate.initialize(season.getEpisodeList());
                // Альтернативный способ инициализации, если Hibernate.initialize недоступен или нежелателен:
                // season.getEpisodeList().size(); // Простое обращение к коллекции также инициирует её загрузку
            }
        }
        return serialOptional;
    }
}
