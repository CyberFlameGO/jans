/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.token.ClientAssertionType;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;


/**
 * @author Yuriy Zabrovarnyy
 */
public class ClientAuthnEnabler {

    private final Builder clientRequest;
    private final Form requestForm;

    public ClientAuthnEnabler(Builder clientRequest, Form requestForm) {
        this.clientRequest = clientRequest;
        this.requestForm = requestForm;
    }

    public void exec(ClientAuthnRequest request) {
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC
                && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
            return;
        }

        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
                requestForm.param("client_id", request.getAuthUsername());
            }
            if (request.getAuthPassword() != null && !request.getAuthPassword().isEmpty()) {
                requestForm.param("client_secret", request.getAuthPassword());
            }
            return;
        }
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                request.getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            requestForm.param("client_assertion_type", ClientAssertionType.JWT_BEARER.toString());
            if (request.getClientAssertion() != null) {
                requestForm.param("client_assertion", request.getClientAssertion());
            }
            if (request.getAuthUsername() != null && !request.getAuthUsername().isEmpty()) {
                requestForm.param("client_id", request.getAuthUsername());
            }
        }
    }
}
