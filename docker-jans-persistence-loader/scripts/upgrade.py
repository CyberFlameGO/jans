import contextlib
import json
import logging.config
import os
from collections import namedtuple

from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.sql import SQLClient

from settings import LOGGING_CONFIG
from utils import doc_id_from_dn
from utils import id_from_dn

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")

Entry = namedtuple("Entry", ["id", "attrs"])


class BaseBackend:
    """Base class for backend adapters. Must be sub-classed per
    implementation details.
    """

    def __init__(self):
        self.jans_admin_ui_role_id = "inum=43F1,ou=scopes,o=jans"
        self.jans_admin_ui_claim = "inum=0A01,ou=attributes,o=jans"
        self.jans_attrs = '{"spontaneousClientId":null,"spontaneousClientScopes":null,"showInConfigurationEndpoint":true}'
        # SCIM users.read, SCIM users.write scopes that get added to config-api client
        self.jans_scim_scopes = [
            "inum=1200.2B7428,ou=scopes,o=jans",  # users.read scope
            "inum=1200.0A0198,ou=scopes,o=jans",  # users.write scope
        ]
        # jans_stat
        self.jans_stat_scopes = [
            "inum=C4F7,ou=scopes,o=jans",  # jans_stat
        ]


#: ID of base entry
JANS_BASE_ID = "o=jans"

#: ID of manager group
JANS_MANAGER_GROUP = "inum=60B7,ou=groups,o=jans"


class LDAPBackend(BaseBackend):
    def __init__(self, manager):
        super().__init__()
        self.manager = manager
        self.client = LdapClient(manager)
        self.type = "ldap"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        def format_attrs(attrs):
            _attrs = {}
            for k, v in attrs.items():
                if len(v) < 2:
                    v = v[0]
                _attrs[k] = v
            return _attrs

        filter_ = filter_ or "(objectClass=*)"

        entry = self.client.get(key, filter_=filter_, attributes=attrs)
        if not entry:
            return None
        return Entry(entry.entry_dn, format_attrs(entry.entry_attributes_as_dict))

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        del_flag = kwargs.get("delete_attr", False)

        if del_flag:
            mod = self.client.MODIFY_DELETE
        else:
            mod = self.client.MODIFY_REPLACE

        for k, v in attrs.items():
            if not isinstance(v, list):
                v = [v]
            attrs[k] = [(mod, v)]
        return self.client.modify(key, attrs)

    def update_people_entries(self):
        # add jansAdminUIRole to default admin user
        admin_inum = self.manager.config.get("admin_inum")
        id_ = f"inum={admin_inum},ou=people,o=jans"
        kwargs = {}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if "jansAdminUIRole" not in entry.attrs:
            entry.attrs["jansAdminUIRole"] = ["api-admin"]
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        # add jansAdminUIRole claim to profile scope
        kwargs = {}

        entry = self.get_entry(self.jans_admin_ui_role_id, **kwargs)
        if not entry:
            return

        if self.jans_admin_ui_claim not in entry.attrs["jansClaim"]:
            entry.attrs["jansClaim"].append(self.jans_admin_ui_claim)
            self.modify_entry(self.jans_admin_ui_role_id, entry.attrs, **kwargs)

    def update_clients_entries(self):
        jca_client_id = self.manager.config.get("jca_client_id")
        id_ = f"inum={jca_client_id},ou=clients,o=jans"
        kwargs = {}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        should_update = False

        # modify redirect UI of config-api client
        hostname = self.manager.config.get("hostname")

        if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]:
            entry.attrs["jansRedirectURI"].append(f"https://{hostname}/admin")
            should_update = True

        # add jans_stat, SCIM users.read, SCIM users.write scopes to config-api client
        for scope in (self.jans_scim_scopes + self.jans_stat_scopes):
            if scope not in entry.attrs["jansScope"]:
                entry.attrs["jansScope"].append(scope)
                should_update = True

        if should_update:
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scim_scopes_entries(self):
        # add jansAttrs to SCIM users.read and users.write scopes
        ids = self.jans_scim_scopes
        kwargs = {}

        for id_ in ids:
            entry = self.get_entry(id_, **kwargs)
            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs[
                    "jansAttrs"] = self.jans_attrs
                self.modify_entry(id_, entry.attrs, **kwargs)

    def update_base_entries(self):
        # add jansManagerGrp to base entry
        entry = self.get_entry(JANS_BASE_ID)
        if not entry:
            return

        if not entry.attrs.get("jansManagerGrp"):
            entry.attrs["jansManagerGrp"] = JANS_MANAGER_GROUP
            self.modify_entry(JANS_BASE_ID, entry.attrs)


