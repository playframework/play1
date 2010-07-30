# Copyright 2009 Canonical Ltd.  All rights reserved.
#
# This file is part of lazr.uri
#
# lazr.uri is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, version 3 of the License.
#
# lazr.uri is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
# License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with lazr.uri.  If not, see <http://www.gnu.org/licenses/>.

"""Functions for working with generic syntax URIs."""

__metaclass__ = type
__all__ = [
    'URI',
    'InvalidURIError',
    'find_uris_in_text',
    'possible_uri_re',
    'merge',
    'remove_dot_segments',
    ]

import re


# Default port numbers for different URI schemes
# The registered URI schemes comes from
#    http://www.iana.org/assignments/uri-schemes.html
# The default ports come from the relevant RFCs

_default_port = {
    # Official schemes
    'acap': '674',
    'dav': '80',
    'dict': '2628',
    'dns': '53',
    'ftp': '21',
    'go': '1096',
    'gopher': '70',
    'h323': '1720',
    'http': '80',
    'https': '443',
    'imap': '143',
    'ipp': '631',
    'iris.beep': '702',
    'ldap': '389',
    'mtqp': '1038',
    'mupdate': '3905',
    'nfs': '2049',
    'nntp': '119',
    'pop': '110',
    'rtsp': '554',
    'sip': '5060',
    'sips': '5061',
    'snmp': '161',
    'soap.beep': '605',
    'soap.beeps': '605',
    'telnet': '23',
    'tftp': '69',
    'tip': '3372',
    'vemmi': '575',
    'xmlrpc.beep': '602',
    'xmlrpc.beeps': '602',
    'z39.50r': '210',
    'z39.50s': '210',

    # Historical schemes
    'prospero': '1525',
    'wais': '210',

    # Common but unregistered schemes
    'bzr+http': '80',
    'bzr+ssh': '22',
    'irc': '6667',
    'sftp': '22',
    'ssh': '22',
    'svn': '3690',
    'svn+ssh': '22',
    }

# Regular expressions adapted from the ABNF in the RFC

scheme_re = r"(?P<scheme>[a-z][-a-z0-9+.]*)"

userinfo_re = r"(?P<userinfo>(?:[-a-z0-9._~!$&\'()*+,;=:]|%[0-9a-f]{2})*)"
# The following regular expression will match some IP address style
# host names that the RFC would not (e.g. leading zeros on the
# components), but is signficantly simpler.
host_re = (r"(?P<host>[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|"
           r"(?:[-a-z0-9._~!$&\'()*+,;=]|%[0-9a-f]{2})*|"
           r"\[[0-9a-z:.]+\])")
port_re = r"(?P<port>[0-9]*)"

authority_re = r"(?P<authority>(?:%s@)?%s(?::%s)?)" % (
    userinfo_re, host_re, port_re)

path_abempty_re = r"(?:/(?:[-a-z0-9._~!$&\'()*+,;=:@]|%[0-9a-f]{2})*)*"
path_noscheme_re = (r"(?:[-a-z0-9._~!$&\'()*+,;=@]|%[0-9a-f]{2})+"
                    r"(?:/(?:[-a-z0-9._~!$&\'()*+,;=:@]|%[0-9a-f]{2})*)*")
path_rootless_re = (r"(?:[-a-z0-9._~!$&\'()*+,;=:@]|%[0-9a-f]{2})+"
                    r"(?:/(?:[-a-z0-9._~!$&\'()*+,;=:@]|%[0-9a-f]{2})*)*")
path_absolute_re = r"/(?:%s)?" % path_rootless_re
path_empty_re = r""

hier_part_re = r"(?P<hierpart>//%s%s|%s|%s|%s)" % (
    authority_re, path_abempty_re, path_absolute_re, path_rootless_re,
    path_empty_re)

relative_part_re = r"(?P<relativepart>//%s%s|%s|%s|%s)" % (
    authority_re, path_abempty_re, path_absolute_re, path_noscheme_re,
    path_empty_re)

# Additionally we also permit square braces in the query portion to
# accomodate real-world URIs.
query_re = r"(?P<query>(?:[-a-z0-9._~!$&\'()*+,;=:@/?\[\]]|%[0-9a-f]{2})*)"
fragment_re = r"(?P<fragment>(?:[-a-z0-9._~!$&\'()*+,;=:@/?]|%[0-9a-f]{2})*)"

uri_re = r"%s:%s(?:\?%s)?(?:#%s)?$" % (
    scheme_re, hier_part_re, query_re, fragment_re)

