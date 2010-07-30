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

import pkg_resources
__version__ = pkg_resources.resource_string(
    "lazr.uri", "version.txt").strip()

# Re-export in such a way that __version__ can still be imported if
# dependencies are not yet available.
try:
    # While we generally frown on "*" imports, this, combined with the fact we
    # only test code from this module, means that we can verify what has been
    # exported.
    from lazr.uri._uri import *
    from lazr.uri._uri import __all__
except ImportError:
    pass
