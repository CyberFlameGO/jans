/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service;

import io.jans.as.client.service.IntrospectionService;
import io.jans.configapi.auth.AuthClientFactory;
import io.jans.util.StringHelper;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.init.Initializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;

@ApplicationScoped
public class OpenIdService extends Initializable implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    private IntrospectionService introspectionService;

    public IntrospectionService getIntrospectionService() {
        init();
        return introspectionService;
    }

    @Override
    protected void initInternal() {
        try {
            loadOpenIdConfiguration();
        } catch (IOException ex) {
            logger.error("Failed to load oxAuth OpenId configuration", ex);
            throw new ConfigurationException("Failed to load oxAuth OpenId configuration", ex);
        }
    }

    private void loadOpenIdConfiguration() throws IOException {
        logger.debug(
                "OpenIdService::loadOpenIdConfiguration() - configurationService.find().getIntrospectionEndpoint() = "
                        + configurationService.find().getIntrospectionEndpoint());
        String introspectionEndpoint = configurationService.find().getIntrospectionEndpoint();
        this.introspectionService = AuthClientFactory.getIntrospectionService(introspectionEndpoint, false);

        logger.debug("\n\n OpenIdService::loadOpenIdConfiguration() - introspectionService =" + introspectionService);
        logger.info("Successfully loaded oxAuth configuration");
    }

}
