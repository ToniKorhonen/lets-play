package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.models.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName(); // usernameParameter set to email
            User current = userRepository.findByEmail(email);
            if (current != null) {
                model.addAttribute("currentUser", current);
            } else {
                // Fallback minimal info if somehow user not found
                model.addAttribute("currentUserEmail", email);
            }
        }
        return "index";
    }
}
