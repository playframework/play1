..
    This file is part of lazr.uri.

    lazr.uri is free software: you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, version 3 of the License.

    lazr.uri is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with lazr.uri.  If not, see <http://www.gnu.org/licenses/>.

lazr.uri
********

The lazr.uri package includes code for parsing and dealing with URIs.

    >>> import lazr.uri
    >>> print 'VERSION:', lazr.uri.__version__
    VERSION: ...

=============
The URI class
=============

    >>> from lazr.uri import URI
    >>> uri1 = URI('http://localhost/foo/bar?123')
    >>> uri2 = URI('http://localhost/foo/bar/baz')
    >>> uri1.contains(uri2)
    True

These next two are equivalent, so the answer should be True, even through
the "outside" one is shorter than the "inside" one.

    >>> uri1 = URI('http://localhost/foo/bar/')
    >>> uri2 = URI('http://localhost/foo/bar')
    >>> uri1.contains(uri2)
    True

The next two are exactly the same.  We consider a url to be inside itself.

    >>> uri1 = URI('http://localhost/foo/bar/')
    >>> uri2 = URI('http://localhost/foo/bar/')
    >>> uri1.contains(uri2)
    True

In the next case, the string of url2 starts with the string of url1.  But,
because url2 continues within the same path step, url2 is not inside url1.

    >>> uri1 = URI('http://localhost/foo/ba')
    >>> uri2 = URI('http://localhost/foo/bar')
    >>> uri1.contains(uri2)
    False

Here, url2 is url1 plus an extra path step.  So, url2 is inside url1.

    >>> uri1 = URI('http://localhost/foo/bar/')
    >>> uri2 = URI('http://localhost/foo/bar/baz')
    >>> uri1.contains(uri2)
    True

Once the URI is parsed, its parts are accessible.

    >>> uri = URI('https://fish.tree:8666/blee/blah')
    >>> uri.scheme
    'https'
    >>> uri.host
    'fish.tree'
    >>> uri.port
    '8666'
    >>> uri.authority
    'fish.tree:8666'
    >>> uri.path
    '/blee/blah'

    >>> uri = URI('https://localhost/blee/blah')
    >>> uri.scheme
    'https'
    >>> uri.host
    'localhost'
    >>> uri.port is None
    True
    >>> uri.authority
    'localhost'
    >>> uri.path
    '/blee/blah'

The grammar from RFC 3986 does not allow for square brackets in the
query component, but Section 3.4 does say how such delimeter
characters should be handled if found in the component.

    >>> uri = URI('http://www.apple.com/store?delivery=[slow]#horse+cart')
    >>> uri.scheme
    'http'
    >>> uri.host
    'www.apple.com'
    >>> uri.port is None
    True
    >>> uri.path
    '/store'
    >>> uri.query
    'delivery=[slow]'
    >>> uri.fragment
    'horse+cart'

====================
Finding URIs in Text
====================

lazr.uri also knows how to retrieve a list of URIs from a block of
text.  This is intended for uses like finding bug tracker URIs or
similar.

The find_uris_in_text() function returns an iterator that yields URI
objects for each URI found in the text.  Note that the returned URIs
have been canonicalised by the URI class:

  >>> from lazr.uri import find_uris_in_text
  >>> text = '''
  ... A list of URIs:
  ...  * http://localhost/a/b
  ...  * http://launchpad.net
  ...  * MAILTO:joe@example.com
  ...  * xmpp:fred@example.org
  ...  * http://bazaar.launchpad.net/%7ename12/firefox/foo
  ...  * http://somewhere.in/time?track=[02]#wasted-years
  ... '''

  >>> for uri in find_uris_in_text(text):
  ...     print uri
  http://localhost/a/b
  http://launchpad.net/
  mailto:joe@example.com
  xmpp:fred@example.org
  http://bazaar.launchpad.net/~name12/firefox/foo
  http://somewhere.in/time?track=[02]#wasted-years

===============
Other Documents
===============

.. toctree::
   :glob:

   *
   docs/*
