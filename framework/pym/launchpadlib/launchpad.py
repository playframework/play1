# Copyright 2008-2009 Canonical Ltd.

# This file is part of launchpadlib.
#
# launchpadlib is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published by the
# Free Software Foundation, version 3 of the License.
#
# launchpadlib is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
# for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with launchpadlib. If not, see <http://www.gnu.org/licenses/>.

"""Root Launchpad API class."""

__metaclass__ = type
__all__ = [
    'Launchpad',
    ]

import os
import stat
import urlparse

from lazr.uri import URI
from lazr.restfulclient.resource import (
    CollectionWithKeyBasedLookup, HostedFile, ServiceRoot)
from launchpadlib.credentials import (
    AccessToken, AnonymousAccessToken, Credentials,
    AuthorizeRequestTokenWithBrowser)
from launchpadlib import uris

# Import some constants for backwards compatibility. This way, old
# scripts that have 'from launchpad import EDGE_SERVICE_ROOT' will still
# work.
from launchpadlib.uris import EDGE_SERVICE_ROOT, STAGING_SERVICE_ROOT
OAUTH_REALM = 'https://api.launchpad.net'


class PersonSet(CollectionWithKeyBasedLookup):
    """A custom subclass capable of person lookup by username."""

    def _get_url_from_id(self, key):
        """Transform a username into the URL to a person resource."""
        return str(self._root._root_uri.ensureSlash()) + '~' + str(key)


class BugSet(CollectionWithKeyBasedLookup):
    """A custom subclass capable of bug lookup by bug ID."""

    def _get_url_from_id(self, key):
        """Transform a bug ID into the URL to a bug resource."""
        return str(self._root._root_uri.ensureSlash()) + 'bugs/' + str(key)


class PillarSet(CollectionWithKeyBasedLookup):
    """A custom subclass capable of lookup by pillar name.

    Projects, project groups, and distributions are all pillars.
    """

    def _get_url_from_id(self, key):
        """Transform a project name into the URL to a project resource."""
        return str(self._root._root_uri.ensureSlash()) + str(key)


