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

"""Command-line applications for Launchpadlib.

This module contains the code for various applications. The applications
themselves are kept in bin/.
"""

__all__ = [
    'RequestTokenApp',
    'TrustedTokenAuthorizationConsoleApp',
    ]

import getpass
import sys

import simplejson

from launchpadlib.credentials import (
    Credentials, RequestTokenAuthorizationEngine, TokenAuthorizationException)
from launchpadlib.uris import lookup_web_root


class RequestTokenApp(object):
    """An application that creates request tokens."""

    def __init__(self, web_root, consumer_name, context):
        """Initialize."""
        self.web_root = lookup_web_root(web_root)
        self.credentials = Credentials(consumer_name)
        self.context = context

    def run(self):
        """Get a request token and return JSON information about it."""
        token = self.credentials.get_request_token(
            self.context, self.web_root,
            token_format=Credentials.DICT_TOKEN_FORMAT)
        return simplejson.dumps(token)


class TrustedTokenAuthorizationConsoleApp(RequestTokenAuthorizationEngine):
    """An application that authorizes request tokens."""

    def __init__(self, web_root, consumer_name, request_token,
                 access_levels='', input_method=raw_input):
        """Constructor.

        :param access_levels: A string of comma-separated access level
        values.  To get an up-to-date list of access levels, pass
        token_format=Credentials.DICT_TOKEN_FORMAT into
        Credentials.get_request_token, load the dict as JSON, and look
        in 'access_levels'.
        """
        access_levels = [level.strip() for level in access_levels.split(',')]
        super(TrustedTokenAuthorizationConsoleApp, self).__init__(
            web_root, consumer_name, request_token, access_levels)

        self.input_method = input_method

    def run(self):
        """Try to authorize a request token from user input."""
        self.error_code = -1 # Start off assuming failure.
        start = "Launchpad credential client (console)"
        self.output(start)
        self.output("-" * len(start))

        try:
            self()
        except TokenAuthorizationException, e:
            print str(e)
            self.error_code = -1
        return self.press_enter_to_exit()

    def exit_with(self, code):
        """Exit the app with the specified error code."""
        sys.exit(code)

    def get_single_char_input(self, prompt, valid):
        """Retrieve a single-character line from the input stream."""
        valid = valid.upper()
        input = None
        while input is None:
            input = self.input_method(prompt).upper()
            if len(input) != 1 or input not in valid:
                input = None
        return input

    def press_enter_to_exit(self):
        """Make the user hit enter, and then exit with an error code."""
        prompt = '\nPress enter to go back to "%s". ' % self.consumer_name
        self.input_method(prompt)
        self.exit_with(self.error_code)

    def input_username(self, cached_username, suggested_message):
        """Collect the Launchpad username from the end-user.

        :param cached_username: A username from a previous entry attempt,
        to be presented as the default.
        """
        if cached_username is not None:
            extra = " [%s] " % cached_username
        else:
            extra = "\n(No Launchpad account? Just hit enter.) "
        username = self.input_method(suggested_message + extra)
        if username == '':
            return cached_username
        return username

    def input_password(self, suggested_message):
        """Collect the Launchpad password from the end-user."""
        if self.input_method is raw_input:
            password = getpass.getpass(suggested_message + " ")
        else:
            password = self.input_method(suggested_message)
        return password

    def input_access_level(self, available_levels, suggested_message,
                           only_one_option=None):
        """Collect the desired level of access from the end-user."""
        if only_one_option is not None:
            self.output(suggested_message)
            prompt = self.message(
                'Do you want to give "%(app)s" this level of access? [YN] ')
            allow = self.get_single_char_input(prompt, "YN")
            if allow == "Y":
                return only_one_option['value']
            else:
                return self.UNAUTHORIZED_ACCESS_LEVEL
        else:
            levels_except_unauthorized = [
                level for level in available_levels
                if level['value'] != self.UNAUTHORIZED_ACCESS_LEVEL]
            options = []
            for i in range(0, len(levels_except_unauthorized)):
                options.append(
                    "%d: %s" % (i+1, levels_except_unauthorized[i]['title']))
            self.output(suggested_message)
            for option in options:
                self.output(option)
            allowed = ("".join(map(str, range(1, i+2)))) + "Q"
            prompt = self.message(
                'What should "%(app)s" be allowed to do using your '
                'Launchpad account? [1-%(max)d or Q] ',
                extra_variables = {'max' : i+1})
            allow = self.get_single_char_input(prompt, allowed)
            if allow == "Q":
                return self.UNAUTHORIZED_ACCESS_LEVEL
            else:
                return levels_except_unauthorized[int(allow)-1]['value']

    def user_refused_to_authorize(self, suggested_message):
        """The user refused to authorize a request token."""
        self.output(suggested_message)
        self.error_code = -2

    def user_authorized(self, access_level, suggested_message):
        """The user authorized a request token with some access level."""
        self.output(suggested_message)
        self.error_code = 0
