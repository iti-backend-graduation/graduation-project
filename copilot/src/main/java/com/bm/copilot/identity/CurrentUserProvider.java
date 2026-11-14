package com.bm.copilot.identity;

/** Public entry point other modules use to find out who is making the current request. */
public interface CurrentUserProvider {

    /**
     * Returns the logged-in user.
     *
     * @throws org.springframework.security.access.AccessDeniedException if nobody is authenticated
     */
    CurrentUser require();
}