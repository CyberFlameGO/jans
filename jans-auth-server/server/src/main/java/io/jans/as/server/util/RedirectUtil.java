/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.util;

import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.config.Constants;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.net.MalformedURLException;
import java.net.URI;

import static io.jans.as.client.AuthorizationRequest.NO_REDIRECT_HEADER;

/**
 * @version July 28, 2021
 */
public class RedirectUtil {

    private final static Logger log = LoggerFactory.getLogger(RedirectUtil.class);

    static String JSON_REDIRECT_PROPNAME = "redirect";

    static int HTTP_REDIRECT = 302;

    public static ResponseBuilder getRedirectResponseBuilder(RedirectUri redirectUriResponse, HttpServletRequest httpRequest) {
        ResponseBuilder builder;

        if (httpRequest != null && httpRequest.getHeader(NO_REDIRECT_HEADER) != null) {
            try {
                URI redirectURI = URI.create(redirectUriResponse.toString());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_REDIRECT_PROPNAME, redirectURI.toURL());
                String jsonResp = jsonObject.toString();
                jsonResp = jsonResp.replace("\\/", "/");
                builder = Response.ok(
                        new GenericEntity<String>(jsonResp, String.class),
                        MediaType.APPLICATION_JSON_TYPE
                );

            } catch (MalformedURLException e) {
                builder = Response.serverError();
                log.debug(e.getMessage(), e);
            } catch (JSONException e) {
                builder = Response.serverError();
                log.debug(e.getMessage(), e);
            }
        } else if (redirectUriResponse.getResponseMode() == ResponseMode.FORM_POST
                || redirectUriResponse.getResponseMode() == ResponseMode.FORM_POST_JWT) {
            builder = new ResponseBuilderImpl();
            builder.status(Response.Status.OK);
            builder.type(MediaType.TEXT_HTML_TYPE);
            builder.cacheControl(CacheControl.valueOf("no-cache, no-store"));
            builder.header(Constants.PRAGMA, Constants.NO_CACHE);
            builder.entity(redirectUriResponse.toString());
        } else {
            URI redirectURI = URI.create(redirectUriResponse.toString());
            builder = Response.status(HTTP_REDIRECT);
            builder.location(redirectURI);
        }

        return builder;
    }
}