class SQLBackend(BaseBackend):
    def __init__(self, manager):
        super().__init__()
        self.manager = manager
        self.client = SQLClient()
        self.type = "sql"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        table_name = kwargs.get("table_name")
        entry = self.client.get(table_name, key, attrs)

        if not entry:
            return None
        return Entry(key, entry)

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        table_name = kwargs.get("table_name")
        return self.client.update(table_name, key, attrs), ""

    def update_people_entries(self):
        # add jansAdminUIRole to default admin user
        admin_inum = self.manager.config.get("admin_inum")
        id_ = doc_id_from_dn(f"inum={admin_inum},ou=people,o=jans")
        kwargs = {"table_name": "jansPerson"}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        # sql entry may have empty jansAdminUIRole hash ({"v": []})
        if not entry.attrs["jansAdminUIRole"]["v"]:
            entry.attrs["jansAdminUIRole"] = {"v": ["api-admin"]}
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        # add jansAdminUIRole claim to profile scope
        id_ = doc_id_from_dn(self.jans_admin_ui_role_id)
        kwargs = {"table_name": "jansScope"}

        entry = self.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.jans_admin_ui_claim not in entry.attrs["jansClaim"]["v"]:
            entry.attrs["jansClaim"]["v"].append(self.jans_admin_ui_claim)
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_clients_entries(self):
        jca_client_id = self.manager.config.get("jca_client_id")
        id_ = doc_id_from_dn(f"inum={jca_client_id},ou=clients,o=jans")
        kwargs = {"table_name": "jansClnt"}

        entry = self.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # modify redirect UI of config-api client
        hostname = self.manager.config.get("hostname")

        if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]["v"]:
            entry.attrs["jansRedirectURI"]["v"].append(f"https://{hostname}/admin")
            should_update = True

        # add jans_stat, SCIM users.read, SCIM users.write scopes to config-api client
        for scope in (self.jans_scim_scopes + self.jans_stat_scopes):
            if scope not in entry.attrs["jansScope"]["v"]:
                entry.attrs["jansScope"]["v"].append(scope)
                should_update = True

        if should_update:
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scim_scopes_entries(self):
        # add jansAttrs to SCIM users.read and users.write scopes
        ids = [doc_id_from_dn(scope) for scope in self.jans_scim_scopes]
        kwargs = {"table_name": "jansScope"}

        for id_ in ids:
            entry = self.get_entry(id_, **kwargs)
            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs[
                    "jansAttrs"] = self.jans_attrs
                self.modify_entry(id_, entry.attrs, **kwargs)

    def update_base_entries(self):
        # add jansManagerGrp to base entry
        id_ = doc_id_from_dn(JANS_BASE_ID)
        kwargs = {"table_name": "jansOrganization"}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if not entry.attrs.get("jansManagerGrp"):
            entry.attrs["jansManagerGrp"] = JANS_MANAGER_GROUP
            self.modify_entry(id_, entry.attrs, **kwargs)


