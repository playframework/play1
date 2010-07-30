# Copyright 2008 Canonical Ltd.

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

"""launchpadlib credentials and authentication support."""

__metaclass__ = type
__all__ = [
    'AccessToken',
    'AnonymousAccessToken',
    'RequestTokenAuthorizationEngine',
    'Consumer',
    'Credentials',
    ]

import base64
import cgi
import httplib2
import sys
import textwrap
from urllib import urlencode, quote
from urlparse import urljoin
import webbrowser

import simplejson

from lazr.restfulclient.errors import HTTPError
from lazr.restfulclient.authorize.oauth import (
    AccessToken as _AccessToken, Consumer, OAuthAuthorizer)

from launchpadlib import uris

request_token_page = '+request-token'
access_token_page = '+access-token'
authorize_token_page = '+authorize-token'


class Credentials(OAuthAuthorizer):
    """Standard credentials storage and usage class.

    :ivar consumer: The consumer (application)
    :type consumer: `Consumer`
    :ivar access_token: Access information on behalf of the user
    :type access_token: `AccessToken`
    """
    _request_token = None

    URI_TOKEN_FORMAT = "uri"
    DICT_TOKEN_FORMAT = "dict"

    def get_request_token(self, context=None, web_root=uris.STAGING_WEB_ROOT,
                          token_format=URI_TOKEN_FORMAT):
        """Request an OAuth token to Launchpad.

        Also store the token in self._request_token.

        This method must not be called on an object with no consumer
        specified or if an access token has already been obtained.

        :param context: The context of this token, that is, its scope of
            validity within Launchpad.
        :param web_root: The URL of the website on which the token
            should be requested.
        :token_format: How the token should be
            presented. URI_TOKEN_FORMAT means just return the URL to
            the page that authorizes the token.  DICT_TOKEN_FORMAT
            means return a dictionary describing the token
            and the site's authentication policy.

        :return: If token_format is URI_TOKEN_FORMAT, the URL for the
            user to authorize the `AccessToken` provided by
            Launchpad. If token_format is DICT_TOKEN_FORMAT, a dict of
            information about the new access token.
        """
        assert self.consumer is not None, "Consumer not specified."
        assert self.access_token is None, "Access token already obtained."
        web_root = uris.lookup_web_root(web_root)
        params = dict(
            oauth_consumer_key=self.consumer.key,
            oauth_signature_method='PLAINTEXT',
            oauth_signature='&')
        url = web_root + request_token_page
        headers = {'Referer' : web_root}
        if token_format == self.DICT_TOKEN_FORMAT:
            headers['Accept'] = 'application/json'
        response, content = httplib2.Http().request(
            url, method='POST', headers=headers, body=urlencode(params))
        if response.status != 200:
            raise HTTPError(response, content)
        if token_format == self.DICT_TOKEN_FORMAT:
            params = simplejson.loads(content)
            if context is not None:
                params["lp.context"] = context
            self._request_token = AccessToken.from_params(params)
            return params
        else:
            self._request_token = AccessToken.from_string(content)
            url = '%s%s?oauth_token=%s' % (web_root, authorize_token_page,
                                           self._request_token.key)
            if context is not None:
                self._request_token.context = context
                url += "&lp.context=%s" % context
            return url

    def exchange_request_token_for_access_token(
        self, web_root=uris.STAGING_WEB_ROOT):
        """Exchange the previously obtained request token for an access token.

        This method must not be called unless get_request_token() has been
        called and completed successfully.

        The access token will be stored as self.access_token.

        :param web_root: The base URL of the website that granted the
            request token.
        """
        assert self._request_token is not None, (
            "get_request_token() doesn't seem to have been called.")
        web_root = uris.lookup_web_root(web_root)
        params = dict(
            oauth_consumer_key=self.consumer.key,
            oauth_signature_method='PLAINTEXT',
            oauth_token=self._request_token.key,
            oauth_signature='&%s' % self._request_token.secret)
        url = web_root + access_token_page
        headers = {'Referer' : web_root}
        response, content = httplib2.Http().request(
            url, method='POST', headers=headers, body=urlencode(params))
        if response.status != 200:
            raise HTTPError(response, content)
        self.access_token = AccessToken.from_string(content)


