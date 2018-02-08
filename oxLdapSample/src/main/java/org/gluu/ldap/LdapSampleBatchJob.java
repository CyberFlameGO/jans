package org.gluu.ldap;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.persist.exception.mapping.EntryPersistenceException;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.ldap.operation.impl.LdapBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.search.filter.Filter;
import org.xdi.log.LoggingHelper;

import com.unboundid.util.StaticUtils;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
public class LdapSampleBatchJob {
    private static final Logger log;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        log = Logger.getLogger(LdapSample.class);
    }

    public static void main(String[] args) {
		// Prepare sample connection details
		LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

		// Create LDAP entry manager
		final LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        LdapBatchOperation<SimpleTokenLdap> tokenLdapBatchOperation = new LdapBatchOperation<SimpleTokenLdap>() {
        	
        	private int processedCount = 0;

        	@Override
        	public List<SimpleTokenLdap> getChunkOrNull(int batchSize) {
        		log.info("Processed: " + processedCount);
                final Filter filter = Filter.createPresenceFilter("oxAuthExpiration");
                return ldapEntryManager.findEntries("o=gluu", SimpleTokenLdap.class, filter, SearchScope.SUB, new String[]{"oxAuthExpiration"}, this, 0, batchSize, batchSize);
            }

            @Override
            public void performAction(List<SimpleTokenLdap> objects) {
                for (SimpleTokenLdap simpleTokenLdap : objects) {
                	try {
	                    CustomAttribute customAttribute = getUpdatedAttribute(ldapEntryManager, "oxAuthExpiration", simpleTokenLdap.getAttribute("oxAuthExpiration"));
	                    simpleTokenLdap.setCustomAttributes(Arrays.asList(new CustomAttribute[]{customAttribute}));
	                    ldapEntryManager.merge(simpleTokenLdap);
	                    processedCount++;
    				} catch (EntryPersistenceException ex) {
    					log.error("Failed to update entry", ex);
    				}
                }
            }
        };
        tokenLdapBatchOperation.iterateAllByChunks(100);


        LdapBatchOperation<SimpleSession> sessionBatchOperation = new LdapBatchOperation<SimpleSession>() {
        	
        	private int processedCount = 0;

        	@Override
        	public List<SimpleSession> getChunkOrNull(int batchSize) {
        		log.info("Processed: " + processedCount);
                final Filter filter = Filter.createPresenceFilter("oxLastAccessTime");
                return ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, new String[]{"oxLastAccessTime"}, this, 0, batchSize, batchSize);
            }

            @Override
            public void performAction(List<SimpleSession> objects) {
                for (SimpleSession simpleSession : objects) {
                	try { 
	                    CustomAttribute customAttribute = getUpdatedAttribute(ldapEntryManager, "oxLastAccessTime", simpleSession.getAttribute("oxLastAccessTime"));
	                    simpleSession.setCustomAttributes(Arrays.asList(new CustomAttribute[]{customAttribute}));
	                    ldapEntryManager.merge(simpleSession);
	                    processedCount++;
					} catch (EntryPersistenceException ex) {
						log.error("Failed to update entry", ex);
					}
                }
            }
        };
        sessionBatchOperation.iterateAllByChunks(100);
    }

    private static CustomAttribute getUpdatedAttribute(LdapEntryManager ldapEntryManager, String attributeName, String attributeValue) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date oxLastAccessTimeDate = StaticUtils.decodeGeneralizedTime(attributeValue);
            calendar.setTime(oxLastAccessTimeDate);
            calendar.add(Calendar.SECOND, -1);

            CustomAttribute customAttribute = new CustomAttribute();
            customAttribute.setName(attributeName);
            customAttribute.setValue(ldapEntryManager.encodeGeneralizedTime(calendar.getTime()));
            return customAttribute;
        } catch (ParseException e) {
            log.error("Can't parse attribute", e);
        }
        return null;
    }
}