class CouchbaseBackend(BaseBackend):
    def __init__(self, manager):
        super().__init__()
        self.manager = manager
        hostname = os.environ.get("CN_COUCHBASE_URL", "localhost")
        user = get_couchbase_superuser(manager) or get_couchbase_user(manager)

        password = ""
        with contextlib.suppress(FileNotFoundError):
            password = get_couchbase_superuser_password(manager)
        password = password or get_couchbase_password(manager)

        self.client = CouchbaseClient(hostname, user, password)
        self.type = "couchbase"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        req = self.client.exec_query(
            f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"
        )
        if not req.ok:
            return

        try:
            _attrs = req.json()["results"][0]
            id_ = _attrs.pop("id")
            entry = Entry(id_, _attrs)
        except IndexError:
            entry = None
        return entry

    def modify_entry(self, key, attrs=None, **kwargs):
        bucket = kwargs.get("bucket")
        del_flag = kwargs.get("delete_attr", False)
        attrs = attrs or {}

        if del_flag:
            kv = ",".join(attrs.keys())
            mod_kv = f"UNSET {kv}"
        else:
            kv = ",".join([
                "{}={}".format(k, json.dumps(v))
                for k, v in attrs.items()
            ])
            mod_kv = f"SET {kv}"

        query = f"UPDATE {bucket} USE KEYS '{key}' {mod_kv}"
        req = self.client.exec_query(query)

        if req.ok:
            resp = req.json()
            status = bool(resp["status"] == "success")
            message = resp["status"]
        else:
            status = False
            message = req.text or req.reason
        return status, message

    def update_people_entries(self):
        # add jansAdminUIRole to default admin user
        admin_inum = self.manager.config.get("admin_inum")
        id_ = id_from_dn(f"inum={admin_inum},ou=people,o=jans")
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        kwargs = {"bucket": f"{bucket}_user"}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if "jansAdminUIRole" not in entry.attrs:
            entry.attrs["jansAdminUIRole"] = ["api-admin"]
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        # add jansAdminUIRole claim to profile scope
        id_ = id_from_dn(self.jans_admin_ui_role_id)
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        kwargs = {"bucket": bucket}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if self.jans_admin_ui_claim not in entry.attrs["jansClaim"]:
            entry.attrs["jansClaim"].append(self.jans_admin_ui_claim)
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_clients_entries(self):
        jca_client_id = self.manager.config.get("jca_client_id")
        id_ = id_from_dn(f"inum={jca_client_id},ou=clients,o=jans")
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        kwargs = {"bucket": bucket}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        should_update = False

        # modify redirect UI of config-api client
        hostname = self.manager.config.get("hostname")

        if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]:
            entry.attrs["jansRedirectURI"].append(f"https://{hostname}/admin")
            should_update = True

        # add jans_stat, SCIM users.read, SCIM users.write scopes to config-api client
        for scope in (self.jans_scim_scopes + self.jans_stat_scopes):
            if scope not in entry.attrs["jansScope"]:
                entry.attrs["jansScope"].append(scope)
                should_update = True

        if should_update:
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scim_scopes_entries(self):
        # add jansAttrs to SCIM users.read and users.write scopes
        ids = map(id_from_dn, self.jans_scim_scopes)
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        kwargs = {"bucket": bucket}

        for id_ in ids:
            entry = self.get_entry(id_, **kwargs)
            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs[
                    "jansAttrs"] = self.jans_attrs
                self.modify_entry(id_, entry.attrs, **kwargs)

    def update_misc(self):
        # 1 - fix objectclass for scim and config-api where it has lowecased objectclass instead of objectClass
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

        # create the index for query
        self.client.exec_query(f'CREATE INDEX `def_jans_fix_oc` ON `{bucket}`(`objectclass`)')

        # get all scopes that has objectclass instead of objectClass
        req = self.client.exec_query(f"SELECT META().id, {bucket}.* FROM {bucket} WHERE `objectclass` IS NOT MISSING")
        if req.ok:
            resp = req.json()
            for doc in resp["results"]:
                id_ = doc.pop("id")
                doc["objectClass"] = doc["objectclass"][-1]
                self.modify_entry(id_, doc, **{"bucket": bucket})
                # remove the objectclass attribute so the query above wont return results
                self.modify_entry(id_, {"objectclass": []}, **{"bucket": bucket, "delete_attr": True})

        # drop the index
        self.client.exec_query(f'DROP INDEX `{bucket}`.`def_jans_fix_oc`')

    def update_base_entries(self):
        # add jansManagerGrp to base entry
        id_ = id_from_dn(JANS_BASE_ID)
        bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        kwargs = {"bucket": bucket}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if not entry.attrs.get("jansManagerGrp"):
            entry.attrs["jansManagerGrp"] = JANS_MANAGER_GROUP
            self.modify_entry(id_, entry.attrs, **kwargs)


