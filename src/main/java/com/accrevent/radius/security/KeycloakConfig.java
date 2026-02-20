package com.accrevent.radius.security;

import ch.qos.logback.core.net.SyslogOutputStream;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Value("${spring.keycloak.realm}")
    private String realm;
    @Value("${radius.keycloak.serverURL}")
    private String serverURL;
    @Value("${radius.keycloak.clientId}")
    private  String clientId;
    @Value("${radius.keycloak.username}")
    private  String username;
    @Value("${radius.keycloak.password}")
    private  String password;

    @Bean
    public Keycloak keycloak() {

        System.out.println("serverURL = " + serverURL);
        System.out.println("clientId = " + clientId);
        System.out.println("username = " + username);
        System.out.println("password = " + password);

        return KeycloakBuilder.builder()
                .serverUrl(serverURL) // Change to your Keycloak server URL
                .realm(realm) // Replace with your Keycloak realm
                .clientId(clientId) // The client ID you created for admin access
                .username(username) // Replace with your Keycloak admin username
                .password(password) // Replace with your Keycloak admin password
                .grantType("password")
                .build();

    }
}