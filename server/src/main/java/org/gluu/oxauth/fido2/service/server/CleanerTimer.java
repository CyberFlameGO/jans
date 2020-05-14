/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.fido2.service.server;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.fido2.service.persist.AuthenticationPersistenceService;
import org.gluu.oxauth.fido2.service.persist.RegistrationPersistenceService;
import org.gluu.oxauth.fido2.service.server.AppConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;
import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.CleanerEvent;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

/**
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Named
public class CleanerTimer {

	public final static int BATCH_SIZE = 1000;
	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private CacheProvider cacheProvider;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private AuthenticationPersistenceService authenticationPersistenceService;

	@Inject
	private RegistrationPersistenceService registrationPersistenceService;

	@Inject
	private Event<TimerEvent> cleanerEvent;

	private long lastFinishedTime;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.debug("Initializing Cleaner Timer");
		this.isActive = new AtomicBoolean(false);

		// Schedule to start cleaner every 1 minute
		cleanerEvent.fire(
				new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new CleanerEvent(), Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
	}

	@Asynchronous
	public void process(@Observes @Scheduled CleanerEvent cleanerEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			processImpl();
		} finally {
			this.isActive.set(false);
		}
	}

	private boolean isStartProcess() {
		int interval = appConfiguration.getCleanServiceInterval();
		if (interval < 0) {
			log.info("Cleaner Timer is disabled.");
			log.warn(
					"Cleaner Timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
			return false;
		}

		long cleaningInterval = interval * 1000;

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= cleaningInterval;
	}

	public void processImpl() {
		try {
			if (!isStartProcess()) {
				log.trace("Starting conditions aren't reached");
				return;
			}

			int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();
			if (chunkSize <= 0)
				chunkSize = BATCH_SIZE;

			Date now = new Date();

			for (String baseDn : createCleanServiceBaseDns()) {
				try {
					if (persistenceEntryManager.hasExpirationSupport(baseDn)) {
						continue;
					}

					log.debug("Start clean up for baseDn: " + baseDn);
					final Stopwatch started = Stopwatch.createStarted();

					int removed = cleanup(baseDn, now, chunkSize);

					log.debug("Finished clean up for baseDn: {}, takes: {}ms, removed items: {}", baseDn,
							started.elapsed(TimeUnit.MILLISECONDS), removed);
				} catch (Exception e) {
					log.error("Failed to process clean up for baseDn: " + baseDn, e);
				}
			}

			processCache(now);

			registrationPersistenceService.cleanup(now, chunkSize);
			authenticationPersistenceService.cleanup(now, chunkSize);

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Exception e) {
			log.error("Failed to process clean up.", e);
		}
	}

	public Set<String> createCleanServiceBaseDns() {
		final Set<String> cleanServiceBaseDns = Sets.newHashSet();
//		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getSessions());

		log.debug("Built-in base dns: " + cleanServiceBaseDns);

		return cleanServiceBaseDns;
	}

	public int cleanup(final String baseDn, final Date now, final int batchSize) {
		try {
			Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("del", true),
					Filter.createLessOrEqualFilter("exp", persistenceEntryManager.encodeTime(baseDn, now)));

			return persistenceEntryManager.remove(baseDn, DeletableEntity.class, filter, batchSize);
		} catch (Exception e) {
			log.error("Failed to perform clean up.", e);
		}

		return 0;
	}

	private void processCache(Date now) {
		try {
			cacheProvider.cleanup(now);
		} catch (Exception e) {
			log.error("Failed to clean up cache.", e);
		}
	}

}
