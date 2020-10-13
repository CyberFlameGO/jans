/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.Serializable;

/**
 * @author "Oleksiy Tataryn"
 *
 */
public abstract class OrganizationService implements Serializable {

    private static final long serialVersionUID = -6601700282123372943L;

    public String getDnForOrganization(String baseDn) {
        if (baseDn == null) {
            baseDn = "o=gluu";
        }
        return baseDn;
    }

}