class Launchpad(ServiceRoot):
    """Root Launchpad API class.

    :ivar credentials: The credentials instance used to access Launchpad.
    :type credentials: `Credentials`
    """

    DEFAULT_VERSION = '1.0'

    RESOURCE_TYPE_CLASSES = {
            'bugs': BugSet,
            'distributions': PillarSet,
            'HostedFile': HostedFile,
            'people': PersonSet,
            'project_groups': PillarSet,
            'projects': PillarSet,
            }

    def __init__(self, credentials, service_root=uris.STAGING_SERVICE_ROOT,
                 cache=None, timeout=None, proxy_info=None,
                 version=DEFAULT_VERSION):
        """Root access to the Launchpad API.

        :param credentials: The credentials used to access Launchpad.
        :type credentials: `Credentials`
        :param service_root: The URL to the root of the web service.
        :type service_root: string
        """
        service_root = uris.lookup_service_root(service_root)
        if (service_root.endswith(version)
            or service_root.endswith(version + '/')):
            error = ("It looks like you're using a service root that "
                     "incorporates the name of the web service version "
                     '("%s"). Please use one of the constants from '
                     "launchpadlib.uris instead, or at least remove "
                     "the version name from the root URI." % version)
            raise ValueError(error)

        super(Launchpad, self).__init__(
            credentials, service_root, cache, timeout, proxy_info, version)

    @classmethod
    def login(cls, consumer_name, token_string, access_secret,
              service_root=uris.STAGING_SERVICE_ROOT,
              cache=None, timeout=None, proxy_info=None,
              version=DEFAULT_VERSION):
        """Convenience for setting up access credentials.

        When all three pieces of credential information (the consumer
        name, the access token and the access secret) are available, this
        method can be used to quickly log into the service root.

        :param consumer_name: the consumer name, as appropriate for the
            `Consumer` constructor
        :type consumer_name: string
        :param token_string: the access token, as appropriate for the
            `AccessToken` constructor
        :type token_string: string
        :param access_secret: the access token's secret, as appropriate for
            the `AccessToken` constructor
        :type access_secret: string
        :param service_root: The URL to the root of the web service.
        :type service_root: string
        :return: The web service root
        :rtype: `Launchpad`
        """
        access_token = AccessToken(token_string, access_secret)
        credentials = Credentials(
            consumer_name=consumer_name, access_token=access_token)
        return cls(credentials, service_root, cache, timeout, proxy_info,
                   version)

    @classmethod
    def get_token_and_login(cls, consumer_name,
                            service_root=uris.STAGING_SERVICE_ROOT,
                            cache=None, timeout=None, proxy_info=None,
                            authorizer_class=AuthorizeRequestTokenWithBrowser,
                            allow_access_levels=[], max_failed_attempts=3,
                            version=DEFAULT_VERSION):
        """Get credentials from Launchpad and log into the service root.

        This is a convenience method which will open up the user's preferred
        web browser and thus should not be used by most applications.
        Applications should, instead, use Credentials.get_request_token() to
        obtain the authorization URL and
        Credentials.exchange_request_token_for_access_token() to obtain the
        actual OAuth access token.

        This method will negotiate an OAuth access token with the service
        provider, but to complete it we will need the user to log into
        Launchpad and authorize us, so we'll open the authorization page in
        a web browser and ask the user to come back here and tell us when they
        finished the authorization process.

        :param consumer_name: The consumer name, as appropriate for the
            `Consumer` constructor
        :type consumer_name: string
        :param service_root: The URL to the root of the web service.
        :type service_root: string
        :return: The web service root
        :rtype: `Launchpad`
        """
        credentials = Credentials(consumer_name)
        service_root = uris.lookup_service_root(service_root)
        web_root_uri = URI(service_root)
        web_root_uri.path = ""
        web_root_uri.host = web_root_uri.host.replace("api.", "", 1)
        web_root = str(web_root_uri.ensureSlash())
        authorization_json = credentials.get_request_token(
            web_root=web_root, token_format=Credentials.DICT_TOKEN_FORMAT)
        authorizer = authorizer_class(
            web_root, authorization_json['oauth_token_consumer'],
            authorization_json['oauth_token'], allow_access_levels,
            max_failed_attempts)
        authorizer()
        credentials.exchange_request_token_for_access_token(web_root)
        return cls(credentials, service_root, cache, timeout, proxy_info,
                   version)

    @classmethod
    def login_anonymously(
        cls, consumer_name, service_root=uris.STAGING_SERVICE_ROOT,
        launchpadlib_dir=None, timeout=None, proxy_info=None,
        version=DEFAULT_VERSION):
        """Get access to Launchpad without providing any credentials."""
        (service_root, launchpadlib_dir, cache_path,
         service_root_dir) = cls._get_paths(service_root, launchpadlib_dir)
        token = AnonymousAccessToken()
        credentials = Credentials(consumer_name, access_token=token)
        return cls(credentials, service_root=service_root, cache=cache_path,
                   timeout=timeout, proxy_info=proxy_info, version=version)

    @classmethod
    def login_with(cls, consumer_name,
                   service_root=uris.STAGING_SERVICE_ROOT,
                   launchpadlib_dir=None, timeout=None, proxy_info=None,
                   authorizer_class=AuthorizeRequestTokenWithBrowser,
                   allow_access_levels=[], max_failed_attempts=3,
                   credentials_file=None, version=DEFAULT_VERSION):
        """Log in to Launchpad with possibly cached credentials.

        This is a convenience method for either setting up new login
        credentials, or re-using existing ones. When a login token is generated
        using this method, the resulting credentials will be saved in
        `credentials_file`, or if not given, into the `launchpadlib_dir`
        directory. If the same `credentials_file`/`launchpadlib_dir` is passed
        in a second time, the credentials in for the consumer will be used
        automatically.

        Each consumer has their own credentials per service root in
        `launchpadlib_dir`. `launchpadlib_dir` is also used for caching
        fetched objects. The cache is per service root, and shared by
        all consumers.

        See `Launchpad.get_token_and_login()` for more information about
        how new tokens are generated.

        :param consumer_name: The consumer name, as appropriate for the
            `Consumer` constructor
        :type consumer_name: string
        :param service_root: The URL to the root of the web service.
        :type service_root: string.  Can either be the full URL to a service
            or one of the short service names.
        :param launchpadlib_dir: The directory where the cache and
            credentials are stored.
        :type launchpadlib_dir: string
        :param credentials_file: If given, the credentials are stored in that
            file instead in `launchpadlib_dir`.
        :type credentials_file: string
        :return: The web service root
        :rtype: `Launchpad`

        """
        (service_root, launchpadlib_dir, cache_path,
         service_root_dir) = cls._get_paths(service_root, launchpadlib_dir)
        credentials_path = os.path.join(service_root_dir, 'credentials')
        if not os.path.exists(credentials_path):
            os.makedirs(credentials_path)
        if credentials_file is None:
            consumer_credentials_path = os.path.join(credentials_path,
                consumer_name)
        else:
            consumer_credentials_path = credentials_file
        if os.path.exists(consumer_credentials_path):
            credentials = Credentials.load_from_path(
                consumer_credentials_path)
            launchpad = cls(
                credentials, service_root=service_root, cache=cache_path,
                timeout=timeout, proxy_info=proxy_info, version=version)
        else:
            launchpad = cls.get_token_and_login(
                consumer_name, service_root=service_root, cache=cache_path,
                timeout=timeout, proxy_info=proxy_info,
                authorizer_class=authorizer_class,
                allow_access_levels=allow_access_levels,
                max_failed_attempts=max_failed_attempts, version=version)
            launchpad.credentials.save_to_path(consumer_credentials_path)
            os.chmod(consumer_credentials_path, stat.S_IREAD | stat.S_IWRITE)
        return launchpad

    @classmethod
    def _get_paths(cls, service_root, launchpadlib_dir=None):
        """Locate launchpadlib-related user paths and ensure they exist.

        This is a helper function used by login_with() and
        login_anonymously().

        :param service_root: The service root the user wants to
            connect to. This may be an alias (which will be
            dereferenced to a URL and returned) or a URL (which will
            be returned as is).
        :param launchpadlib_dir: The user's base launchpadlib
            directory, if known. This may be modified, expanded, or
            determined from the environment if missing. A definitive
            value will be returned.

        :return: A 4-tuple:
            (service_root_uri, launchpadlib_dir, cache_dir, service_root_dir)
        """
        if launchpadlib_dir is None:
            home_dir = os.environ['HOME']
            launchpadlib_dir = os.path.join(home_dir, '.launchpadlib')
        launchpadlib_dir = os.path.expanduser(launchpadlib_dir)
        if not os.path.exists(launchpadlib_dir):
            os.makedirs(launchpadlib_dir,0700)
        os.chmod(launchpadlib_dir,0700)
        # Determine the real service root.
        service_root = uris.lookup_service_root(service_root)
        # Each service root has its own cache and credential dirs.
        scheme, host_name, path, query, fragment = urlparse.urlsplit(
            service_root)
        service_root_dir = os.path.join(launchpadlib_dir, host_name)
        cache_path = os.path.join(service_root_dir, 'cache')
        if not os.path.exists(cache_path):
            os.makedirs(cache_path)
        return (service_root, launchpadlib_dir, cache_path, service_root_dir)
