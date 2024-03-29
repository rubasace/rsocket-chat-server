package com.notech.rsocket.rsocketchatserver.config;

import com.notech.rsocket.rsocketchatserver.controller.ConnectionController;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
class ServerConfig {

    private static final int RSOCKET_PORT = 7000;

    @Bean
    CloseableChannel closableChannel(final ConnectionController connectionController) throws CertificateException, NoSuchAlgorithmException {
        return RSocketFactory.receive()
                             .frameDecoder(PayloadDecoder.ZERO_COPY)
                             .acceptor(connectionController)
                             .transport(WebsocketServerTransport.create(RSOCKET_PORT))
                             .start()
                             .block();
    }
}