relative_ref_re = r"%s(?:\?%s)?(?:#%s)?$" % (
    relative_part_re, query_re, fragment_re)

uri_pat = re.compile(uri_re, re.IGNORECASE)
relative_ref_pat = re.compile(relative_ref_re, re.IGNORECASE)


def merge(basepath, relpath, has_authority):
    """Merge two URI path components into a single path component.

    Follows rules specified in Section 5.2.3 of RFC 3986.

    The algorithm in the RFC treats the empty basepath edge case
    differently for URIs with and without an authority section, which
    is why the third argument is necessary.
    """
    if has_authority and basepath == '':
        return '/' + relpath
    slash = basepath.rfind('/')
    return basepath[:slash+1] + relpath


def remove_dot_segments(path):
    """Remove '.' and '..' segments from a URI path.

    Follows the rules specified in Section 5.2.4 of RFC 3986.
    """
    output = []
    while path:
        if path.startswith('../'):
            path = path[3:]
        elif path.startswith('./'):
            path = path[2:]
        elif path.startswith('/./') or path == '/.':
            path = '/' + path[3:]
        elif path.startswith('/../') or path == '/..':
            path = '/' + path[4:]
            if len(output) > 0:
                del output[-1]
        elif path in ['.', '..']:
            path = ''
        else:
            if path.startswith('/'):
                slash = path.find('/', 1)
            else:
                slash = path.find('/')
            if slash < 0:
                slash = len(path)
            output.append(path[:slash])
            path = path[slash:]
    return ''.join(output)


def normalise_unreserved(string):
    """Return a version of 's' where no unreserved characters are encoded.

    Unreserved characters are defined in Section 2.3 of RFC 3986.

    Percent encoded sequences are normalised to upper case.
    """
    result = string.split('%')
    unreserved = ('ABCDEFGHIJKLMNOPQRSTUVWXYZ'
                  'abcdefghijklmnopqrstuvwxyz'
                  '0123456789-._~')
    for index, item in enumerate(result):
        if index == 0:
            continue
        try:
            ch = int(item[:2], 16)
        except ValueError:
            continue
        if chr(ch) in unreserved:
            result[index] = chr(ch) + item[2:]
        else:
            result[index] = '%%%02X%s' % (ch, item[2:])
    return ''.join(result)


class InvalidURIError(Exception):
    """Invalid URI"""


