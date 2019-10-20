package com.notech.rsocket.rsocketchatserver.model;

public class Message {

    private String user;
    private String message;
    private long timestamp;

    public Message() {

    }

    public Message(final String message, final long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }
}
