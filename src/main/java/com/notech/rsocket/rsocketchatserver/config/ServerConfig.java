package com.notech.rsocket.rsocketchatserver.config;

import com.notech.rsocket.rsocketchatserver.controller.ConnectionController;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.HttpServer;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

@Configuration
class ServerConfig {

    private static final int RSOCKET_PORT = 7000;

    @Bean
    CloseableChannel closableChannel(final ConnectionController connectionController) throws CertificateException, NoSuchAlgorithmException {
        SecureRandom random =SecureRandom.getInstanceStrong();
        SelfSignedCertificate certificate = new SelfSignedCertificate("nasvigo.com", random, 1024);
        return RSocketFactory.receive()
                             //                             .resume()
                             //                             .resumeSessionDuration(Duration.ofMinutes(5))
                             .frameDecoder(PayloadDecoder.ZERO_COPY)
                             .acceptor(connectionController)
                             .transport(WebsocketServerTransport.create(HttpServer.create()
                                                                                  .port(RSOCKET_PORT)
                                                                                  .secure(sslContextSpec -> sslContextSpec.sslContext(
                                                                                          SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey())
                                                                                                           .trustManager(InsecureTrustManagerFactory.INSTANCE)))))

                             .start()
                             .block();
    }
}