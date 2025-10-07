package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.models.User;
import com.Zone01.lets_play.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Autowired
    public HomeController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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

    @GetMapping("/products")
    public String products(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            User current = userRepository.findByEmail(email);
            if (current != null) {
                model.addAttribute("currentUser", current);
            }
        }
        return "products";
    }

    @GetMapping("/web/token")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getTokenForWebUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String token = jwtService.generateToken(user);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
}
