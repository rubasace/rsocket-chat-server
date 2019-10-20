package com.notech.rsocket.rsocketchatserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notech.rsocket.rsocketchatserver.model.Message;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
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

    private final DirectProcessor<String> notificationsProcessor = DirectProcessor.create();
    private final Flux<Payload> notificationsFlux = notificationsProcessor.map(DefaultPayload::create);


    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {

        Flux.from(payloads)
            .map(this::toMessage)
            .doOnNext(messagesProcessor::onNext)
            .subscribe();

        return messagesFlux;
    }

    @Override
    public Flux<Payload> requestStream(final Payload payload) {
        return notificationsFlux;
    }


    private Message toMessage(final Payload payload) {
        try {
            String dataUtf8 = payload.getDataUtf8();
            Message message = objectMapper.readValue(dataUtf8, Message.class);
            message.setUser(usersMap.getOrDefault(message.getUser(), "Unknown"));
            return message;
        } catch (IOException e) {
            e.printStackTrace();
            return new Message();
        }
    }

    private Payload toPayload(final Message message) {
        try {
            return DefaultPayload.create(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    Mono<Void> disconnect(final String userId) {
        return Mono.just(userId)
                   .map(usersMap::remove)
                   .doOnNext(username -> notificationsProcessor.onNext(username + " disconnected"))
                   .subscribeOn(Schedulers.single())
                   .then();
    }

    void connect(final String username, final String userId) {
        notificationsProcessor.onNext(username + " is now connected");
        usersMap.put(userId, username);
    }
}
