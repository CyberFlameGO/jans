function() {
	
	var stream = read('classpath:karate.properties');
	var props = new java.util.Properties();
	props.load(stream);

    var env = props.get('karate.env'); // get java system property 'karate.env'
    karate.configure("ssl", true);
    
    if (!env) {
        env = 'dev'; //env can be anything: dev, qa, staging, etc.
    }
    
    var url = props.get('karate.test.url');
    var port = props.get('karate.test.port');
    var baseUrl = url + (port ? ':' + port : '');
    var config = {
	    env: env,
	    accessToken: '527bd668-78e3-4095-94ef-e537be1af91d',
	    baseUrl: baseUrl,
	    healthUrl: baseUrl + '/health',
	    fido2Url: baseUrl + '/jans-config-api/api/v1/fido2/config',
	    acrsUrl: baseUrl + '/jans-config-api/api/v1/acrs',
	    authConfigurationUrl: baseUrl + '/jans-config-api/api/v1/config/oxauth',
	    scriptsUrl: baseUrl + '/jans-config-api/api/v1/config/scripts',
	    cacheUrl: baseUrl + '/jans-config-api/api/v1/config/cache',
	    ldapUrl: baseUrl + '/jans-config-api/api/v1/config/database/ldap',
	    openidclients_url: baseUrl + '/jans-config-api/api/v1/openid/clients',
	    scopes_url: baseUrl + '/jans-config-api/api/v1/scopes',
	    openidsectors_url: baseUrl + '/jans-config-api/api/v1/openid/sectoridentifiers',
	    umaresources_url: baseUrl + '/jans-config-api/api/v1/uma/resources',
	    attributes_url: baseUrl + '/jans-config-api/api/v1/attributes',
	    smtp_url: baseUrl + '/jans-config-api/api/v1/config/smtp',
    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);
    
    return config;
}