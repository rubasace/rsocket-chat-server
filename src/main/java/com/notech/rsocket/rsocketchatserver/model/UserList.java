package com.notech.rsocket.rsocketchatserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class UserList {

    private List<ConnectionData> connections;

}
