# Copyright 2009 Canonical Ltd.

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

"""Tests for the Launchpad class."""

__metaclass__ = type

import os
import shutil
import stat
import tempfile
import unittest

from launchpadlib.credentials import (
    AccessToken, AuthorizeRequestTokenWithBrowser, Credentials)
from launchpadlib.launchpad import Launchpad
from launchpadlib import uris

class NoNetworkLaunchpad(Launchpad):
    """A Launchpad instance for tests with no network access.

    It's only useful for making sure that certain methods were called.
    It can't be used to interact with the API.
    """

    consumer_name = None
    passed_in_kwargs = None
    credentials = None
    get_token_and_login_called = False

    def __init__(self, credentials, **kw):
        self.credentials = credentials
        self.passed_in_kwargs = kw

    @classmethod
    def get_token_and_login(cls, consumer_name, **kw):
        """Create fake credentials and record that we were called."""
        credentials = Credentials(
            consumer_name, consumer_secret='consumer_secret:42',
            access_token=AccessToken('access_key:84', 'access_secret:168'))
        launchpad = cls(credentials, **kw)
        launchpad.get_token_and_login_called = True
        launchpad.consumer_name = consumer_name
        launchpad.passed_in_kwargs = kw
        return launchpad


class TestNameLookups(unittest.TestCase):
    """Test the utility functions in the 'uris' module."""

    def setUp(self):
        self.aliases = sorted(
            ['production', 'edge', 'staging', 'dogfood', 'dev', 'test_dev'])

    def test_short_names(self):
        # Ensure the short service names are all supported.
        self.assertEqual(sorted(uris.service_roots.keys()), self.aliases)
        self.assertEqual(sorted(uris.web_roots.keys()), self.aliases)

    def test_lookups(self):
        """Ensure that short service names turn into long service names."""

        # If the service name is a known alias, lookup methods convert
        # it to a URL.
        for alias in self.aliases:
            self.assertEqual(
                uris.lookup_service_root(alias), uris.service_roots[alias])
            self.assertEqual(
                uris.lookup_web_root(alias), uris.web_roots[alias])

        # If the service name is a valid URL, lookup methods let it
        # through.
        other_root = "http://some-other-server.com"
        self.assertEqual(uris.lookup_service_root(other_root), other_root)
        self.assertEqual(uris.lookup_web_root(other_root), other_root)

        # Otherwise, lookup methods raise an exception.
        not_a_url = "not-a-url"
        self.assertRaises(ValueError, uris.lookup_service_root, not_a_url)
        self.assertRaises(ValueError, uris.lookup_web_root, not_a_url)


class TestServiceNameWithEmbeddedVersion(unittest.TestCase):
    """Reject service roots that include the version at the end of the URL.

    If the service root is "http://api.launchpad.net/beta/" and the
    version is "beta", the launchpadlib constructor will raise an
    exception.

    This happens with scripts that were written against old versions
    of launchpadlib. The alternative is to try to silently fix it (the
    fix will eventually break as new versions of the web service are
    released) or to go ahead and make a request to
    http://api.launchpad.net/beta/beta/, and cause an unhelpful 404
    error.
    """

    def test_service_name_with_embedded_version(self):
        # Basic test. If there were no exception raised here,
        # launchpadlib would make a request to
        # /version-foo/version-foo.
        version = "version-foo"
        root = uris.service_roots['staging'] + version
        try:
            Launchpad(None, root, version=version)
        except ValueError, e:
            self.assertTrue(str(e).startswith(
                "It looks like you're using a service root that incorporates "
                'the name of the web service version ("version-foo")'))
        else:
            raise AssertionError(
                "Expected a ValueError that was not thrown!")

        # Make sure the problematic URL is caught even if it has a
        # slash on the end.
        root += '/'
        self.assertRaises(ValueError, Launchpad, None, root, version=version)

        # Test that the default version has the same problem
        # when no explicit version is specified
        default_version = NoNetworkLaunchpad.DEFAULT_VERSION
        root = uris.service_roots['staging'] + default_version + '/'
        self.assertRaises(ValueError, Launchpad, None, root)


