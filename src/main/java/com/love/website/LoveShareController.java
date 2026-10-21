package com.love.website;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/shares")
public class LoveShareController {

    private static final String SESSION_ATTRIBUTE_PREFIX = "unlocked-share:";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final LoveMessageRepository messageRepository;
    private final LoveShareRepository shareRepository;
    private final SharePasswordService passwordService;
    private final String configuredShareBaseUrl;

    public LoveShareController(
            LoveMessageRepository messageRepository,
            LoveShareRepository shareRepository,
            SharePasswordService passwordService,
            @Value("${app.share-base-url:}") String configuredShareBaseUrl) {
        this.messageRepository = messageRepository;
        this.shareRepository = shareRepository;
        this.passwordService = passwordService;
        this.configuredShareBaseUrl = configuredShareBaseUrl;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createShare(
            @RequestBody CreateShareRequest request,
            HttpServletRequest servletRequest) {
        if (request.senderName() == null || request.message() == null || request.password() == null
                || request.senderName().isBlank() || request.message().isBlank()
                || request.password().length() < 8
                || request.senderName().length() > 50 || request.message().length() > 1000
                || request.password().length() > 128) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Enter a name, message, and password between 8 and 128 characters."));
        }

        LoveMessage message = messageRepository.save(
                new LoveMessage(request.senderName().trim(), request.message().trim(), true));
        SharePasswordService.HashedPassword password = passwordService.hash(request.password());
        String token = createToken();
        shareRepository.save(new LoveShare(token, message.getId(), password.salt(), password.hash()));

        String shareUrl = getShareBaseUrl(servletRequest) + "/share/" + token;
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShareCreatedResponse(token, shareUrl));
    }

    @PostMapping("/{token}/unlock")
    public ResponseEntity<?> unlock(
            @PathVariable String token,
            @RequestBody UnlockRequest request,
            HttpServletRequest servletRequest) {
        Optional<LoveShare> share = findShare(token);
        if (share.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (request.password() == null || request.password().length() > 128
                || !passwordService.matches(
                        request.password(), share.get().getPasswordSalt(), share.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "That password is incorrect."));
        }

        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(SESSION_ATTRIBUTE_PREFIX + token, true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getSharedMessage(@PathVariable String token, HttpServletRequest request) {
        Optional<LoveShare> share = findShare(token);
        if (share.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(
                session.getAttribute(SESSION_ATTRIBUTE_PREFIX + token))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Enter the password to open this link."));
        }

        return messageRepository.findById(share.get().getMessageId())
                .<ResponseEntity<?>>map(message -> ResponseEntity.ok(new SharedMessageResponse(
                        message.getSenderName(), message.getMessage(), message.getCreatedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Optional<LoveShare> findShare(String token) {
        if (!TOKEN_PATTERN.matcher(token).matches()) {
            return Optional.empty();
        }
        return shareRepository.findById(token);
    }

    private String createToken() {
        byte[] tokenBytes = new byte[32];
        TOKEN_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String getShareBaseUrl(HttpServletRequest request) {
        if (!configuredShareBaseUrl.isBlank()) {
            return configuredShareBaseUrl.replaceAll("/+$", "");
        }

        String scheme = request.isSecure() ? "https" : "http";
        String host = request.getServerName();
        if (!isLocalHost(host)) {
            return formatBaseUrl(scheme, host, request.getServerPort());
        }

        return findNetworkAddress()
                .map(address -> formatBaseUrl(scheme, address, request.getServerPort()))
                .orElseThrow(() -> new IllegalStateException(
                        "Could not determine a network address for share links. "
                                + "Set app.share-base-url in application.properties."));
    }

    private boolean isLocalHost(String host) {
        return host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1")
                || host.equals("0:0:0:0:0:0:0:1");
    }

    private Optional<String> findNetworkAddress() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(networkInterface -> {
                        try {
                            return networkInterface.isUp() && !networkInterface.isLoopback();
                        } catch (SocketException exception) {
                            throw new IllegalStateException("Could not inspect network interfaces.", exception);
                        }
                    })
                    .sorted(Comparator.comparing(
                            networkInterface -> !networkInterface.getDisplayName()
                                    .matches("(?i).*(wi-?fi|wireless|ethernet).*")))
                    .flatMap(networkInterface ->
                            Collections.list(networkInterface.getInetAddresses()).stream())
                    .filter(Inet4Address.class::isInstance)
                    .filter(address -> !address.isLoopbackAddress() && !address.isLinkLocalAddress())
                    .map(InetAddress::getHostAddress)
                    .findFirst();
        } catch (SocketException exception) {
            throw new IllegalStateException("Could not inspect network interfaces.", exception);
        }
    }

    private String formatBaseUrl(String scheme, String host, int port) {
        boolean standardPort = (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);
        return scheme + "://" + host + (standardPort ? "" : ":" + port);
    }

    public record CreateShareRequest(String senderName, String message, String password) {
    }

    public record UnlockRequest(String password) {
    }

    public record ShareCreatedResponse(String token, String shareUrl) {
    }

    public record SharedMessageResponse(String senderName, String message, LocalDateTime createdAt) {
    }
}