class AccessToken(_AccessToken):
    """An OAuth access token."""

    @classmethod
    def from_params(cls, params):
        """Create and return a new `AccessToken` from the given dict."""
        key = params['oauth_token']
        secret = params['oauth_token_secret']
        context = params.get('lp.context')
        return cls(key, secret, context)

    @classmethod
    def from_string(cls, query_string):
        """Create and return a new `AccessToken` from the given string."""
        params = cgi.parse_qs(query_string, keep_blank_values=False)
        key = params['oauth_token']
        assert len(key) == 1, (
            "Query string must have exactly one oauth_token.")
        key = key[0]
        secret = params['oauth_token_secret']
        assert len(secret) == 1, "Query string must have exactly one secret."
        secret = secret[0]
        context = params.get('lp.context')
        if context is not None:
            assert len(context) == 1, (
                "Query string must have exactly one context")
            context = context[0]
        return cls(key, secret, context)


class AnonymousAccessToken(_AccessToken):
    """An OAuth access token that doesn't authenticate anybody.

    This token can be used for anonymous access.
    """
    def __init__(self):
        super(AnonymousAccessToken, self).__init__('','')


class SimulatedLaunchpadBrowser(object):
    """A programmable substitute for a human-operated web browser.

    Used by client programs to interact with Launchpad's credential
    pages, without opening them in the user's actual web browser.
    """

    def __init__(self, web_root=uris.STAGING_WEB_ROOT):
        self.web_root = uris.lookup_web_root(web_root)
        self.http = httplib2.Http()

    def _auth_header(self, username, password):
        """Utility method to generate a Basic auth header."""
        auth = base64.encodestring("%s:%s" % (username, password))[:-1]
        return "Basic " + auth

    def get_token_info(self, username, password, request_token,
                       access_levels=''):
        """Retrieve a JSON representation of a request token.

        This is useful for verifying that the end-user gave a valid
        username and password, and for reconciling the client's
        allowable access levels with the access levels defined in
        Launchpad.
        """
        if access_levels != '':
            s = "&allow_permission="
            access_levels = s + s.join(access_levels)
        page = "%s?oauth_token=%s%s" % (
            authorize_token_page, request_token, access_levels)
        url = urljoin(self.web_root, page)
        # We can't use httplib2's add_credentials, because Launchpad
        # doesn't respond to credential-less access with a 401
        # response code.
        headers = {'Accept' : 'application/json',
                   'Referer' : self.web_root}
        headers['Authorization'] = self._auth_header(username, password)
        response, content = self.http.request(url, headers=headers)
        # Detect common error conditions and set the response code
        # appropriately. This lets code that uses
        # SimulatedLaunchpadBrowser detect standard response codes
        # instead of having Launchpad-specific knowledge.
        location = response.get('content-location')
        if response.status == 200 and '+login' in location:
            response.status = 401
        elif response.get('content-type') != 'application/json':
            response.status = 500
        return response, content

    def grant_access(self, username, password, request_token, access_level,
                     context=None):
        """Grant a level of access to an application on behalf of a user."""
        headers = {'Content-type' : 'application/x-www-form-urlencoded',
                   'Referer' : self.web_root}
        headers['Authorization'] = self._auth_header(username, password)
        body = "oauth_token=%s&field.actions.%s=True" % (
            quote(request_token), quote(access_level))
        if context is not None:
            body += "&lp.context=%s" % quote(context)
        url = urljoin(self.web_root, "+authorize-token")
        response, content = self.http.request(
            url, method="POST", headers=headers, body=body)
        # This would be much less fragile if Launchpad gave us an
        # error code to work with.
        if "Unauthenticated user POSTing to page" in content:
            response.status = 401 # Unauthorized
        elif 'Request already reviewed' in content:
            response.status = 409 # Conflict
        elif 'What level of access' in content:
            response.status = 400 # Bad Request
        elif 'Unable to identify application' in content:
            response.status = 400 # Bad Request
        elif not 'Almost finished' in content:
            response.status = 500 # Internal Server Error
        return response, content


