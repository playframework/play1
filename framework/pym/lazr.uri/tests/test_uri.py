# Copyright 2006-2009 Canonical Ltd.  All rights reserved.
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
"""Unit tests."""

__metaclass__ = type
__all__ = [
    'test_suite',
    ]

import unittest

from lazr.uri import (
    InvalidURIError, URI, find_uris_in_text, merge, remove_dot_segments)


class URITestCase(unittest.TestCase):

    def test_normalisation(self):
        # URI normalisation examples from Section 6.2.2 of RFC 3986.
        self.assertEqual(str(URI('eXAMPLE://a/./b/../b/%63/%7bfoo%7d')),
                         'example://a/b/c/%7Bfoo%7D')

        self.assertEqual(str(URI('http://www.EXAMPLE.com/')),
                         'http://www.example.com/')
        self.assertEqual(str(URI('http://www.gnome.org/%7ejamesh/')),
                         'http://www.gnome.org/~jamesh/')

        # Port number normalisation, and adding missing slash for URIs
        # with authority:
        self.assertEqual(str(URI('http://example.com')),
                             'http://example.com/')
        self.assertEqual(str(URI('http://example.com:/')),
                             'http://example.com/')
        self.assertEqual(str(URI('http://example.com:80/')),
                             'http://example.com/')

    def test_merge(self):
        # Test that the merge() function performs as described in
        # Section 5.2.3 of RFC 3986
        self.assertEqual(merge('', 'foo', has_authority=True), '/foo')
        self.assertEqual(merge('', 'foo', has_authority=False), 'foo')
        self.assertEqual(merge('/a/b/c', 'foo', has_authority=True),
                         '/a/b/foo')
        self.assertEqual(merge('/a/b/', 'foo', has_authority=True),
                         '/a/b/foo')

    def test_remove_dot_segments(self):
        # remove_dot_segments() examples from Section 5.2.4 of RFC 3986:
        self.assertEqual(remove_dot_segments('/a/b/c/./../../g'), '/a/g')
        self.assertEqual(remove_dot_segments('mid/content=5/../6'), 'mid/6')

    def test_normal_resolution(self):
        # Normal URI resolution examples from Section 5.4.1 of RFC 3986:
        base = URI('http://a/b/c/d;p?q')
        def resolve(relative):
            return str(base.resolve(relative))
        self.assertEqual(resolve('g:h'),     'g:h')
        self.assertEqual(resolve('g'),       'http://a/b/c/g')
        self.assertEqual(resolve('./g'),     'http://a/b/c/g')
        self.assertEqual(resolve('g/'),      'http://a/b/c/g/')
        self.assertEqual(resolve('/g'),      'http://a/g')
        # The extra slash here comes from normalisation:
        self.assertEqual(resolve('//g'),     'http://g/')
        self.assertEqual(resolve('?y'),      'http://a/b/c/d;p?y')
        self.assertEqual(resolve('g?y'),     'http://a/b/c/g?y')
        self.assertEqual(resolve('#s'),      'http://a/b/c/d;p?q#s')
        self.assertEqual(resolve('g#s'),     'http://a/b/c/g#s')
        self.assertEqual(resolve('g?y#s'),   'http://a/b/c/g?y#s')
        self.assertEqual(resolve(';x'),      'http://a/b/c/;x')
        self.assertEqual(resolve('g;x'),     'http://a/b/c/g;x')
        self.assertEqual(resolve('g;x?y#s'), 'http://a/b/c/g;x?y#s')
        self.assertEqual(resolve(''),        'http://a/b/c/d;p?q')
        self.assertEqual(resolve('.'),       'http://a/b/c/')
        self.assertEqual(resolve('./'),      'http://a/b/c/')
        self.assertEqual(resolve('..'),      'http://a/b/')
        self.assertEqual(resolve('../'),     'http://a/b/')
        self.assertEqual(resolve('../g'),    'http://a/b/g')
        self.assertEqual(resolve('../..'),   'http://a/')
        self.assertEqual(resolve('../../'),  'http://a/')
        self.assertEqual(resolve('../../g'), 'http://a/g')

    def test_abnormal_resolution(self):
        # Abnormal URI resolution examples from Section 5.4.2 of RFC 3986:
        base = URI('http://a/b/c/d;p?q')
        def resolve(relative):
            return str(base.resolve(relative))
        self.assertEqual(resolve('../../../g'),   'http://a/g')
        self.assertEqual(resolve('../../../../g'),'http://a/g')
        self.assertEqual(resolve('/./g'),         'http://a/g')
        self.assertEqual(resolve('/../g'),        'http://a/g')
        self.assertEqual(resolve('g.'),           'http://a/b/c/g.')
        self.assertEqual(resolve('.g'),           'http://a/b/c/.g')
        self.assertEqual(resolve('g..'),          'http://a/b/c/g..')
        self.assertEqual(resolve('..g'),          'http://a/b/c/..g')
        self.assertEqual(resolve('./../g'),       'http://a/b/g')
        self.assertEqual(resolve('./g/.'),        'http://a/b/c/g/')
        self.assertEqual(resolve('g/./h'),        'http://a/b/c/g/h')
        self.assertEqual(resolve('g/../h'),       'http://a/b/c/h')
        self.assertEqual(resolve('g;x=1/./y'),    'http://a/b/c/g;x=1/y')
        self.assertEqual(resolve('g;x=1/../y'),   'http://a/b/c/y')
        self.assertEqual(resolve('g?y/./x'),      'http://a/b/c/g?y/./x')
        self.assertEqual(resolve('g?y/../x'),     'http://a/b/c/g?y/../x')
        self.assertEqual(resolve('g#s/./x'),      'http://a/b/c/g#s/./x')
        self.assertEqual(resolve('g#s/../x'),     'http://a/b/c/g#s/../x')
        # XXX 2009-01-30 jamesh:
        # I've disabled this test since we refuse to accept HTTP URIs
        # without a hostname component.
        #self.assertEqual(resolve('http:g'),       'http:g')

    def test_underDomain_matches_subdomain(self):
        # URI.underDomain should return True when asked whether the url is
        # under one of its parent domains.
        uri = URI('http://code.launchpad.dev/foo')
        self.assertTrue(uri.underDomain('code.launchpad.dev'))
        self.assertTrue(uri.underDomain('launchpad.dev'))
        self.assertTrue(uri.underDomain(''))

    def test_underDomain_doesnt_match_non_subdomain(self):
        # URI.underDomain should return False when asked whether the url is
        # under a domain which isn't one of its parents.
        uri = URI('http://code.launchpad.dev/foo')
        self.assertFalse(uri.underDomain('beta.code.launchpad.dev'))
        self.assertFalse(uri.underDomain('google.com'))
        self.assertFalse(uri.underDomain('unchpad.dev'))


def additional_tests():
    return unittest.TestLoader().loadTestsFromName(__name__)
