package com.notech.rsocket.rsocketchatserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notech.rsocket.rsocketchatserver.model.Message;
import com.notech.rsocket.rsocketchatserver.model.Notification;
import com.notech.rsocket.rsocketchatserver.model.UserData;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService extends AbstractRSocket {

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, String> usersMap = new HashMap<>();

    private final DirectProcessor<Message> messagesProcessor = DirectProcessor.create();
    private final Flux<Payload> messagesFlux = messagesProcessor.map(this::toPayload)
                                                                .cache(5)
                                                                .onBackpressureBuffer();

    private final DirectProcessor<Notification> notificationsProcessor = DirectProcessor.create();
    private final Flux<Payload> notificationsFlux = notificationsProcessor.map(this::toPayload);


    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {

        Flux.from(payloads)
            .map(payload -> toObject(payload, Message.class))
            .doOnNext(messagesProcessor::onNext)
            .subscribe();

        return messagesFlux;
    }

    @Override
    public Flux<Payload> requestStream(final Payload payload) {
        return notificationsFlux;
    }


    private <T> T toObject(final Payload payload, final Class<T> clazz) {
        try {
            return objectMapper.readValue(payload.getDataUtf8(), clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Payload toPayload(final Object object) {
        try {
            return DefaultPayload.create(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    Mono<Void> disconnect(final String userId) {
        return Mono.just(userId)
                   .map(usersMap::remove)
                   .doOnNext(username -> notificationsProcessor.onNext(new Notification(username + " disconnected")))
                   .subscribeOn(Schedulers.single())
                   .then();
    }

    void connect(final String username, final String userId, final RSocket sendingSocket) {
        sendingSocket.fireAndForget(toPayload(new UserData(userId)))
                     //                     .subscribeOn(Schedulers.single())
                     .subscribeOn(Schedulers.single())
                     .subscribe();
        notificationsProcessor.onNext(new Notification(username + " is now connected"));
        usersMap.put(userId, username);
    }
}
