package com.notech.rsocket.rsocketchatserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ConnectionData {

    private String username;
    private DeviceType deviceType;



    public static enum DeviceType {

        MOBILE,
        TABLET,
        COMPUTER
    }


}