class SpannerBackend(BaseBackend):
    def __init__(self, manager):
        super().__init__()
        self.manager = manager
        self.client = SpannerClient()
        self.type = "spanner"

    def get_entry(self, key, filter_="", attrs=None, **kwargs):
        table_name = kwargs.get("table_name")
        entry = self.client.get(table_name, key, attrs)

        if not entry:
            return None
        return Entry(key, entry)

    def modify_entry(self, key, attrs=None, **kwargs):
        attrs = attrs or {}
        table_name = kwargs.get("table_name")
        return self.client.update(table_name, key, attrs), ""

    def update_people_entries(self):
        # add jansAdminUIRole to default admin user
        admin_inum = self.manager.config.get("admin_inum")
        id_ = doc_id_from_dn(f"inum={admin_inum},ou=people,o=jans")
        kwargs = {"table_name": "jansPerson"}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        # sql entry may have empty jansAdminUIRole hash ({"v": []})
        if not entry.attrs["jansAdminUIRole"]:
            entry.attrs["jansAdminUIRole"] = ["api-admin"]
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scopes_entries(self):
        # add jansAdminUIRole claim to profile scope
        id_ = doc_id_from_dn(self.jans_admin_ui_role_id)
        kwargs = {"table_name": "jansScope"}

        entry = self.get_entry(id_, **kwargs)

        if not entry:
            return

        if self.jans_admin_ui_claim not in entry.attrs["jansClaim"]:
            entry.attrs["jansClaim"].append(self.jans_admin_ui_claim)
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_clients_entries(self):
        jca_client_id = self.manager.config.get("jca_client_id")
        id_ = doc_id_from_dn(f"inum={jca_client_id},ou=clients,o=jans")
        kwargs = {"table_name": "jansClnt"}

        entry = self.get_entry(id_, **kwargs)

        if not entry:
            return

        should_update = False

        # modify redirect UI of config-api client
        hostname = self.manager.config.get("hostname")

        if f"https://{hostname}/admin" not in entry.attrs["jansRedirectURI"]:
            entry.attrs["jansRedirectURI"].append(f"https://{hostname}/admin")
            should_update = True

        # add jans_stat, SCIM users.read, SCIM users.write scopes to config-api client
        for scope in (self.jans_scim_scopes + self.jans_stat_scopes):
            if scope not in entry.attrs["jansScope"]:
                entry.attrs["jansScope"].append(scope)
                should_update = True

        if should_update:
            self.modify_entry(id_, entry.attrs, **kwargs)

    def update_scim_scopes_entries(self):
        # add jansAttrs to SCIM users.read and users.write scopes
        ids = [doc_id_from_dn(scope) for scope in self.jans_scim_scopes]
        kwargs = {"table_name": "jansScope"}

        for id_ in ids:
            entry = self.get_entry(id_, **kwargs)
            if not entry:
                continue

            if "jansAttrs" not in entry.attrs:
                entry.attrs[
                    "jansAttrs"] = self.jans_attrs
                self.modify_entry(id_, entry.attrs, **kwargs)

    def update_base_entries(self):
        # add jansManagerGrp to base entry
        id_ = doc_id_from_dn(JANS_BASE_ID)
        kwargs = {"table_name": "jansOrganization"}

        entry = self.get_entry(id_, **kwargs)
        if not entry:
            return

        if not entry.attrs.get("jansManagerGrp"):
            entry.attrs["jansManagerGrp"] = JANS_MANAGER_GROUP
            self.modify_entry(id_, entry.attrs, **kwargs)


class Upgrade:
    def __init__(self, manager):
        self.manager = manager

        persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        if persistence_type == "sql":
            backend_cls = SQLBackend
        elif persistence_type == "couchbase":
            backend_cls = CouchbaseBackend
        elif persistence_type == "spanner":
            backend_cls = SpannerBackend
        else:
            backend_cls = LDAPBackend
        self.backend = backend_cls(manager)

    def invoke(self):
        logger.info("Running upgrade process (if required)")
        self.backend.update_people_entries()
        self.backend.update_scopes_entries()
        self.backend.update_clients_entries()
        self.backend.update_scim_scopes_entries()
        self.backend.update_base_entries()

        if hasattr(self.backend, "update_misc"):
            self.backend.update_misc()
