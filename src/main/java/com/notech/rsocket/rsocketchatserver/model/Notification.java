package com.notech.rsocket.rsocketchatserver.model;

public class Notification {

    private String message;

    public Notification() {
    }

    public Notification(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}
