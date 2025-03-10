import re
import os
import time
import pprint
import inspect
from collections import OrderedDict

from setup_app.paths import INSTALL_DIR
from setup_app.static import InstallTypes
from setup_app.utils.printVersion import get_war_info
from setup_app.utils import base

class Config:

    # we define statics here so that is is acessible without construction
    jansOptFolder = '/opt/jans'
    distFolder = '/opt/dist'
    jre_home = '/opt/jre'
    jansBaseFolder = '/etc/jans'
    certFolder = '/etc/certs'
    oxBaseDataFolder = '/var/jans'
    etc_hosts = '/etc/hosts'
    etc_hostname = '/etc/hostname'
    osDefault = '/etc/default'
    sysemProfile = '/etc/profile'
    jython_home = '/opt/jython'
    ldapBaseFolder = '/opt/opendj'
    network = '/etc/sysconfig/network'
    jetty_home = '/opt/jetty'
    jetty_base = os.path.join(jansOptFolder, 'jetty')
    installed_instance = False
    profile = 'jans'

    @classmethod
    def get(self, attr, default=None):
        return getattr(self, attr) if hasattr(self, attr) else default


    @classmethod
    def determine_version(self):
        oxauth_info = get_war_info(os.path.join(self.distJansFolder, 'jans-auth.war'))
        self.oxVersion = oxauth_info['version']
        self.currentJansVersion = re.search('([\d.]+)', oxauth_info['version']).group().strip('.')
        self.githubBranchName = oxauth_info['branch']

        self.ce_setup_zip = 'https://github.com/JanssenProject/jans-setup/archive/%s.zip' % self.githubBranchName

    @classmethod
    def dump(self, dumpFile=False):
        if self.dump_config_on_error:
            return

        myDict = {}
        for obj_name, obj in inspect.getmembers(self):
            obj_name = str(obj_name)
            if not obj_name.startswith('__') and (not callable(obj)):
                myDict[obj_name] = obj

        if dumpFile:
            fn = os.path.join(self.install_dir, 'config-'+time.ctime().replace(' ', '-'))
            with open(fn, 'w') as w:
                w.write(pprint.pformat(myDict, indent=2))
        else:
            pp = pprint.PrettyPrinter(indent=2)
            pp.pprint(myDict)

    @classmethod
    def calculate_mem(self):
        if Config.opendj_install:
            self.opendj_max_ram = int(int(Config.jans_max_mem) * .2) # 20% of mem_use
            self.application_max_ram = int(int(Config.jans_max_mem) * .8) # 80% of mem_use
        else:
            self.opendj_max_ram = 0
            self.application_max_ram = int(Config.jans_max_mem)

    @classmethod
    def init(self, install_dir=INSTALL_DIR):

        self.install_dir = install_dir
        self.thread_queue = None
        self.jetty_user = 'jetty'
        self.dump_config_on_error = False


        self.ldapBinFolder = os.path.join(self.ldapBaseFolder, 'bin')
        if base.snap:
            self.ldapBaseFolder = os.path.join(base.snap_common, 'opendj')
            self.jetty_user = 'root'

        #create dummy progress bar that logs to file in case not defined
        progress_log_file = os.path.join(self.install_dir, 'logs', 'progress-bar.log')
        class DummyProgress:

            services = []

            def register(self, installer):
                pass

            def before_start(self):
                pass

            def start(self):
                pass

            def progress(self, service_name, msg, incr=False):
                with open(progress_log_file, 'a') as w:
                    w.write("{}: {}\n".format(service_name, msg))

        self.pbar = DummyProgress()

        self.properties_password = None
        self.noPrompt = False

        self.distAppFolder = os.path.join(self.distFolder, 'app')
        self.distJansFolder = os.path.join(self.distFolder, 'jans')
        self.distTmpFolder = os.path.join(self.distFolder, 'tmp')

        self.downloadWars = None
        self.templateRenderingDict = {
                                        'oxauthClient_2_inum': 'AB77-1A2B',
                                        'oxauthClient_3_inum': '3E20',
                                        'oxauthClient_4_inum': 'FF81-2D39',
                                        'idp_attribute_resolver_ldap.search_filter': '(|(uid=$requestContext.principalName)(mail=$requestContext.principalName))',
                                        'oxd_port': '8443',
                                     }

        # java commands
        self.cmd_java = os.path.join(self.jre_home, 'bin/java')
        self.cmd_keytool = os.path.join(self.jre_home, 'bin/keytool')
        self.cmd_jar = os.path.join(self.jre_home, 'bin/jar')
        os.environ['OPENDJ_JAVA_HOME'] =  self.jre_home

        # Component ithversions
        self.apache_version = None
        self.opendj_version = None

        #passwords
        self.ldapPass = None
        self.admin_password = None
        self.admin_password = ''
        self.cb_password = None
        self.encoded_cb_password = ''

        #DB installation types
        self.opendj_install = InstallTypes.LOCAL
        self.cb_install = InstallTypes.NONE
        self.rdbm_install = False
        
        self.couchbase_buckets = []
        
        #rdbm
        self.rdbm_install_type = InstallTypes.NONE
        self.rdbm_type = 'mysql'
        self.rdbm_host = 'localhost'
        self.rdbm_port = 3306
        self.rdbm_db = 'jans'
        self.rdbm_user = None
        self.rdbm_password = None
        self.static_rdbm_dir = os.path.join(self.install_dir, 'static/rdbm')

        #spanner
        self.spanner_project = 'jans-project'
        self.spanner_instance = 'jans-instance'
        self.spanner_database = 'jansdb' 
        self.spanner_emulator_host = None
        self.google_application_credentials = None

        # Jans components installation status
        self.loadData = True
        self.installJans = True
        self.installJre = True
        self.installJetty = True
        self.installJython = True
        self.installOxAuth = True
        self.installOxTrust = True
        self.installHttpd = True
        self.installSaml = False
        self.installOxAuthRP = False
        self.installPassport = False
        self.installJansRadius = False
        self.installScimServer = True
        self.installFido2 = True
        self.installConfigApi = True
        self.installCasa = False
        self.installOxd = False
        self.installEleven = False
        self.installJansCli = False
        self.loadTestData = False
        self.allowPreReleasedFeatures = False

        # backward compatibility
        self.os_type = base.os_type
        self.os_version = base.os_version
        self.os_initdaemon = base.os_initdaemon

        self.persistence_type = 'ldap'

        self.setup_properties_fn = os.path.join(self.install_dir, 'setup.properties')
        self.savedProperties = os.path.join(self.install_dir, 'setup.properties.last')

        self.jansOptBinFolder = os.path.join(self.jansOptFolder, 'bin')
        self.jansOptSystemFolder = os.path.join(self.jansOptFolder, 'system')
        self.jansOptPythonFolder = os.path.join(self.jansOptFolder, 'python')
        self.configFolder = os.path.join(self.jansBaseFolder, 'conf') 

        self.jans_properties_fn = os.path.join(self.configFolder,'jans.properties')
        self.jans_hybrid_roperties_fn = os.path.join(self.configFolder, 'jans-hybrid.properties')

        self.cache_provider_type = 'NATIVE_PERSISTENCE'

        self.java_type = 'jre'

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.encoded_ox_ldap_pw = None
        self.encode_salt = None
        self.admin_inum = None

        self.jans_max_mem = int(base.current_mem_size * .85 * 1000) # 85% of physical memory
        self.calculate_mem()

        self.ldapBaseFolderldapPass = None

        self.outputFolder = os.path.join(self.install_dir, 'output')
        self.templateFolder = os.path.join(self.install_dir, 'templates')
        self.staticFolder = os.path.join(self.install_dir, 'static')

        self.extensionFolder = os.path.join(self.staticFolder, 'extension')

        self.encoded_ldapTrustStorePass = None

        self.ldapCertFn = self.opendj_cert_fn = os.path.join(self.certFolder, 'opendj.crt')
        self.ldapTrustStoreFn = self.opendj_p12_fn = os.path.join(self.certFolder, 'opendj.p12')

        self.oxd_package = base.determine_package(os.path.join(Config.distJansFolder, 'oxd-server*.tgz'))

        self.opendj_p12_pass = None

        self.ldap_binddn = 'cn=directory manager'
        self.ldap_hostname = 'localhost'
        self.couchbase_hostname = 'localhost'
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldap_admin_port = '4444'

        self.ldap_user_home = self.ldapBaseFolder
        self.ldapPassFn = os.path.join(self.ldap_user_home, '.pw')
        self.ldap_backend_type = 'je'

        self.jansScriptFiles = [
                            os.path.join(self.install_dir, 'static/scripts/logmanager.sh'),
                            os.path.join(self.install_dir, 'static/scripts/testBind.py')
                            ]

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.defaultTrustStoreFN = os.path.join(self.jre_home, 'jre/lib/security/cacerts')
        self.defaultTrustStorePW = 'changeit'


        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.

        self.jans_python_readme = os.path.join(self.jansOptPythonFolder, 'libs/python.txt')
        self.ox_ldap_properties = os.path.join(self.configFolder, 'jans-ldap.properties')
        self.jansCouchebaseProperties = os.path.join(self.configFolder, 'jans-couchbase.properties')
        self.jansRDBMProperties = os.path.join(self.configFolder, 'jans-sql.properties')
        self.jansSpannerProperties = os.path.join(self.configFolder, 'jans-spanner.properties')

        self.ldif_base = os.path.join(self.outputFolder, 'base.ldif')
        self.ldif_attributes = os.path.join(self.outputFolder, 'attributes.ldif')
        self.ldif_scopes = os.path.join(self.outputFolder, 'scopes.ldif')

        self.ldif_metric = os.path.join(self.staticFolder, 'metric/o_metric.ldif')
        self.ldif_site = os.path.join(self.install_dir, 'static/cache-refresh/o_site.ldif')
        self.ldif_configuration = os.path.join(self.outputFolder, 'configuration.ldif')

        self.system_profile_update_init = os.path.join(self.outputFolder, 'system_profile_init')
        self.system_profile_update_systemd = os.path.join(self.outputFolder, 'system_profile_systemd')

        ### rsyslog file customised for init.d
        self.rsyslogUbuntuInitFile = os.path.join(self.install_dir, 'static/system/ubuntu/rsyslog')
        self.ldap_setup_properties = os.path.join(self.templateFolder, 'opendj-setup.properties')

        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=Jans Auth CA Certificates'
        self.default_sig_key_algs = 'RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 EdDSA'
        self.default_enc_key_algs = 'RSA1_5 RSA-OAEP ECDH-ES'
        self.default_key_expiration = 365

        self.post_messages = []

        self.ldif_files = [self.ldif_base,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_site,
                           self.ldif_metric,
                           self.ldif_configuration,
                           ]


        self.ce_templates = {
                             self.jans_python_readme: True,
                             self.ox_ldap_properties: True,
                             self.ldap_setup_properties: False,
                             self.etc_hostname: False,
                             self.ldif_base: False,
                             self.ldif_attributes: False,
                             self.ldif_scopes: False,
                             self.network: False,
                             self.jans_properties_fn: True,
                             }

        self.service_requirements = {
                        'opendj': ['', 70],
                        'jans-auth': ['opendj', 72],
                        'jans-fido2': ['opendj', 73],
                        'identity': ['opendj jans-auth', 74],
                        'jans-scim': ['opendj jans-auth', 75],
                        'idp': ['opendj jans-auth', 76],
                        'casa': ['opendj jans-auth', 78],
                        'oxd-server': ['opendj jans-auth', 80],
                        'passport': ['opendj jans-auth', 82],
                        'jans-auth-rp': ['opendj jans-auth', 84],
                        'jans-radius': ['opendj jans-auth', 86],
                        'jans-eleven': ['opendj jans-auth', 79],
                        'jans-config-api': ['opendj jans-auth', 85],
                        }

        self.install_time_ldap = None


        self.couchbaseBucketDict = OrderedDict((
                        ('default', { 'ldif':[
                                            self.ldif_base, 
                                            self.ldif_attributes,
                                            self.ldif_scopes,
                                            self.ldif_configuration,
                                            self.ldif_metric,
                                            ],
                                      'memory_allocation': 100,
                                      'mapping': '',
                                      'document_key_prefix': []
                                    }),

                        ('user',     {   'ldif': [],
                                        'memory_allocation': 300,
                                        'mapping': 'people, groups, authorizations',
                                        'document_key_prefix': ['groups_', 'people_', 'authorizations_'],
                                    }),

                        ('site',     {   'ldif': [self.ldif_site],
                                        'memory_allocation': 100,
                                        'mapping': 'cache-refresh',
                                        'document_key_prefix': ['site_', 'cache-refresh_'],
                                    }),

                        ('cache',    {   'ldif': [],
                                        'memory_allocation': 100,
                                        'mapping': 'cache',
                                        'document_key_prefix': ['cache_'],
                                    }),

                        ('token',   { 'ldif': [],
                                      'memory_allocation': 300,
                                      'mapping': 'tokens',
                                      'document_key_prefix': ['tokens_'],
                                    }),

                        ('session',   { 'ldif': [],
                                      'memory_allocation': 200,
                                      'mapping': 'sessions',
                                      'document_key_prefix': [],
                                    }),

                    ))

        self.mappingLocations = { group: 'ldap' for group in self.couchbaseBucketDict }  #default locations are OpenDJ
        self.non_setup_properties = {
            'oxauth_client_jar_fn': os.path.join(self.distJansFolder, 'jans-auth-client-jar-with-dependencies.jar')
                }
        Config.addPostSetupService = []

    
