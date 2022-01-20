/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.configuration;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimAppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConf;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.util.properties.FileConfiguration;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigurationFactory {
    
    public static final String CONFIGURATION_ENTRY_DN = "scim_ConfigurationEntryDN";

    @Inject
    private Logger log;

    @Inject
    private Event<ScimAppConfiguration> configurationUpdateEvent;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private PersistenceEntryManager entryManager;

    public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
    public final static String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

    static {
        if (System.getProperty("jans.base") != null) {
            BASE_DIR = System.getProperty("jans.base");
        } else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
            BASE_DIR = System.getProperty("catalina.base");
        } else if (System.getProperty("catalina.home") != null) {
            BASE_DIR = System.getProperty("catalina.home");
        } else if (System.getProperty("jboss.home.dir") != null) {
            BASE_DIR = System.getProperty("jboss.home.dir");
        } else {
            BASE_DIR = null;
        }
    }

    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String BASE_PROPERTIES_FILE = DIR + "jans.properties";
    private static final String APP_PROPERTIES_FILE = DIR + "scim.properties";


    private boolean loaded = false;
    private FileConfiguration baseConfiguration;    
    private PersistenceConfiguration persistenceConfiguration;
    private ScimAppConfiguration dynamicConf;
    private StaticConfiguration staticConf;
    private String cryptoConfigurationSalt;

    private AtomicBoolean isActive;
    private long baseConfigurationFileLastModifiedTime;
    private long loadedRevision = -1;
    private boolean loadedFromLdap = true;

    @PostConstruct
    public void init() {
        this.isActive = new AtomicBoolean(true);
        try {
            log.error("ScimConfigurationFactory::loadConfigurationFromLdap() - persistanceFactoryService:{} ",persistanceFactoryService);
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
            
            loadBaseConfiguration();
            
            //create();
          
        } finally {
            this.isActive.set(false);
        }
    }

    public void create() {
        if (!createFromDb()) {
            log.error("Failed to load configuration from LDAP. Please fix it!!!.");
            throw new ConfigurationException("Failed to load configuration from LDAP.");
        } else {
            log.info("Configuration loaded successfully.");
        }
    }
    

    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    @Produces
    @ApplicationScoped
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }

    @Produces
    @ApplicationScoped
    public ScimAppConfiguration getAppConfiguration() {
        return dynamicConf;
    }

    @Produces
    @ApplicationScoped
    public StaticConfiguration getStaticConfiguration() {
        return staticConf;
    }

    public BaseDnConfiguration getBaseDn() {
        return getStaticConfiguration().getBaseDn();
    }

    public String getCryptoConfigurationSalt() {
        return cryptoConfigurationSalt;
    }

    private boolean createFromDb() {
        log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final ScimConf c = loadConfigurationFromLdap();
            if (c != null) {
                init(c);

              
                this.loaded = true;
                configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(dynamicConf);

                return true;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        throw new ConfigurationException("Unable to find configuration in DB... ");
    }

    private ScimConf loadConfigurationFromLdap(String... returnAttributes) {
        log.error("ScimConfigurationFactory::loadConfigurationFromLdap() - returnAttributes:{}, entryManager:{}, ",returnAttributes,entryManager);
       
        final String dn = getConfigurationDn();
        try {
            final ScimConf conf = entryManager.find(dn, ScimConf.class, returnAttributes);
            log.error("ScimConfigurationFactory::loadConfigurationFromLdap() - conf:{} ",conf);
            return conf;
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
        }

        return null;
    }

    public String getConfigurationDn() {
        return this.baseConfiguration.getString(CONFIGURATION_ENTRY_DN);
    }

    private void init(ScimConf conf) {
        initConfigurationConf(conf);
        this.loadedRevision = conf.getRevision();
    }

    private void initConfigurationConf(ScimConf conf) {
        if (conf.getDynamicConf() != null) {
            dynamicConf = conf.getDynamicConf();
        }
    }

    private void loadBaseConfiguration() {
        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
        File baseConfiguration = new File(BASE_PROPERTIES_FILE);
        this.baseConfigurationFileLastModifiedTime = baseConfiguration.lastModified();
    }


    private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
        try {
            FileConfiguration fileConfiguration = new FileConfiguration(fileName);

            return fileConfiguration;
        } catch (Exception ex) {
            if (isMandatory) {
                log.error("Failed to load configuration from {}", fileName, ex);
                throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
            }
        }

        return null;
    }

}
