package com.notech.rsocket.rsocketchatserver.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Message {

    private String user;
    private String message;
    private long timestamp;


}
