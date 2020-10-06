/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.model.custom.script;

import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.auth.AuthenticationCustomScript;
import io.jans.model.custom.script.type.auth.DummyPersonAuthenticationType;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import io.jans.model.custom.script.type.authz.ConsentGatheringType;
import io.jans.model.custom.script.type.authz.DummyConsentGatheringType;
import io.jans.model.custom.script.type.ciba.DummyEndUserNotificationType;
import io.jans.model.custom.script.type.ciba.EndUserNotificationType;
import io.jans.model.custom.script.type.client.ClientRegistrationType;
import io.jans.model.custom.script.type.client.DummyClientRegistrationType;
import io.jans.model.custom.script.type.id.DummyIdGeneratorType;
import io.jans.model.custom.script.type.id.IdGeneratorType;
import io.jans.model.custom.script.type.idp.DummyIdpType;
import io.jans.model.custom.script.type.idp.IdpType;
import io.jans.model.custom.script.type.introspection.DummyIntrospectionType;
import io.jans.model.custom.script.type.introspection.IntrospectionType;
import io.jans.model.custom.script.type.logout.DummyEndSessionType;
import io.jans.model.custom.script.type.logout.EndSessionType;
import io.jans.model.custom.script.type.owner.DummyResourceOwnerPasswordCredentialsType;
import io.jans.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import io.jans.model.custom.script.type.persistence.DummyPeristenceType;
import io.jans.model.custom.script.type.persistence.PersistenceType;
import io.jans.model.custom.script.type.postauthn.DummyPostAuthnType;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;
import io.jans.model.custom.script.type.scim.DummyScimType;
import io.jans.model.custom.script.type.scim.ScimType;
import io.jans.model.custom.script.type.scope.DummyDynamicScopeType;
import io.jans.model.custom.script.type.scope.DynamicScopeType;
import io.jans.model.custom.script.type.session.ApplicationSessionType;
import io.jans.model.custom.script.type.session.DummyApplicationSessionType;
import io.jans.model.custom.script.type.spontaneous.DummySpontaneousScopeType;
import io.jans.model.custom.script.type.spontaneous.SpontaneousScopeType;
import io.jans.model.custom.script.type.uma.*;
import io.jans.model.custom.script.type.user.*;
import io.jans.model.custom.script.type.BaseExternalType;
import org.gluu.model.custom.script.type.uma.*;
import org.gluu.model.custom.script.type.user.*;
import io.jans.persist.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * List of supported custom scripts
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public enum CustomScriptType implements AttributeEnum {

    PERSON_AUTHENTICATION("person_authentication", "Person Authentication", PersonAuthenticationType.class, AuthenticationCustomScript.class,
            "PersonAuthentication", new DummyPersonAuthenticationType()),
    INTROSPECTION("introspection", "Introspection", IntrospectionType.class, CustomScript.class, "Introspection", new DummyIntrospectionType()),
    RESOURCE_OWNER_PASSWORD_CREDENTIALS("resource_owner_password_credentials", "Resource Owner Password Credentials", ResourceOwnerPasswordCredentialsType.class, CustomScript.class, "ResourceOwnerPasswordCredentials", new DummyResourceOwnerPasswordCredentialsType()),
    APPLICATION_SESSION("application_session", "Application Session", ApplicationSessionType.class, CustomScript.class, "ApplicationSession",
            new DummyApplicationSessionType()),
    CACHE_REFRESH("cache_refresh", "Cache Refresh", CacheRefreshType.class, CustomScript.class, "CacheRefresh",
            new DummyCacheRefreshType()),
    UPDATE_USER("update_user", "Update User", UpdateUserType.class, CustomScript.class, "UpdateUser", new DummyUpdateUserType()),
    USER_REGISTRATION("user_registration", "User Registration", UserRegistrationType.class, CustomScript.class, "UserRegistration",
            new DummyUserRegistrationType()),
    CLIENT_REGISTRATION("client_registration", "Client Registration", ClientRegistrationType.class, CustomScript.class, "ClientRegistration",
            new DummyClientRegistrationType()),
    ID_GENERATOR("id_generator", "Id Generator", IdGeneratorType.class, CustomScript.class, "IdGenerator",
            new DummyIdGeneratorType()),
    UMA_RPT_POLICY("uma_rpt_policy", "UMA RPT Policies", UmaRptPolicyType.class, CustomScript.class, "UmaRptPolicy",
            new UmaDummyRptPolicyType()),
    UMA_RPT_CLAIMS("uma_rpt_claims", "UMA RPT Claims", UmaRptClaimsType.class, CustomScript.class, "UmaRptClaims", new UmaDummyRptClaimsType()),
    UMA_CLAIMS_GATHERING("uma_claims_gathering", "UMA Claims Gathering", UmaClaimsGatheringType.class, CustomScript.class, "UmaClaimsGathering",
            new UmaDummyClaimsGatheringType()),
    CONSENT_GATHERING("consent_gathering", "Consent Gathering", ConsentGatheringType.class, CustomScript.class, "ConsentGathering",
            new DummyConsentGatheringType()),
    DYNAMIC_SCOPE("dynamic_scope", "Dynamic Scopes", DynamicScopeType.class, CustomScript.class, "DynamicScope",
            new DummyDynamicScopeType()),
    SPONTANEOUS_SCOPE("spontaneous_scope", "Spontaneous Scopes", SpontaneousScopeType.class, CustomScript.class, "SpontaneousScope", new DummySpontaneousScopeType()),
    END_SESSION("end_session", "End Session", EndSessionType.class, CustomScript.class, "EndSession", new DummyEndSessionType()),
    POST_AUTHN("post_authn", "Post Authentication", PostAuthnType.class, CustomScript.class, "PostAuthn", new DummyPostAuthnType()),
    SCIM("scim", "SCIM", ScimType.class, CustomScript.class, "ScimEventHandler", new DummyScimType()),
    CIBA_END_USER_NOTIFICATION("ciba_end_user_notification", "CIBA End User Notification", EndUserNotificationType.class,
            CustomScript.class, "EndUserNotification", new DummyEndUserNotificationType()),
    PERSISTENCE_EXTENSION("persistence_extension", "Persistence Extension", PersistenceType.class, CustomScript.class, "PersistenceExtension", new DummyPeristenceType()),
    IDP("idp", "Idp Extension", IdpType.class, CustomScript.class, "IdpExtension", new DummyIdpType());

    private String value;
    private String displayName;
    private Class<? extends BaseExternalType> customScriptType;
    private Class<? extends CustomScript> customScriptModel;
    private String pythonClass;
    private BaseExternalType defaultImplementation;

    private static Map<String, CustomScriptType> MAP_BY_VALUES = new HashMap<String, CustomScriptType>();

    static {
        for (CustomScriptType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    CustomScriptType(String value, String displayName, Class<? extends BaseExternalType> customScriptType,
            Class<? extends CustomScript> customScriptModel, String pythonClass, BaseExternalType defaultImplementation) {
        this.displayName = displayName;
        this.value = value;
        this.customScriptType = customScriptType;
        this.customScriptModel = customScriptModel;
        this.pythonClass = pythonClass;
        this.defaultImplementation = defaultImplementation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public Class<? extends BaseExternalType> getCustomScriptType() {
        return customScriptType;
    }

    public Class<? extends CustomScript> getCustomScriptModel() {
        return customScriptModel;
    }

    public String getPythonClass() {
        return pythonClass;
    }

    public BaseExternalType getDefaultImplementation() {
        return defaultImplementation;
    }

    public static CustomScriptType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
