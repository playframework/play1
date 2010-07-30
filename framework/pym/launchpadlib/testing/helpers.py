# Copyright 2008 Canonical Ltd.

# This file is part of launchpadlib.
#
# launchpadlib is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# launchpadlib is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with launchpadlib.  If not, see
# <http://www.gnu.org/licenses/>.

"""launchpadlib testing helpers."""


__metaclass__ = type
__all__ = [
    'TestableLaunchpad',
    'nopriv_read_nonprivate',
    'salgado_read_nonprivate',
    'salgado_with_full_permissions',
    ]

import simplejson

from launchpadlib.launchpad import Launchpad
from launchpadlib.credentials import (
    AuthorizeRequestTokenWithBrowser, Credentials,
    RequestTokenAuthorizationEngine, SimulatedLaunchpadBrowser)


class TestableLaunchpad(Launchpad):
    """A base class for talking to the testing root service."""

    def __init__(self, credentials, service_root=None,
                 cache=None, timeout=None, proxy_info=None,
                 version=Launchpad.DEFAULT_VERSION):
        super(TestableLaunchpad, self).__init__(
            credentials, 'test_dev', cache, timeout, proxy_info,
            version=version)


class KnownTokens:
    """Known access token/secret combinations."""

    def __init__(self, token_string, access_secret):
        self.token_string = token_string
        self.access_secret = access_secret

    def login(self, cache=None, timeout=None, proxy_info=None,
              version=Launchpad.DEFAULT_VERSION):
        """Login using these credentials."""
        return TestableLaunchpad.login(
            'launchpad-library', self.token_string, self.access_secret,
            cache=cache, timeout=timeout, proxy_info=proxy_info,
            version=version)


salgado_with_full_permissions = KnownTokens('salgado-change-anything', 'test')
salgado_read_nonprivate = KnownTokens('salgado-read-nonprivate', 'secret')
nopriv_read_nonprivate = KnownTokens('nopriv-read-nonprivate', 'mystery')


class ScriptableRequestTokenAuthorization(RequestTokenAuthorizationEngine):
    """A request token process that doesn't need any user input.

    The RequestTokenAuthorizationEngine is supposed to be hooked up to a
    user interface, but that makes it difficult to test. This subclass
    is designed to be easy to test.
    """

    def __init__(self, consumer_name, username, password, choose_access_level,
                 allow_access_levels=[], max_failed_attempts=2,
                 web_root="http://launchpad.dev:8085/"):

        # Get a request token.
        self.credentials = Credentials(consumer_name)
        self.credentials.get_request_token(web_root=web_root)

        # Initialize the superclass with the new request token.
        super(ScriptableRequestTokenAuthorization, self).__init__(
            web_root, consumer_name, self.credentials._request_token.key,
                allow_access_levels, max_failed_attempts)

        self.username = username
        self.password = password
        self.choose_access_level = choose_access_level

    def __call__(self, exchange_for_access_token=True):
        super(ScriptableRequestTokenAuthorization, self).__call__()

        # Now verify that it worked by exchanging the authorized
        # request token for an access token.
        if (exchange_for_access_token and
            self.choose_access_level != self.UNAUTHORIZED_ACCESS_LEVEL):
            self.credentials.exchange_request_token_for_access_token(
                web_root=self.web_root)
            return self.credentials.access_token
        return None

    def open_page_in_user_browser(self, url):
        """Print a status message."""
        print ("[If this were a real application, the end-user's web "
               "browser would be opened to %s]" % url)

    def input_username(self, cached_username, suggested_message):
        """Collect the Launchpad username from the end-user."""
        print suggested_message
        if cached_username is not None:
            print "Cached email address: " + cached_username
        return self.username

    def input_password(self, suggested_message):
        """Collect the Launchpad password from the end-user."""
        print suggested_message
        return self.password

    def input_access_level(self, available_levels, suggested_message,
                           only_one_option):
        """Collect the desired level of access from the end-user."""
        print suggested_message
        print [level['value'] for level in available_levels]
        return self.choose_access_level

    def startup(self, suggested_messages):
        for message in suggested_messages:
            print message


class DummyAuthorizeRequestTokenWithBrowser(AuthorizeRequestTokenWithBrowser):

    def __init__(self, web_root, consumer_name, request_token, username,
                 password, allow_access_levels=[], max_failed_attempts=3):
        super(DummyAuthorizeRequestTokenWithBrowser, self).__init__(
            web_root, consumer_name, request_token, allow_access_levels,
            max_failed_attempts)

    def open_page_in_user_browser(self, url):
        """Print a status message."""
        print ("[If this were a real application, the end-user's web "
               "browser would be opened to %s]" % url)


class UserInput(object):
    """A class to store fake user input in a readable way.

    An instance of this class can be used as a substitute for the
    raw_input() function.
    """

    def __init__(self, inputs):
        """Initialize with a line of user inputs."""
        self.stream = iter(inputs)

    def __call__(self, prompt):
        """Print and return the next line of input."""
        line = self.readline()
        print prompt + "[User input: %s]" % line
        return line

    def readline(self):
        """Return the next line of input."""
        next_input = self.stream.next()
        return str(next_input)
