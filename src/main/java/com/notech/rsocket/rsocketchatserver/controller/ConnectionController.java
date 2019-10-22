package com.notech.rsocket.rsocketchatserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notech.rsocket.rsocketchatserver.model.UserData;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.DefaultPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.UUID;

@Controller
public class ConnectionController implements SocketAcceptor {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public Mono<RSocket> accept(final ConnectionSetupPayload setup, final RSocket sendingSocket) {

        String username = toObject(setup, UserData.class).getUserId();
        String userId = UUID.randomUUID().toString();

        chatService.connect(username, userId, sendingSocket);

        System.out.println(username + " Connected: " + userId);

        sendingSocket.onClose()
                     .onErrorResume(e -> Mono.empty())
                     .then(chatService.disconnect(userId))
                     .subscribeOn(Schedulers.single())
                     .subscribe();

        return Mono.just(chatService);
    }

    private <T> T toObject(final Payload payload, final Class<T> clazz) {
        try {
            return objectMapper.readValue(payload.getDataUtf8(), clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
