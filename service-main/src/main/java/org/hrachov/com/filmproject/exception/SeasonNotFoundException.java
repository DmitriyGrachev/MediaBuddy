package org.hrachov.com.filmproject.exception;

public class SeasonNotFoundException extends RuntimeException{
    public SeasonNotFoundException(Long seasonId){
        super("The season with id " + seasonId + " was not found.");
    }
}
