package org.hrachov.com.filmproject.exception;

public class IlligalRoleException extends IllegalArgumentException{
    public IlligalRoleException(String role) {
        super("The role " + role + " is not allowed to perform this action.");
    }
    public IlligalRoleException() {
        super();
    }
}
