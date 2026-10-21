
package com.love.website;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class LoveMessageController {

    private final LoveMessageRepository repository;

    public LoveMessageController(LoveMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<LoveMessage> getMessages() {
        return repository.findAllByPrivateShareFalseOrderByIdDesc();
    }

    @PostMapping
    public ResponseEntity<?> saveMessage(
            @RequestBody Map<String, String> body) {

        String senderName = body.get("senderName");
        String message = body.get("message");

        if (senderName == null || message == null
                || senderName.isBlank() || message.isBlank()
                || senderName.length() > 50
                || message.length() > 1000) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please enter valid details."));
        }

        LoveMessage saved = repository.save(
                new LoveMessage(senderName.trim(), message.trim()));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
