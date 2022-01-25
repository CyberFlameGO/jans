/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import java.io.IOException;
import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.util.StringHelper;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.init.Initializable;
import org.slf4j.Logger;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;


@ApplicationScoped
@Named("openIdConfigService")
public class OpenIdConfigService extends Initializable implements Serializable {

    private static final long serialVersionUID = 7875838160379126796L;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    private OpenIdConfigurationResponse openIdConfiguration;

    @Override
    protected void initInternal() {
        try {
            loadOpenIdConfiguration();
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to load oxAuth configuration");
        }
    }

    private void loadOpenIdConfiguration() throws IOException {
        String openIdProvider = appConfiguration.getIssuer();
        if (StringHelper.isEmpty(openIdProvider)) {
            throw new ConfigurationException("OpenIdProvider Url is invalid");
        }

        openIdProvider = openIdProvider + "/.well-known/openid-configuration";

        final OpenIdConfigurationClient openIdConfigurationClient = new OpenIdConfigurationClient(openIdProvider);
        final OpenIdConfigurationResponse response = openIdConfigurationClient.execOpenIdConfiguration();
        if ((response == null) || (response.getStatus() != 200)) {
            throw new ConfigurationException("Failed to load oxAuth configuration");
        }

        log.info("Successfully loaded oxAuth configuration");

        this.openIdConfiguration = response;
    }

    public OpenIdConfigurationResponse getOpenIdConfiguration() {
        // Call each time to allows retry
        init();

        return openIdConfiguration;
    }

}
