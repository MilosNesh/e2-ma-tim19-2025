package com.example.habitgame.model;

import com.google.type.DateTime;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.google.firebase.Timestamp;

public class Message {
    private String id;
    private String text;
    private String authorUsername;
    private String authorEmail;
    private Timestamp  date;
    private String allianceId;
    public Message() {
    }
    public Message(String text, String authorUsername, String authorEmail, Timestamp date, String allianceId) {
        this.text = text;
        this.authorUsername = authorUsername;
        this.authorEmail = authorEmail;
        this.date = date;
        this.allianceId = allianceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public Timestamp  getDate() {
        return date;
    }

    public void setDate(Timestamp  date) {
        this.date = date;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public LocalDateTime getDateAsLocalDateTime() {
        if (date != null) {
            return date.toDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }
}