class URI:
    """A class that represents a URI.

    This class can represent arbitrary URIs that conform to the
    generic syntax described in RFC 3986.
    """

    def __init__(self, uri=None, scheme=None, userinfo=None, host=None,
                 port=None, path=None, query=None, fragment=None):
        """Create a URI instance.

        Can be called with either a string URI or the component parts
        of the URI as keyword arguments.

        In either case, all arguments are expected to be appropriately
        URI encoded.
        """
        assert (uri is not None and scheme is None and userinfo is None and
                host is None and port is None and path is None and
                query is None and fragment is None) or uri is None, (
            "URI() must be called with a single string argument or "
            "with URI components given as keyword arguments.")

        if uri is not None:
            if isinstance(uri, unicode):
                try:
                    uri = uri.encode('ASCII')
                except UnicodeEncodeError:
                    raise InvalidURIError(
                        'URIs must consist of ASCII characters')
            match = uri_pat.match(uri)
            if match is None:
                raise InvalidURIError('"%s" is not a valid URI' % uri)
            self.scheme = match.group('scheme')
            self.userinfo = match.group('userinfo')
            self.host = match.group('host')
            self.port = match.group('port')
            hierpart = match.group('hierpart')
            authority = match.group('authority')
            if authority is None:
                self.path = hierpart
            else:
                # Skip past the //authority part
                self.path = hierpart[2+len(authority):]
            self.query = match.group('query')
            self.fragment = match.group('fragment')
        else:
            if scheme is None:
                raise InvalidURIError('URIs must have a scheme')
            if host is None and (userinfo is not None or port is not None):
                raise InvalidURIError(
                    'host must be given if userinfo or port are')
            if path is None:
                raise InvalidURIError('URIs must have a path')
            self.scheme = scheme
            self.userinfo = userinfo
            self.host = host
            self.port = port
            self.path = path
            self.query = query
            self.fragment = fragment

        self._normalise()

        if (self.scheme in ['http', 'https', 'ftp', 'gopher', 'telnet',
                           'imap', 'mms', 'rtsp', 'svn', 'svn+ssh',
                           'bzr', 'bzr+http', 'bzr+ssh'] and
            not self.host):
            raise InvalidURIError('%s URIs must have a host name' %
                                  self.scheme)


    def _normalise(self):
        """Perform normalisation of URI components."""
        self.scheme = self.scheme.lower()

        if self.userinfo is not None:
            self.userinfo = normalise_unreserved(self.userinfo)
        if self.host is not None:
            self.host = normalise_unreserved(self.host.lower())
        if self.port == '':
            self.port = None
        elif self.port is not None:
            if self.port == _default_port.get(self.scheme):
                self.port = None
        if self.host is not None and self.path == '':
            self.path = '/'
        self.path = normalise_unreserved(remove_dot_segments(self.path))

        if self.query is not None:
            self.query = normalise_unreserved(self.query)
        if self.fragment is not None:
            self.fragment = normalise_unreserved(self.fragment)

    @property
    def authority(self):
        """The authority part of the URI"""
        if self.host is None:
            return None
        authority = self.host
        if self.userinfo is not None:
            authority = '%s@%s' % (self.userinfo, authority)
        if self.port is not None:
            authority = '%s:%s' % (authority, self.port)
        return authority

    @property
    def hier_part(self):
        """The hierarchical part of the URI"""
        authority = self.authority
        if authority is None:
            return self.path
        else:
            return '//%s%s' % (authority, self.path)

    def __str__(self):
        uri = '%s:%s' % (self.scheme, self.hier_part)
        if self.query is not None:
            uri += '?%s' % self.query
        if self.fragment is not None:
            uri += '#%s' % self.fragment
        return uri

    def __repr__(self):
        return '%s(%r)' % (self.__class__.__name__, str(self))

    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return (self.scheme == other.scheme and
                    self.authority == other.authority and
                    self.path == other.path and
                    self.query == other.query and
                    self.fragment == other.fragment)
        else:
            return NotImplemented

    def __ne__(self, other):
        equal = self.__eq__(other)
        if equal == NotImplemented:
            return NotImplemented
        else:
            return not equal

    def replace(self, **parts):
        """Replace one or more parts of the URI, returning the result."""
        if not parts:
            return self
        baseparts = dict(
            scheme=self.scheme,
            userinfo=self.userinfo,
            host=self.host,
            port=self.port,
            path=self.path,
            query=self.query,
            fragment=self.fragment)
        baseparts.update(parts)
        return self.__class__(**baseparts)

    def resolve(self, reference):
        """Resolve the given URI reference relative to this URI.

        Uses the rules from Section 5.2 of RFC 3986 to resolve the new
        URI.
        """
        # If the reference is a full URI, then return it as is.
        try:
            return self.__class__(reference)
        except InvalidURIError:
            pass

        match = relative_ref_pat.match(reference)
        if match is None:
            raise InvalidURIError("Invalid relative reference")

        parts = dict(scheme=self.scheme)
        authority = match.group('authority')
        if authority is not None:
            parts['userinfo'] = match.group('userinfo')
            parts['host'] = match.group('host')
            parts['port'] = match.group('port')
            # Skip over the //authority part
            parts['path'] = remove_dot_segments(
                match.group('relativepart')[2+len(authority):])
            parts['query'] = match.group('query')
        else:
            path = match.group('relativepart')
            query = match.group('query')
            if path == '':
                parts['path'] = self.path
                if query is not None:
                    parts['query'] = query
                else:
                    parts['query'] = self.query
            else:
                if path.startswith('/'):
                    parts['path'] = remove_dot_segments(path)
                else:
                    parts['path'] = merge(self.path, path,
                                          has_authority=self.host is not None)
                    parts['path'] = remove_dot_segments(parts['path'])
                parts['query'] = query
            parts['userinfo'] = self.userinfo
            parts['host'] = self.host
            parts['port'] = self.port
        parts['fragment'] = match.group('fragment')

        return self.__class__(**parts)

    def append(self, path):
        """Append the given path to this URI.

        The path must not start with a slash, but a slash is added to
        base URI (before appending the path), in case it doesn't end
        with a slash.
        """
        assert not path.startswith('/')
        return self.ensureSlash().resolve(path)

    def contains(self, other):
        """Returns True if the URI 'other' is contained by this one."""
        if (self.scheme != other.scheme or
            self.authority != other.authority):
            return False
        if self.path == other.path:
            return True
        basepath = self.path
        if not basepath.endswith('/'):
            basepath += '/'
        otherpath = other.path
        if not otherpath.endswith('/'):
            otherpath += '/'
        return otherpath.startswith(basepath)

    def underDomain(self, domain):
        """Return True if the given domain name a parent of the URL's host."""
        if len(domain) == 0:
            return True
        our_segments = self.host.split('.')
        domain_segments = domain.split('.')
        return our_segments[-len(domain_segments):] == domain_segments

    def ensureSlash(self):
        """Return a URI with the path normalised to end with a slash."""
        if self.path.endswith('/'):
            return self
        else:
            return self.replace(path=self.path + '/')

    def ensureNoSlash(self):
        """Return a URI with the path normalised to not end with a slash."""
        if self.path.endswith('/'):
            return self.replace(path=self.path.rstrip('/'))
        else:
            return self


