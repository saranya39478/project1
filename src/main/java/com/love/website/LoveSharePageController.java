package com.love.website;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

@Controller
public class LoveSharePageController {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final LoveShareRepository shareRepository;

    public LoveSharePageController(LoveShareRepository shareRepository) {
        this.shareRepository = shareRepository;
    }

    @GetMapping("/share/{token}")
    public String openShare(@PathVariable String token) {
        if (!TOKEN_PATTERN.matcher(token).matches() || !shareRepository.existsById(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
