package com.love.website;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "love_shares")
public class LoveShare {

    @Id
    @Column(length = 43)
    private String token;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false, length = 24)
    private String passwordSalt;

    @Column(nullable = false, length = 44)
    private String passwordHash;

    protected LoveShare() {
    }

    public LoveShare(String token, Long messageId, String passwordSalt, String passwordHash) {
        this.token = token;
        this.messageId = messageId;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    public String getToken() {
        return token;
    }

    public Long getMessageId() {
        return messageId;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