# Regular expression for finding URIs in a body of text:
#
# From RFC 3986 ABNF for URIs:
#
#   URI           = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
#   hier-part     = "//" authority path-abempty
#                 / path-absolute
#                 / path-rootless
#                 / path-empty
#
#   authority     = [ userinfo "@" ] host [ ":" port ]
#   userinfo      = *( unreserved / pct-encoded / sub-delims / ":" )
#   host          = IP-literal / IPv4address / reg-name
#   reg-name      = *( unreserved / pct-encoded / sub-delims )
#   port          = *DIGIT
#
#   path-abempty  = *( "/" segment )
#   path-absolute = "/" [ segment-nz *( "/" segment ) ]
#   path-rootless = segment-nz *( "/" segment )
#   path-empty    = 0<pchar>
#
#   segment       = *pchar
#   segment-nz    = 1*pchar
#   pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
#
#   query         = *( pchar / "/" / "?" )
#   fragment      = *( pchar / "/" / "?" )
#
#   unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
#   pct-encoded   = "%" HEXDIG HEXDIG
#   sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
#                 / "*" / "+" / "," / ";" / "="
#
# We only match a set of known scheme names.  We don't handle
# IP-literal either.
#
# We will simplify "unreserved / pct-encoded / sub-delims" as the
# following regular expression:
#   [-a-zA-Z0-9._~%!$&'()*+,;=]
#
# We also require that the path-rootless form not begin with a
# colon to avoid matching strings like "http::foo" (to avoid bug
# #40255).
#
# The path-empty pattern is not matched either, due to false
# positives.
#
# Some allowed URI punctuation characters will be trimmed if they
# appear at the end of the URI since they may be incidental in the
# flow of the text.
#
# apport has at one time produced query strings containing sqaure
# braces (that are not percent-encoded). In RFC 2986 they seem to be
# allowed by section 2.2 "Reserved Characters", yet section 3.4
# "Query" appears to provide a strict definition of the query string
# that would forbid square braces. Either way, links with
# non-percent-encoded square braces are being used on Launchpad so
# it's probably best to accomodate them.

possible_uri_re = r'''
\b
(?:about|gopher|http|https|sftp|news|ftp|mailto|file|irc|jabber|xmpp)
:
(?:
  (?:
    # "//" authority path-abempty
    //
    (?: # userinfo
      [%(unreserved)s:]*
      @
    )?
    (?: # host
      \d+\.\d+\.\d+\.\d+ |
      [%(unreserved)s]*
    )
    (?: # port
      : \d*
    )?
    (?: / [%(unreserved)s:@]* )*
  ) | (?:
    # path-absolute
    /
    (?: [%(unreserved)s:@]+
        (?: / [%(unreserved)s:@]* )* )?
  ) | (?:
    # path-rootless
    [%(unreserved)s@]
    [%(unreserved)s:@]*
    (?: / [%(unreserved)s:@]* )*
  )
)
(?: # query
  \?
  [%(unreserved)s:@/\?\[\]]*
)?
(?: # fragment
  \#
  [%(unreserved)s:@/\?]*
)?
''' % {'unreserved': "-a-zA-Z0-9._~%!$&'()*+,;="}

possible_uri_pat = re.compile(possible_uri_re, re.IGNORECASE | re.VERBOSE)
uri_trailers_pat = re.compile(r'([,.?:);>]+)$')

def find_uris_in_text(text):
    """Scan a block of text for URIs, and yield the ones found."""
    for match in possible_uri_pat.finditer(text):
        uri_string = match.group()
        # remove characters from end of URI that are not likely to be
        # part of the URI.
        uri_string = uri_trailers_pat.sub('', uri_string)
        try:
            uri = URI(uri_string)
        except InvalidURIError:
            continue
        yield uri
