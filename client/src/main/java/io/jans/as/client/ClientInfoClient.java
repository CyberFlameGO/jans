/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.AuthorizationMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;

/**
 * Encapsulates functionality to make client info request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public class ClientInfoClient extends BaseClient<ClientInfoRequest, ClientInfoResponse> {

    private static final Logger LOG = Logger.getLogger(ClientInfoClient.class);

    /**
     * Constructs an Client Info client by providing a REST url where the service is located.
     *
     * @param url The REST Service location.
     */
    public ClientInfoClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        if (getRequest().getAuthorizationMethod() == null
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            return HttpMethod.POST;
        } else { // AuthorizationMethod.URL_QUERY_PARAMETER
            return HttpMethod.GET;
        }
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @param accessToken The access token obtained from the Jans Auth authorization request.
     * @return The service response.
     */
    public ClientInfoResponse execClientInfo(String accessToken) {
        setRequest(new ClientInfoRequest(accessToken));

        return exec();
    }

    public ClientInfoResponse exec() {
        initClientRequest();
        return execInternal();
    }


    @Deprecated
    public ClientInfoResponse exec(ClientHttpEngine engine) {
    	resteasyClient = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
		webTarget = resteasyClient.target(getUrl());

		return execInternal();
    }


    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The service response.
     */
    private ClientInfoResponse execInternal() {
        // Prepare request parameters

        Builder clientRequest = prepareAuthorizatedClientRequest();

        // Call REST Service and handle response
        try {
            if (getRequest().getAuthorizationMethod() == null
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD
                    || getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
                clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();
            } else {  //AuthorizationMethod.URL_QUERY_PARAMETER
                clientResponse = clientRequest.buildGet().invoke();
            }

            setResponse(new ClientInfoResponse(clientResponse));

            String entity = clientResponse.readEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            parseEntity(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

	private Builder prepareAuthorizatedClientRequest() {
		Builder clientRequest = null;
        if (getRequest().getAuthorizationMethod() == null
                || getRequest().getAuthorizationMethod() == AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
            	clientRequest = webTarget.request();
                clientRequest.header("Authorization", "Bearer " + getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER) {
            if (StringUtils.isNotBlank(getRequest().getAccessToken())) {
                requestForm.param("access_token", getRequest().getAccessToken());
            }
        } else if (getRequest().getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER && StringUtils.isNotBlank(getRequest().getAccessToken())) {
            addReqParam("access_token", getRequest().getAccessToken());
        }

        if (clientRequest == null) {
        	clientRequest = webTarget.request();
        }

        clientRequest.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
		return clientRequest;
	}

    private void parseEntity(String entity) {
        if (StringUtils.isBlank(entity)) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject(entity);

            for (Iterator<String> iterator = jsonObj.keys(); iterator.hasNext(); ) {
                String key = iterator.next();
                getResponse().getClaimMap().put(key, ClientUtil.extractListByKeyOptString(jsonObj, key));
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}