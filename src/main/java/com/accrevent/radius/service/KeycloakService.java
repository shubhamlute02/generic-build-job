package com.accrevent.radius.service;

import com.accrevent.radius.model.UserLogout;
import com.accrevent.radius.repository.UserLogoutRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeycloakService {
    private final Keycloak keycloak;
    private final UserLogoutRepository userLogoutRepository;
    @Value("${spring.keycloak.realm}")
    private String realm;
    public KeycloakService(Keycloak keycloak, UserLogoutRepository userLogoutRepository) {
        this.keycloak = keycloak;
        this.userLogoutRepository = userLogoutRepository;
    }

    // Find userId by username (fetch from Keycloak)
    public Optional<String> getUserIdByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users().search(username, 0, 1);
        if (users != null && !users.isEmpty()) {
            return Optional.of(users.get(0).getId());
        }
        return Optional.empty();
    }

    // Call Keycloak logout & store logout time persistently
    public void logoutUserByUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Invalidate sessions in Keycloak
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        userResource.logout();

        // Save logout time in DB
        Instant now = Instant.now();

        UserLogout userLogout = userLogoutRepository.findByUserId(userId)
                .orElse(new UserLogout(userId, now));
        userLogout.setLogoutTime(now);
        userLogoutRepository.save(userLogout);
    }

    // Get last logout time from DB for unread comment logic
    public Optional<Instant> getLastLogoutTime(String userId) {
        return userLogoutRepository.findByUserId(userId).map(UserLogout::getLogoutTime);
    }

    public List<Map<String, Object>> getAllUsers() {
        RealmResource realmResource = keycloak.realm(realm);

        // Fetch all users
        List<UserRepresentation> users = realmResource.users().list();

        List<Map<String, Object>> userInfoList = new ArrayList<>();

        for (UserRepresentation user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("email", user.getEmail());
            userInfo.put("enabled", user.isEnabled());

            // Fetch roles for this user
            UserResource userResource = realmResource.users().get(user.getId());
            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
            List<String> roleNames = realmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());

            userInfo.put("roles", roleNames);

            // (Optional) Add attributes if present
            userInfo.put("attributes", user.getAttributes());

            userInfoList.add(userInfo);
        }

        return userInfoList;
    }



}