class RequestTokenAuthorizationEngine(object):

    UNAUTHORIZED_ACCESS_LEVEL = "UNAUTHORIZED"

    # Suggested messages for clients to display in common situations.

    AUTHENTICATION_FAILURE = "I can't log in with the credentials you gave me. Let's try again."

    CHOOSE_ACCESS_LEVEL = """Now it's time for you to decide how much power to give "%(app)s" over your Launchpad account."""

    CHOOSE_ACCESS_LEVEL_ONE = CHOOSE_ACCESS_LEVEL + """

"%(app)s" says it needs the following level of access to your Launchpad account: "%(level)s". It can't work with any other level of access, so denying this level of access means prohibiting "%(app)s" from using your Launchpad account at all."""

    USER_AUTHORIZED = """Okay, I'm telling Launchpad to grant "%(app)s" access to your account."""

    USER_REFUSED_TO_AUTHORIZE = """Okay, I'm going to cancel the request that "%(app)s" made for access to your account. You can always set this up again later."""

    CLIENT_ERROR = """Sorry, but Launchpad is behaving in a way this client doesn't understand. There might be a bug in the client, a bug in the server, or this client might just be out of date."""

    CONSUMER_MISMATCH = """WARNING: The application you're using told me its name was "%(old_consumer)s", but it told Launchpad its name was "%(real_consumer)s". This is probably not a problem, but it's a little suspicious, so you might want to look into this before continuing. I'll refer to the application as "%(real_consumer)s" from this point on."""

    INPUT_USERNAME = "What email address do you use on Launchpad?"

    INPUT_PASSWORD = "What's your Launchpad password? "

    NONEXISTENT_REQUEST_TOKEN = """Launchpad couldn't find an outstanding request for integration between "%(app)s" and your Launchpad account. Either someone (hopefully you) already set up the integration, or else "%(app)s" is simply wrong and didn't actually set this up with Launchpad. If you still can't use "%(app)s" with Launchpad, try this process again from the beginning."""

    REQUEST_TOKEN_ALREADY_AUTHORIZED = """It looks like you already approved this request to grant "%(app)s" access to your Launchpad account. You shouldn't need to do anything more."""

    SERVER_ERROR = """There seems to be something wrong on the Launchpad server side, and I can't continue. Hopefully this is a temporary problem, but if it persists, it's probably because of a bug in Lauchpad or (less likely) a bug in "%(app)s"."""

    STARTUP_MESSAGE = """An application identified as "%(app)s" wants to access Launchpad on your behalf. I'm the Launchpad credential client and I'm here to ask for your Launchpad username and password."""

    STARTUP_MESSAGE_2 = """I'll use your Launchpad password to give "%(app)s" limited access to your Launchpad account. I will not show your password to "%(app)s" itself."""

    SUCCESS = """You're all done! You should now be able to use Launchpad integration features of "%(app)s." """

    SUCCESS_UNAUTHORIZED = """You're all done! "%(app)s" still doesn't have access to your Launchpad account."""

    TOO_MANY_AUTHENTICATION_FAILURES = """You've failed the password entry too many times. I'm going to exit back to "%(app)s." Try again once you've solved the problem with your Launchpad account."""

    YOU_NEED_A_LAUNCHPAD_ACCOUNT = """OK, you'll need to get yourself a Launchpad account before you can integrate Launchpad into "%(app)s."

I'm opening the Launchpad registration page in your web browser so you can create an account. Once you've created an account, you can try this again."""

    def __init__(self, web_root, consumer_name, request_token,
                 allow_access_levels=[], max_failed_attempts=3):
        self.web_root = uris.lookup_web_root(web_root)
        self.consumer_name = consumer_name
        self.request_token = request_token
        self.browser = SimulatedLaunchpadBrowser(self.web_root)
        self.max_failed_attempts = max_failed_attempts
        self.allow_access_levels = allow_access_levels
        self.text_wrapper = textwrap.TextWrapper(
            replace_whitespace=False, width=78)

    def __call__(self):

        self.startup(
            [self.message(self.STARTUP_MESSAGE),
             self.message(self.STARTUP_MESSAGE_2)])

        # Have the end-user enter their Launchpad username and password.
        # Make sure the credentials are valid, and get information
        # about the request token as a side effect.
        username, password, token_info = self.get_http_credentials()

        # Update this object with fresh information about the request token.
        self.token_info = token_info
        self.reconciled_access_levels = token_info['access_levels']
        self._check_consumer()

        # Have the end-user choose an access level from the fresh list.
        if len(self.reconciled_access_levels) == 2:
            # There's only one choice: allow access at a certain level
            # or don't allow access at all.
            message = self.CHOOSE_ACCESS_LEVEL_ONE
            level = [level for level in self.reconciled_access_levels
                     if level['value'] != self.UNAUTHORIZED_ACCESS_LEVEL][0]
            extra = {'level' : level['title']}
            only_one_option = level
        else:
            message = self.CHOOSE_ACCESS_LEVEL
            extra = None
            only_one_option = None
        access_level = self.input_access_level(
            self.reconciled_access_levels, self.message(message, extra),
            only_one_option)

        # Notify the program of the user's choice.
        if access_level == self.UNAUTHORIZED_ACCESS_LEVEL:
            self.user_refused_to_authorize(
                    self.message(self.USER_REFUSED_TO_AUTHORIZE))
        else:
            self.user_authorized(
                access_level, self.message(self.USER_AUTHORIZED))

        # Try to grant the specified level of access to the request token.
        response, content = self.browser.grant_access(
            username, password, self.request_token, access_level)
        if response.status == 409:
            raise RequestTokenAlreadyAuthorized(
                self.message(self.REQUEST_TOKEN_ALREADY_AUTHORIZED))
        elif response.status == 400:
            raise ClientError(self.message(self.CLIENT_ERROR))
        elif response.status == 500:
            raise ServerError(self.message(self.SERVER_ERROR))
        if access_level == self.UNAUTHORIZED_ACCESS_LEVEL:
            message = self.SUCCESS_UNAUTHORIZED
        else:
            message = self.SUCCESS
        self.success(self.message(message))

    def get_http_credentials(self, cached_username=None, failed_attempts=0):
        """Authenticate the user to Launchpad, or raise an exception trying.

        :return: A 3-tuple (username, password,
        token_info). 'username' and 'password' are the validated
        Launchpad username and password. 'token_info' is a dict of
        validated information about the request token, including
        Launchpad's reconciled list of its available access levels
        with the access levels the third-party client will accept.

        :param cached_username: If the user has tried to enter their
        credentials before and failed, this variable will contain the
        username they entered the first time. This can be presented as
        a default, since users are more likely to enter the wrong
        password than the wrong username.

        :param failed_attempts: This method calls itself recursively
        until failed_attempts equals self.max_failed_attempts.
        """
        username = self.input_username(
            cached_username, self.message(self.INPUT_USERNAME))
        if username is None:
            self.open_page_in_user_browser(
                urljoin(self.web_root, "+login"))
            raise NoLaunchpadAccount(
                self.message(self.YOU_NEED_A_LAUNCHPAD_ACCOUNT))
        password = self.input_password(self.message(self.INPUT_PASSWORD))
        response, content = self.browser.get_token_info(
            username, password, self.request_token, self.allow_access_levels)
        if response.status == 500:
            raise ServerError(self.message(self.SERVER_ERROR))
        elif response.status == 401:
            failed_attempts += 1
            if failed_attempts == self.max_failed_attempts:
                raise TooManyAuthenticationFailures(
                    self.message(self.TOO_MANY_AUTHENTICATION_FAILURES))
            else:
                self.authentication_failure(
                    self.message(self.AUTHENTICATION_FAILURE))
                return self.get_http_credentials(username, failed_attempts)
        token_info = simplejson.loads(content)
        # If Launchpad provides no information about the request token,
        # that means the request token doesn't exist.
        if 'oauth_token' not in token_info:
            raise RequestTokenAlreadyAuthorized(
                self.message(self.NONEXISTENT_REQUEST_TOKEN))
        return username, password, token_info

    def _check_consumer(self):
        """Sanity-check the server consumer against the client consumer."""
        real_consumer = self.token_info['oauth_token_consumer']
        if real_consumer != self.consumer_name:
            message = self.message(
                self.CONSUMER_MISMATCH, { 'old_consumer' : self.consumer_name,
                                          'real_consumer' : real_consumer })
            self.server_consumer_differs_from_client_consumer(
                self.consumer_name, real_consumer, message)
            self.consumer_name = real_consumer

    def message(self, raw_message, extra_variables=None):
        """Prepare a message by plugging in the app name."""
        variables = { 'app' : self.consumer_name }
        if extra_variables is not None:
            variables.update(extra_variables)
        return raw_message % variables

    def open_page_in_user_browser(self, url):
        """Open a web page in the user's web browser."""
        webbrowser.open(url)

    # You should define these methods in your subclass.

    def output(self, message):
        print self.text_wrapper.fill(message)

    def input_username(self, cached_username, suggested_message):
        """Collect the Launchpad username from the end-user.

        :param cached_username: A username from a previous entry attempt,
        to be presented as the default.
        """
        raise NotImplementedError()

    def input_password(self, suggested_message):
        """Collect the Launchpad password from the end-user."""
        raise NotImplementedError()

    def input_access_level(self, available_levels, suggested_message,
                           only_one_option=None):
        """Collect the desired level of access from the end-user."""
        raise NotImplementedError()

    def startup(self, suggested_messages):
        """Hook method called on startup."""
        for message in suggested_messages:
            self.output(message)
            self.output("\n")

    def authentication_failure(self, suggested_message):
        """The user entered invalid credentials."""
        self.output(suggested_message)
        self.output("\n")

    def user_refused_to_authorize(self, suggested_message):
        """The user refused to authorize a request token."""
        self.output(suggested_message)
        self.output("\n")

    def user_authorized(self, access_level, suggested_message):
        """The user authorized a request token with some access level."""
        self.output(suggested_message)
        self.output("\n")

    def server_consumer_differs_from_client_consumer(
        self, client_name, real_name, suggested_message):
        """The client seems to be lying or mistaken about its name.

        When requesting a request token, the client told Launchpad
        that its consumer name was "foo". Now the client is telling the
        end-user that its name is "bar". Something is fishy and at the very
        least the end-user should be warned about this.
        """
        self.output("\n")
        self.output(suggested_message)
        self.output("\n")

    def success(self, suggested_message):
        """The token was successfully authorized."""
        self.output(suggested_message)


