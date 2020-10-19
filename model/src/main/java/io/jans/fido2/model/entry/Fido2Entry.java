/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.entry;

import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * Fido2 base persistence entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@DataEntry(sortBy = "creationDate")
@ObjectClass
public class Fido2Entry extends BaseEntry {

    @AttributeName(ignoreDuringUpdate = true, name = "jansId")
    private String id;

    @AttributeName(name = "jansCodeChallenge")
    private String challange;

    @AttributeName(name = "jansCodeChallengeHash")
    private String challangeHash;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "personInum")
    private String userInum;

    public Fido2Entry() {
    }

    public Fido2Entry(String dn) {
        super(dn);
    }

    public Fido2Entry(String dn, String id, Date creationDate, String userInum, String challange) {
        super(dn);
        this.id = id;
        this.creationDate = creationDate;
        this.userInum = userInum;
        this.challange = challange;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallange() {
        return challange;
    }

    public void setChallange(String challange) {
        this.challange = challange;
    }

    public String getChallangeHash() {
        return challangeHash;
    }

    public void setChallangeHash(String challangeHash) {
        this.challangeHash = challangeHash;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getUserInum() {
        return userInum;
    }

    public void setUserInum(String userInum) {
        this.userInum = userInum;
    }
}
