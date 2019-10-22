package com.notech.rsocket.rsocketchatserver.model;

import java.util.List;

public class UserList {

    private List<String> usernames;

    public UserList() {
    }

    public UserList(final List<String> usernames) {
        this.usernames = usernames;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(final List<String> usernames) {
        this.usernames = usernames;
    }
}
