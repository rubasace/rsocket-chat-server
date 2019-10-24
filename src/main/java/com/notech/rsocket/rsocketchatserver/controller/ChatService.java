package com.notech.rsocket.rsocketchatserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notech.rsocket.rsocketchatserver.model.Message;
import com.notech.rsocket.rsocketchatserver.model.UserList;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Service
public class ChatService extends AbstractRSocket {

    @Autowired
    private ObjectMapper objectMapper;
    //TODO play with schedulers so no race conditions during demo :)
    private final LinkedHashMap<String, String> usersMap = new LinkedHashMap<>();

    private final DirectProcessor<Message> messagesProcessor = DirectProcessor.create();
    private final Flux<Payload> messagesFlux = messagesProcessor.map(this::toPayload)
                                                                .cache(100)
                                                                .onBackpressureBuffer();

    private final DirectProcessor<UserList> notificationsProcessor = DirectProcessor.create();
    //TODO fix no notifications sent first time connected, until other connection
    private final Flux<Payload> notificationsFlux = notificationsProcessor.map(this::toPayload).cache(1);


    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {

        Flux.from(payloads)
            .map(this::toMessage)
            .doOnNext(messagesProcessor::onNext)
            .doOnNext(System.out::println)
            .subscribe();

        return messagesFlux;
    }

    @Override
    public Flux<Payload> requestStream(final Payload payload) {
        return notificationsFlux;
    }

    private Message toMessage(final Payload payload) {
        Message message = toObject(payload, Message.class);
        message.setUser(usersMap.getOrDefault(message.getUser(), "Unknown"));
        return message;
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
                   .doOnNext(usersMap::remove)
                   .doOnNext(username -> notifyUserList())
                   .subscribeOn(Schedulers.single())
                   .then();
    }

    private void notifyUserList() {
        notificationsProcessor.onNext(new UserList(new LinkedList<>(usersMap.values())));
    }

    void connect(final String username, final String userId, final RSocket sendingSocket) {
        sendingSocket.fireAndForget(toPayload(new UserData(userId)))
                     //                     .subscribeOn(Schedulers.single())
                     .subscribeOn(Schedulers.single())
                     .subscribe();
        usersMap.put(userId, username);
        notifyUserList();
    }
}
