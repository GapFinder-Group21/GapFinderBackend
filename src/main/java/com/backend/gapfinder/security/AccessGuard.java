package com.backend.gapfinder.security;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.UserModel;

// Checks that the authenticated user (from the JWT) is the owner of the data in the request
public final class AccessGuard {

    private AccessGuard() {
    }

    // The user id sent by the client must be the same as the authenticated user
    public static void requireSameUser(UserModel authenticatedUser, Long userId) {
        if (authenticatedUser == null || userId == null || !authenticatedUser.getId().equals(userId)) {
            throw new ForbiddenException("No tienes permiso para actuar en nombre de otro usuario");
        }
    }
}
