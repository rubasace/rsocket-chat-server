package com.notech.rsocket.rsocketchatserver.controller;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.DefaultPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Controller
public class ConnectionController implements SocketAcceptor {

    @Autowired
    private ChatService chatService;


    @Override
    public Mono<RSocket> accept(final ConnectionSetupPayload setup, final RSocket sendingSocket) {

        String username = setup.getDataUtf8();
        String userId = UUID.randomUUID().toString();

        chatService.connect(username, userId);

        System.out.println(username + " Connected: " + userId);

        sendingSocket.fireAndForget(DefaultPayload.create(userId))
                     //                     .subscribeOn(Schedulers.single())
                     .subscribeOn(Schedulers.single())
                     .subscribe();

        sendingSocket.onClose()
                     .onErrorResume(e -> Mono.empty())
                     .then(chatService.disconnect(userId))
                     .subscribeOn(Schedulers.single())
                     .subscribe();

        return Mono.just(chatService);
    }

}
