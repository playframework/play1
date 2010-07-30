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

"""Launchpad-specific URIs and convenience lookup functions.

The code in this module lets users say "edge" when they mean
"https://api.edge.launchpad.net/".
"""

__metaclass__ = type
__all__ = [
    'lookup_service_root',
    'lookup_web_root',
    ]

from urlparse import urlparse

LPNET_SERVICE_ROOT = 'https://api.launchpad.net/'
EDGE_SERVICE_ROOT = 'https://api.edge.launchpad.net/'
STAGING_SERVICE_ROOT = 'https://api.staging.launchpad.net/'
DEV_SERVICE_ROOT = 'https://api.launchpad.dev/'
DOGFOOD_SERVICE_ROOT = 'https://api.dogfood.launchpad.net/'
TEST_DEV_SERVICE_ROOT = 'http://api.launchpad.dev:8085/'

LPNET_WEB_ROOT = 'https://launchpad.net/'
EDGE_WEB_ROOT = 'https://edge.launchpad.net/'
STAGING_WEB_ROOT = 'https://staging.launchpad.net/'
DEV_WEB_ROOT = 'https://launchpad.dev/'
DOGFOOD_WEB_ROOT = 'https://dogfood.launchpad.net/'
TEST_DEV_WEB_ROOT = 'http://launchpad.dev:8085/'


service_roots = dict(
    production=LPNET_SERVICE_ROOT,
    edge=EDGE_SERVICE_ROOT,
    staging=STAGING_SERVICE_ROOT,
    dogfood=DOGFOOD_SERVICE_ROOT,
    dev=DEV_SERVICE_ROOT,
    test_dev=TEST_DEV_SERVICE_ROOT
    )


web_roots = dict(
    production=LPNET_WEB_ROOT,
    edge=EDGE_WEB_ROOT,
    staging=STAGING_WEB_ROOT,
    dogfood=DOGFOOD_WEB_ROOT,
    dev=DEV_WEB_ROOT,
    test_dev=TEST_DEV_WEB_ROOT
    )


def _dereference_alias(root, aliases):
    """Dereference what might a URL or an alias for a URL."""
    if root in aliases:
        return aliases[root]

    # It's not an alias. Is it a valid URL?
    (scheme, netloc, path, parameters, query, fragment) = urlparse(root)
    if scheme != "" and netloc != "":
        return root

    # It's not an alias or a valid URL.
    raise ValueError("%s is not a valid URL or an alias for any Launchpad "
                     "server" % root)


def lookup_service_root(service_root):
    """Dereference an alias to a service root.

    A recognized server alias such as "edge" gets turned into the
    appropriate URI. A URI gets returned as is. Any other string raises a
    ValueError.
    """
    return _dereference_alias(service_root, service_roots)


def lookup_web_root(web_root):
    """Dereference an alias to a website root.

    A recognized server alias such as "edge" gets turned into the
    appropriate URI. A URI gets returned as is. Any other string raises a
    ValueError.
    """
    return _dereference_alias(web_root, web_roots)

