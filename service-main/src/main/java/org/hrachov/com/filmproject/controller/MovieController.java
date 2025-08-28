package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.MovieNotFoundInDb;
import org.hrachov.com.filmproject.exception.MoviesNotFound;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movie")
@AllArgsConstructor
public class MovieController {

    private static final Logger logger = LoggerFactory.getLogger(MovieController.class); // Добавляем логгер

    private final MovieService movieService;
    private final CommentService commentService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;


    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> getMovie(@PathVariable long id) {
        MovieDTO movie = movieService.getMovieDtoById(id);
        if (movie == null) {
            throw new MovieNotFoundInDb("Movie not found");
        }
        List<CommentDTO> comments = objectMapper.convertValue(commentService.getCommentsByMovie(movie.getId()),new TypeReference<List<CommentDTO>>(){});
        movie.setComments(comments);
        return ResponseEntity.ok(movie);
    }
    @GetMapping("/stream/{fileName}")
    public void getMovieRange(@PathVariable String fileName,
                              @RequestHeader HttpHeaders headers, // Заголовки от клиента
                              HttpServletResponse response) {
        try {
            if (fileName.contains("..")) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid path characters.");
                return;
            }
            fileName = "movies/" + fileName;
            String microserviceUrl = "http://localhost:8083/api/stream/" + fileName;

            RequestCallback requestCallback = httpRequest -> {
                List<HttpRange> clientRanges = headers.getRange();
                if (!clientRanges.isEmpty()) {
                    String rangesValue = clientRanges.stream()
                            .map(HttpRange::toString)
                            .collect(Collectors.joining(", "));
                    httpRequest.getHeaders().set(HttpHeaders.RANGE, "bytes=" + rangesValue);
                    logger.debug("Proxying request to microservice with Range header: bytes={}", rangesValue);
                }
            };

            String finalFileName = fileName;
            ResponseExtractor<Void> responseExtractor = microserviceResponse -> {
                response.setStatus(microserviceResponse.getStatusCode().value());
                logger.debug("Microservice response status: {}", microserviceResponse.getStatusCode());

                microserviceResponse.getHeaders().forEach((name, values) -> {
                    values.forEach(value -> {
                         response.addHeader(name, value);
                    });
                });
                logger.debug("Copied headers from microservice response to client response.");


                try (InputStream inputStream = microserviceResponse.getBody()) {
                    StreamUtils.copy(inputStream, response.getOutputStream());
                    response.getOutputStream().flush();
                    logger.debug("Successfully streamed data to client for: {}", finalFileName);
                } catch (IOException e) {
                    logger.error("Error streaming data to client for {}: {}", finalFileName, e.getMessage());
                    if (!response.isCommitted()) {
                        try {
                            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error during data streaming.");
                        } catch (IOException e1) {
                            logger.error("Failed to send error response to client after streaming error: {}", e1.getMessage());
                        }
                    }
                }
                return null;
            };

            try {
                logger.info("Attempting to proxy stream request for {} to {}", fileName, microserviceUrl);
                restTemplate.execute(new URI(microserviceUrl), HttpMethod.GET, requestCallback, responseExtractor);
            } catch (URISyntaxException e) {
                logger.error("Invalid microservice URL syntax: {}", microserviceUrl, e);
                if (!response.isCommitted()) {
                    sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal configuration error.");
                }
            } catch (Exception e) {
                logger.error("Error connecting to or processing response from streaming microservice for {}: {}", fileName, e.getMessage(), e);
                if (!response.isCommitted()) {
                    sendError(response, HttpStatus.SERVICE_UNAVAILABLE, "Video streaming service is temporarily unavailable.");
                }
            }
        } catch (IOException e) {
            logger.error("IOException while initially processing request for {}: {}", fileName, e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) {
        try {
            if (!response.isCommitted()) {
                response.sendError(status.value(), message);
            }
        } catch (IOException ioException) {
            logger.warn("Failed to send error response (status: {}, message: {}) to client: {}", status, message, ioException.getMessage());
        }
    }
}