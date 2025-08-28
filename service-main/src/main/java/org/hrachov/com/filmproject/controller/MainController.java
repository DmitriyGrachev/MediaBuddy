package org.hrachov.com.filmproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
//TODO

@Controller
public class MainController {
    @GetMapping("/")
    public String index() {
        return "index"; // Renders templates/index.html
    }

    @GetMapping("/movies")
    public String moviesPage() {
        return "films/moviesPage";
    }
    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }
    @GetMapping("/movie/{id}")
    public String moviePage(@PathVariable int id) {
        return "films/moviePage";
    }

    @GetMapping("/movies/search")
    public String movieSearchPage() {
        return "films/moviesSearchPage";
    }

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }
    @GetMapping("/serial/{id}")
    public String serialPage(@PathVariable int id) {
        return "serial/serial_player";
    }
    @GetMapping("/videoDirectories")
    public String videoDirectoriesPage() {
        return "videosAll";
    }

    @GetMapping("/videos/{id}")
    public String videoDirectoriesPage(@PathVariable int id) {
        return "videosInRepo";
    }
    //TODO
    @GetMapping("/authfront/login")
    public String login(){
        return "login";
    }
    @GetMapping("/authfront/logout")
    public String logout() {
        return "logout";
    }
    @GetMapping("/authfront/register")
    public String register() {
        return "register";
    }
    @GetMapping("/upload")
    public String upload() {
        return "upload_video";
    }
    @GetMapping("/admin_dashboard")
    public String adminDashboard() {
        return "admin_dashboard";
    }
    @GetMapping("/email_form")
    public String emailForm() {
        return "email_form";
    }
    @GetMapping("/reset_password_form")
    public String resetPasswordForm(@RequestParam String email) {
        return "reset_password_form";
    }
    @GetMapping("chats")
    public String chatsPage() {
        return "chats";
    }
    @GetMapping("/oauth2/success")
    public String oauth2SuccessPage() {
        return "oauth2-success";
    }

}
