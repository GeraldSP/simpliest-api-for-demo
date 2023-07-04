package dev.snowdrop.example.service;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Date;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;

@Path("/a")
@ApplicationScope
@Component
@Produces(MediaType.TEXT_PLAIN)
public class RestSample {

    @Value("${prueba.endpointConSSL}")
    private String endpoint;

    @Value("${prueba.trust-store}")
    private String truststorePath;

    @Value("${prueba.trust-store-pass}")
    private String truststorePass;

    @GET
    @Path("/{text}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String saySomething(@PathParam("text") String text) throws Exception {
        System.out.println("Message Received!");
        System.out.println("Calling endpoint: "+ endpoint );
        System.out.println("using trustore at: "+ truststorePath );
        String result = consume();
        return String.format("result from service %s", result);
    }

    public String consume() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(new File(truststorePath)),
                truststorePass.toCharArray());
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                new SSLContextBuilder()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .loadKeyMaterial(keyStore, "password".toCharArray()).build());
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        ResponseEntity<String> response = restTemplate.getForEntity(
                endpoint, String.class);
        return response.getBody();
    }

}