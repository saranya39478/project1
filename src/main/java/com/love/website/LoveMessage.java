
package com.love.website;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

@Entity
@Table(name = "love_messages")
public class LoveMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String senderName;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean privateShare;

    private LocalDateTime createdAt;

    public LoveMessage() {
    }

    public LoveMessage(String senderName, String message) {
        this.senderName = senderName;
        this.message = message;
    }

    public LoveMessage(String senderName, String message, boolean privateShare) {
        this.senderName = senderName;
        this.message = message;
        this.privateShare = privateShare;
    }

    @PrePersist
    public void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isPrivateShare() {
        return privateShare;
    }
}
