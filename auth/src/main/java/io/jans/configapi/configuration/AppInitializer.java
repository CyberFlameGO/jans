/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.configuration;

import io.jans.configapi.auth.api.ApiProtectionService;
import io.jans.configapi.auth.service.AuthorizationService;
import io.jans.configapi.auth.service.OpenIdAuthorizationService;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

@ApplicationScoped
@Named("appInitializer")
public class AppInitializer {

    public AppInitializer() {
        System.out.println(
                "\n\n\n\n **************************** AppInitializer() ****************************  \n\n\n\n");
    }

    @Inject
    Logger log;

    @Inject
    private Event<ApplicationInitializedEvent> eventApplicationInitialized;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    BeanManager beanManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private ApiProtectionService apiProtectionService;

    @Inject
    private Instance<AuthorizationService> authorizationServiceInstance;

    // void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("=================================================================");
        log.info("=============  STARTING API APPLICATION  ========================");
        log.info("=================================================================");

        // System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED,
        // "true"); ???TBD Set in webapp

        this.configurationFactory.create();
        persistenceEntryManagerInstance.get();
        this.createAuthorizationService();
        // this.createMetricRegistry(); //TDB???
        log.info("=================================================================");
        log.info("==============  APPLICATION IS UP AND RUNNING ===================");
        log.info("=================================================================");
    }

    // void onStop(/* @Observes ShutdownEvent ev */) {
    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  API APPLICATION STOPPED  ==========================");
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public ConfigurationFactory getConfigurationFactory() {
        return configurationFactory;
    }

    @Produces
    @ApplicationScoped
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    public PersistenceEntryManager createPersistenceEntryManager() throws OxIntializationException {
        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService
                .getPersistenceEntryManagerFactory(configurationFactory.getPersistenceConfiguration());
        PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory
                .createEntryManager(configurationFactory.getDecryptedConnectionProperties());
        log.debug("Created {} with operation service {}", persistenceEntryManager,
                persistenceEntryManager.getOperationService());
        return persistenceEntryManager;
    }

    @Produces
    @ApplicationScoped
    @Named("authorizationService")
    private AuthorizationService createAuthorizationService() {
        log.info(
                "=============  AppInitializer::createAuthorizationService() - ConfigurationFactory.getApiProtectionType() = "
                        + ConfigurationFactory.getApiProtectionType());
        if (StringHelper.isEmpty(ConfigurationFactory.getApiProtectionType())) {
            throw new ConfigurationException("API Protection Type not defined");
        }
        try {
            // Verify resources available
            apiProtectionService.verifyResources(ConfigurationFactory.getApiProtectionType(),
                    ConfigurationFactory.getApiClientId());
            return authorizationServiceInstance.select(OpenIdAuthorizationService.class).get();
        } catch (Exception ex) {
            log.error("Failed to create AuthorizationService instance", ex);
            throw new ConfigurationException("Failed to create AuthorizationService instance", ex);
        }
    }

    /*
     * 
     * @Produces
     * 
     * @ApplicationScoped
     * 
     * @Named("metricRegistry") private MetricRegistry createMetricRegistry() {
     * log.info("=============  AppInitializer::MetricRegistry() - Entry"); try {
     * //MetricRegistry metricRegistry =
     * MetricRegistries.get(MetricRegistry.Type.APPLICATION); // MetricRegistry
     * metricRegistry = new MetricRegistry(); //Metadata metadata =
     * Metadata.builder() //log.
     * info("=============  AppInitializer::MetricRegistry() - metricRegistry = "
     * +metricRegistry+"\n"); return null; } catch (Exception ex) {
     * log.error("Failed to create MetricRegistry instance", ex); throw new
     * ConfigurationException("Failed to create MetricRegistry instance", ex); } }
     */

    public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
        closePersistenceEntryManager();
        PersistenceEntryManager ldapEntryManager = persistenceEntryManagerInstance.get();
        persistenceEntryManagerInstance.destroy(ldapEntryManager);
        log.debug("Recreated instance {} with operation service: {}", ldapEntryManager,
                ldapEntryManager.getOperationService());
    }

    private void closePersistenceEntryManager() {
        PersistenceEntryManager oldInstance = CdiUtil.getContextBean(beanManager, PersistenceEntryManager.class,
                ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
        if (oldInstance == null || oldInstance.getOperationService() == null)
            return;

        log.debug("Attempting to destroy {} with operation service: {}", oldInstance,
                oldInstance.getOperationService());
        oldInstance.destroy();
        log.debug("Destroyed {} with operation service: {}", oldInstance, oldInstance.getOperationService());
    }
}
