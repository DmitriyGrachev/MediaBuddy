package org.hrachov.com.filmproject.service;

import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.UserRating;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRatingRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;


public interface UserRatingService {
    public void rateMovie(Long userId, Long filmId,int newRatingValue);
}