class TestLaunchpadLoginWith(unittest.TestCase):
    """Tests for Launchpad.login_with()."""

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_dirs_created(self):
        # The path we pass into login_with() is the directory where
        # cache and credentials for all service roots are stored.
        launchpadlib_dir = os.path.join(self.temp_dir, 'launchpadlib')
        launchpad = NoNetworkLaunchpad.login_with(
            'not important', service_root='http://api.example.com/beta',
            launchpadlib_dir=launchpadlib_dir)
        # The 'launchpadlib' dir got created.
        self.assertTrue(os.path.isdir(launchpadlib_dir))
        # A directory for the passed in service root was created.
        service_path = os.path.join(launchpadlib_dir, 'api.example.com')
        self.assertTrue(os.path.isdir(service_path))
        # Inside the service root directory, there is a 'cache' and a
        # 'credentials' directory.
        self.assertTrue(
            os.path.isdir(os.path.join(service_path, 'cache')))
        credentials_path = os.path.join(service_path, 'credentials')
        self.assertTrue(os.path.isdir(credentials_path))

    def test_dirs_created_are_changed_to_secure(self):
        launchpadlib_dir = os.path.join(self.temp_dir, 'launchpadlib')
        # Verify a newly created-by-hand directory is insecure
        os.mkdir(launchpadlib_dir)
        os.chmod(launchpadlib_dir, 0755)
        self.assertTrue(os.path.isdir(launchpadlib_dir))
        statinfo = os.stat(launchpadlib_dir)
        mode = stat.S_IMODE(statinfo.st_mode)
        self.assertNotEqual(mode, stat.S_IWRITE | stat.S_IREAD | stat.S_IEXEC)
        launchpad = NoNetworkLaunchpad.login_with(
            'not important', service_root='http://api.example.com/beta',
            launchpadlib_dir=launchpadlib_dir)
        # Verify the mode has been changed to 0700
        statinfo = os.stat(launchpadlib_dir)
        mode = stat.S_IMODE(statinfo.st_mode)
        self.assertEqual(mode, stat.S_IWRITE | stat.S_IREAD | stat.S_IEXEC)

    def test_dirs_created_are_secure(self):
        launchpadlib_dir = os.path.join(self.temp_dir, 'launchpadlib')
        launchpad = NoNetworkLaunchpad.login_with(
            'not important', service_root='http://api.example.com/beta',
            launchpadlib_dir=launchpadlib_dir)
        self.assertTrue(os.path.isdir(launchpadlib_dir))
        # Verify the mode is safe
        statinfo = os.stat(launchpadlib_dir)
        mode = stat.S_IMODE(statinfo.st_mode)
        self.assertEqual(mode, stat.S_IWRITE | stat.S_IREAD | stat.S_IEXEC)

    def test_version_is_propagated(self):
        # Make sure the login_with() method conveys the 'version'
        # argument all the way to the Launchpad object. The
        # credentials will be cached to disk.
        launchpadlib_dir = os.path.join(self.temp_dir, 'launchpadlib')
        launchpad = NoNetworkLaunchpad.login_with(
            'not important', service_root='http://api.example.com/',
            launchpadlib_dir=launchpadlib_dir, version="foo")
        self.assertEquals(launchpad.passed_in_kwargs['version'], 'foo')

        # Now execute the same test a second time. This time, the
        # credentials are loaded from disk and a different code path
        # is executed. We want to make sure this code path propagates
        # the 'version' argument.
        launchpad = NoNetworkLaunchpad.login_with(
            'not important', service_root='http://api.example.com/',
            launchpadlib_dir=launchpadlib_dir, version="bar")
        self.assertEquals(launchpad.passed_in_kwargs['version'], 'bar')

    def test_no_credentials_calls_get_token_and_login(self):
        # If no credentials are found, get_token_and_login() is called.
        service_root = 'http://api.example.com/beta'
        timeout = object()
        proxy_info = object()
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            service_root=service_root, timeout=timeout, proxy_info=proxy_info)
        self.assertEqual(launchpad.consumer_name, 'app name')
        expected_arguments = dict(
            allow_access_levels=[],
            authorizer_class=AuthorizeRequestTokenWithBrowser,
            max_failed_attempts=3,
            service_root=service_root,
            timeout=timeout,
            proxy_info=proxy_info,
            cache=os.path.join(self.temp_dir, 'api.example.com', 'cache'),
            version='beta')
        self.assertEqual(launchpad.passed_in_kwargs, expected_arguments)

    def test_anonymous_login(self):
        """Test the anonymous login helper function."""
        launchpad = NoNetworkLaunchpad.login_anonymously(
            'anonymous access', launchpadlib_dir=self.temp_dir,
            service_root='http://api.example.com/beta')
        self.assertEqual(launchpad.credentials.access_token.key, '')
        self.assertEqual(launchpad.credentials.access_token.secret, '')

        # Test that anonymous credentials are not saved.
        credentials_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials',
            'anonymous access')
        self.assertFalse(os.path.exists(credentials_path))

    def test_new_credentials_are_saved(self):
        # After get_token_and_login() have been called, the created
        # credentials are saved.
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            service_root='http://api.example.com/beta')
        credentials_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials', 'app name')
        self.assertTrue(os.path.exists(credentials_path))
        # Make sure that the credentials can be loaded, thus were
        # written correctly.
        loaded_credentials = Credentials.load_from_path(credentials_path)
        self.assertEqual(loaded_credentials.consumer.key, 'app name')
        self.assertEqual(
            loaded_credentials.consumer.secret, 'consumer_secret:42')
        self.assertEqual(
            loaded_credentials.access_token.key, 'access_key:84')
        self.assertEqual(
            loaded_credentials.access_token.secret, 'access_secret:168')

    def test_new_credentials_are_secure(self):
        # The newly created credentials file is only readable and
        # writable by the user.
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            service_root='http://api.example.com/beta')
        credentials_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials', 'app name')
        statinfo = os.stat(credentials_path)
        mode = stat.S_IMODE(statinfo.st_mode)
        self.assertEqual(mode, stat.S_IWRITE | stat.S_IREAD)

    def test_existing_credentials_are_reused(self):
        # If a credential file for the application already exists, that
        # one is used.
        os.makedirs(
            os.path.join(self.temp_dir, 'api.example.com', 'credentials'))
        credentials_file_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials', 'app name')
        credentials = Credentials(
            'app name', consumer_secret='consumer_secret:42',
            access_token=AccessToken('access_key:84', 'access_secret:168'))
        credentials.save_to_path(credentials_file_path)

        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            service_root='http://api.example.com/beta')
        self.assertFalse(launchpad.get_token_and_login_called)
        self.assertEqual(launchpad.credentials.consumer.key, 'app name')
        self.assertEqual(
            launchpad.credentials.consumer.secret, 'consumer_secret:42')
        self.assertEqual(
            launchpad.credentials.access_token.key, 'access_key:84')
        self.assertEqual(
            launchpad.credentials.access_token.secret, 'access_secret:168')

    def test_existing_credentials_arguments_passed_on(self):
        # When re-using existing credentials, the arguments login_with
        # is called with are passed on the the __init__() method.
        os.makedirs(
            os.path.join(self.temp_dir, 'api.example.com', 'credentials'))
        credentials_file_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials', 'app name')
        credentials = Credentials(
            'app name', consumer_secret='consumer_secret:42',
            access_token=AccessToken('access_key:84', 'access_secret:168'))
        credentials.save_to_path(credentials_file_path)

        service_root = 'http://api.example.com/'
        timeout = object()
        proxy_info = object()
        version = "foo"
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            service_root=service_root, timeout=timeout, proxy_info=proxy_info,
            version=version)
        expected_arguments = dict(
            service_root=service_root,
            timeout=timeout,
            proxy_info=proxy_info,
            version=version,
            cache=os.path.join(self.temp_dir, 'api.example.com', 'cache'))
        self.assertEqual(launchpad.passed_in_kwargs, expected_arguments)

    def test_None_launchpadlib_dir(self):
        # If no launchpadlib_dir is passed in to login_with,
        # $HOME/.launchpadlib is used.
        old_home = os.environ['HOME']
        os.environ['HOME'] = self.temp_dir
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', service_root='http://api.example.com/beta')
        # Reset the environment to the old value.
        os.environ['HOME'] = old_home

        cache_dir = launchpad.passed_in_kwargs['cache']
        launchpadlib_dir = os.path.abspath(
            os.path.join(cache_dir, '..', '..'))
        self.assertEqual(
            launchpadlib_dir, os.path.join(self.temp_dir, '.launchpadlib'))
        self.assertTrue(os.path.exists(
            os.path.join(launchpadlib_dir, 'api.example.com', 'cache')))
        self.assertTrue(os.path.exists(
            os.path.join(launchpadlib_dir, 'api.example.com', 'credentials')))

    def test_short_service_name(self):
        # A short service name is converted to the full service root URL.
        launchpad = NoNetworkLaunchpad.login_with('app name', 'staging')
        self.assertEqual(
            launchpad.passed_in_kwargs['service_root'],
            'https://api.staging.launchpad.net/')

        # A full URL as the service name is left alone.
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', uris.service_roots['staging'])
        self.assertEqual(
            launchpad.passed_in_kwargs['service_root'],
            uris.service_roots['staging'])

        # A short service name that does not match one of the
        # pre-defined service root names, and is not a valid URL,
        # raises an exception.
        launchpad = ('app name', 'https://')
        self.assertRaises(
            ValueError, NoNetworkLaunchpad.login_with, 'app name', 'foo')

    def test_separate_credentials_file(self):
        my_credentials_path = os.path.join(self.temp_dir, 'my_creds') 
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            credentials_file=my_credentials_path,
            service_root='http://api.example.com/beta')
        default_credentials_path = os.path.join(
            self.temp_dir, 'api.example.com', 'credentials', 'app name')
        self.assertFalse(os.path.exists(default_credentials_path))
        self.assertTrue(os.path.exists(my_credentials_path))

        self.assertTrue(launchpad.get_token_and_login_called)

        # gets reused, too
        launchpad = NoNetworkLaunchpad.login_with(
            'app name', launchpadlib_dir=self.temp_dir,
            credentials_file=my_credentials_path,
            service_root='http://api.example.com/beta')
        self.assertFalse(os.path.exists(default_credentials_path))
        self.assertTrue(os.path.exists(my_credentials_path))
        self.assertFalse(launchpad.get_token_and_login_called)


def test_suite():
    return unittest.TestLoader().loadTestsFromName(__name__)
