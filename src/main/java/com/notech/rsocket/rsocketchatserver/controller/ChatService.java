package com.notech.rsocket.rsocketchatserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notech.rsocket.rsocketchatserver.model.ConnectionData;
import com.notech.rsocket.rsocketchatserver.model.Message;
import com.notech.rsocket.rsocketchatserver.model.UserData;
import com.notech.rsocket.rsocketchatserver.model.UserList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService extends AbstractRSocket {

    @Autowired
    private ObjectMapper objectMapper;
    private final Map<String, ConnectionData> connectionsMap = new HashMap<>();

    private final DirectProcessor<Message> messagesProcessor = DirectProcessor.create();
    private final Flux<Payload> messagesFlux = messagesProcessor.map(this::toPayload)
                                                                .cache(100);

    private final DirectProcessor<UserList> notificationsProcessor = DirectProcessor.create();
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

    void connect(final ConnectionData connectionData, final String userId, final RSocket sendingSocket) {
        Mono.just(connectionData)
            .doOnNext(data -> connectionsMap.put(userId, connectionData))
            .doOnNext(data -> notifyUserList())
            .flatMap(data -> sendingSocket.fireAndForget(toPayload(new UserData(userId))))
            .subscribeOn(Schedulers.single())
            .subscribe();
    }

    Mono<Void> disconnect(final String userId) {
        return Mono.just(userId)
                   .doOnNext(connectionsMap::remove)
                   .doOnNext(username -> notifyUserList())
                   .subscribeOn(Schedulers.single())
                   .then();
    }


    private Message toMessage(final Payload payload) {
        Message message = toObject(payload, Message.class);

        message.setUser(Optional.ofNullable(connectionsMap.get(message.getUser())).map(ConnectionData::getUsername).orElse("Anonymous"));
        return message;
    }

    private <T> T toObject(final Payload payload, final Class<T> clazz) {
        try {
            String dataUtf8 = payload.getDataUtf8();
            payload.release();
            return objectMapper.readValue(dataUtf8, clazz);
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


    private void notifyUserList() {
        notificationsProcessor.onNext(new UserList(new ArrayList<>((connectionsMap.values()))));
    }


}
