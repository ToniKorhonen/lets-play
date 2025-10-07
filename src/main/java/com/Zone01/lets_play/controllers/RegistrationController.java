package com.Zone01.lets_play.controllers;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.models.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class RegistrationController { // renommé depuis authController pour éviter conflit de bean avec AuthController REST
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        log.info("[REGISTER] Incoming registration attempt: email='{}'", user != null ? user.getEmail() : "null");

        if (result.hasErrors()) {
            log.warn("[REGISTER] Validation errors for email='{}': {}", user.getEmail(), result.getAllErrors());
            model.addAttribute("errorMessage", "Please fix the validation errors below.");
            return "register";
        }

        try {
            if (userRepository.findByEmail(user.getEmail()) != null) {
                log.warn("[REGISTER] Email already exists (pre-check) : {}", user.getEmail());
                result.rejectValue("email", "email.exists", "Email already in use");
                model.addAttribute("errorMessage", "This email is already registered.");
                return "register";
            }

            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("USER");
                log.debug("[REGISTER] No role provided. Defaulting to USER.");
            }

            String rawPassword = user.getPassword();
            if (rawPassword == null) {
                log.warn("[REGISTER] Raw password is null for email='{}'", user.getEmail());
                result.rejectValue("password", "password.null", "Password is required");
                model.addAttribute("errorMessage", "Password is required.");
                return "register";
            }
            log.debug("[REGISTER] Raw password length for email='{}': {}", user.getEmail(), rawPassword.length());

            user.setPassword(passwordEncoder.encode(rawPassword));
            log.debug("[REGISTER] Password encoded for email='{}'", user.getEmail());

            userRepository.save(user);
            log.info("[REGISTER] User successfully saved to MongoDB: id={}, email={}", user.getId(), user.getEmail());
            return "redirect:/login?registered";
        } catch (DuplicateKeyException dk) {
            // Handles race condition with unique index or direct duplicate insertion
            log.warn("[REGISTER] DuplicateKeyException for email='{}' (unique index)", user.getEmail());
            result.rejectValue("email", "email.exists", "Email already in use");
            model.addAttribute("errorMessage", "This email is already registered.");
            return "register";
        } catch (Exception e) {
            log.error("[REGISTER] Exception while registering email='{}'", user.getEmail(), e);
            model.addAttribute("errorMessage", "Internal error occurred. Try again later.");
            return "register";
        }
    }
}