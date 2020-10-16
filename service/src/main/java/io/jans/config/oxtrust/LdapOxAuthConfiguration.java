/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import io.jans.config.oxauth.WebKeysSettings;
import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Rahat Ali
 * @version 2.1, 19/04/2015
 */
@DataEntry
@ObjectClass(value = "oxAuthConfiguration")
public class LdapOxAuthConfiguration extends Entry {

    private static final long serialVersionUID = 2453308522994526877L;

    @DN
    private String dn;

    @AttributeName(name = "jsConfDyn")
    private String dynamicConf;

    @AttributeName(name = "jsConfStatic")
    private String staticConf;

    @AttributeName(name = "jsConfErrors")
    private String errors;

    @AttributeName(name = "jsConfWebKeys")
    private String webKeys;

    @JsonObject
    @AttributeName(name = "jsWebKeysSettings")
    private WebKeysSettings webKeysSettings;

    @AttributeName(name = "jsRevision")
    private long revision;

    public LdapOxAuthConfiguration() {
    }

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getDynamicConf() {
		return dynamicConf;
	}

	public void setDynamicConf(String dynamicConf) {
		this.dynamicConf = dynamicConf;
	}

	public String getStaticConf() {
		return staticConf;
	}

	public void setStaticConf(String staticConf) {
		this.staticConf = staticConf;
	}

	public String getErrors() {
		return errors;
	}

	public void setErrors(String errors) {
		this.errors = errors;
	}

	public String getWebKeys() {
		return webKeys;
	}

	public void setWebKeys(String webKeys) {
		this.webKeys = webKeys;
	}

	public WebKeysSettings getWebKeysSettings() {
		return webKeysSettings;
	}

	public void setWebKeysSettings(WebKeysSettings webKeysSettings) {
		this.webKeysSettings = webKeysSettings;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