class AuthorizeRequestTokenWithBrowser(RequestTokenAuthorizationEngine):
    """The simplest and most secure request token authorizer.

    This authorizer simply opens up the end-user's web browser to a
    Launchpad URL and lets the end-user authorize the request token
    themselves.
    """

    def __init__(self, web_root, consumer_name, request_token,
                 allow_access_levels=[], max_failed_attempts=3):
        web_root = uris.lookup_web_root(web_root)
        page = "+authorize-token?oauth_token=%s" % request_token
        if len(allow_access_levels) > 0:
            page += ("&allow_permission=" +
                     "&allow_permission=".join(allow_access_levels))
        self.authorization_url = urljoin(web_root, page)

        super(AuthorizeRequestTokenWithBrowser, self).__init__(
            web_root, consumer_name, request_token,
            allow_access_levels, max_failed_attempts)

    def __call__(self):
        self.open_page_in_user_browser(self.authorization_url)
        print "The authorization page:"
        print "   (%s)" % self.authorization_url
        print "should be opening in your browser. After you have authorized"
        print "this program to access Launchpad on your behalf you should come"
        print ("back here and press <Enter> to finish the authentication "
               "process.")
        self.wait_for_request_token_authorization()

    def wait_for_request_token_authorization(self):
        """Get the end-user to hit enter."""
        sys.stdin.readline()


class TokenAuthorizationException(Exception):
    pass


class RequestTokenAlreadyAuthorized(TokenAuthorizationException):
    pass


class ClientError(TokenAuthorizationException):
    pass


class ServerError(TokenAuthorizationException):
    pass


class NoLaunchpadAccount(TokenAuthorizationException):
    pass


class TooManyAuthenticationFailures(TokenAuthorizationException):
    pass
