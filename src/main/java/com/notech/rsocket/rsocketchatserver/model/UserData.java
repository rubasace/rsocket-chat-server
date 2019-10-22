package com.notech.rsocket.rsocketchatserver.model;

public class UserData {

    private String userId;

    public UserData() {
    }

    public UserData(final String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }
}
