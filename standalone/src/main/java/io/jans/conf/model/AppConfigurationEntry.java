/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.conf.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy Movchan
 * @version 0.1, 03/25/2016
 */
@DataEntry
@ObjectClass(value = "oxApplicationConfiguration")
public class AppConfigurationEntry implements Serializable {

	private static final long serialVersionUID = 1847361642302974184L;

	@DN
    private String dn;

    @AttributeName(name = "oxRevision")
    private long revision;

    @JsonObject
	@AttributeName(name = "oxConfApplication")
    private AppConfiguration application;

    public AppConfigurationEntry() {}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public AppConfiguration getApplication() {
		return application;
	}

	public void setApplication(AppConfiguration application) {
		this.application = application;
	}

}
