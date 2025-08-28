package org.hrachov.com.filmproject.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@ControllerAdvice
public class ExceptionHandlerAdvice{
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        System.err.println("Authentication error: " + ex.getMessage());
        return Map.of(
                "error", "User not found",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler({io.jsonwebtoken.JwtException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleJwtException(Exception ex) {
        System.err.println("JWT error: " + ex.getMessage());
        return Map.of(
                "error", "Invalid token",
                "message", ex.getMessage()
        );
    }
    @ExceptionHandler({UserNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleUserNotFoundException(UserNotFoundException ex) {
        System.err.println("User not found: " + ex.getMessage());
        return Map.of(
                "error", "User not found",
                "message", ex.getMessage()
        );
    }
    @ExceptionHandler({UserNotLoggedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleUserNotLoggedException(UserNotLoggedException ex) {
        System.err.println("User not logged: " + ex.getMessage());
        return Map.of(
                "error","User not logged in",
                "message", ex.getMessage()
        );
    }
    @ExceptionHandler({MoviesNotFound.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public Map<String,String> handleMoviesNotFoundException(MoviesNotFound moviesNotFound) {
        System.err.println("Movies not found: " + moviesNotFound.getMessage());
        return Map.of(
                "error","Movies service is DOWN",
                "message", moviesNotFound.getMessage()
        );
    }
    @ExceptionHandler({MovieNotFoundInDb.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleMoviesNotFoundException(MovieNotFoundInDb moviesNotFound) {
        System.err.println("Movies not found: " + moviesNotFound.getMessage());
        return Map.of(
                "error","Movie not found in DB",
                "message", moviesNotFound.getMessage()
        );
    }
    @ExceptionHandler({FilmNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleFilmNotFoundException(FilmNotFoundException filmNotFoundException) {
        System.err.println("Film not found: " + filmNotFoundException.getMessage());
        return Map.of(
                "error","Film not found",
                "message", filmNotFoundException.getMessage()
        );
    }
    @ExceptionHandler({OmdbException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleOmdbException(OmdbException omdbException) {
        System.err.println("Omdb threw exception: " + omdbException.getMessage());
        return Map.of(
                "error","Omdb threw exception",
                "message", omdbException.getMessage()
        );
    }
    @ExceptionHandler({IlligalRoleException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String,String> handleIlligalRoleException(IlligalRoleException illigalRoleException){
        System.err.println("Illifal role exception: " + illigalRoleException.getMessage());
        return Map.of(
                "error","Illigal role exception",
                "message", illigalRoleException.getMessage()
        );
    }
    @ExceptionHandler({BlockUserException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String,String> handleBlockUserException(BlockUserException blockUserException){
        System.err.println("Block user exception: " + blockUserException.getMessage());
        return Map.of(
                "error","Error blocking user",
                "message", blockUserException.getMessage()
        );
    }
    @ExceptionHandler({SeasonNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleSeasonNotFoundException(SeasonNotFoundException seasonNotFoundException){
        System.err.println("Season not found: " + seasonNotFoundException.getMessage());
        return Map.of(
                "error","Season not found",
                "message", seasonNotFoundException.getMessage()
        );
    }
    @ExceptionHandler({EpisodeFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleEpisodeNotFoundException(EpisodeFoundException episodeFoundException){
        System.err.println("Episode not found: " + episodeFoundException.getMessage());
        return Map.of(
                "error","Episode not found",
                "message", episodeFoundException.getMessage()
        );
    }
    @ExceptionHandler({SerialNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleSerialNotFoundException(SerialNotFoundException serialNotFoundException){
        System.err.println("Serial not found: " + serialNotFoundException.getMessage());
        return Map.of(
                "error","Serial not found",
                "message", serialNotFoundException.getMessage()
        );
    }
    @ExceptionHandler({VideoDirectoryNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleVideoDirectoryNotFoundException(VideoDirectoryNotFoundException videoDirectoryNotFoundException){
        System.err.println("Video directory not found: " + videoDirectoryNotFoundException.getMessage());
        return Map.of(
                "error","VideoDirectory not found",
                "message", videoDirectoryNotFoundException.getMessage()
        );
    }
    @ExceptionHandler({FavoriteNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String,String> handleFavoriteNotFoundException(FavoriteNotFoundException favoriteNotFoundException){
        System.err.println("Favorite not found: " + favoriteNotFoundException.getMessage());
        return Map.of(
                "status","error",
                "error","Favorite not found",
                "message",favoriteNotFoundException.getMessage()
        );
    }
}
