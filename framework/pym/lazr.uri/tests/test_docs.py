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
"Test harness for doctests."

__metaclass__ = type
__all__ = [
    'additional_tests',
    ]

import atexit
import doctest
import os
import pkg_resources
import unittest

DOCTEST_FLAGS = (
    doctest.ELLIPSIS |
    doctest.NORMALIZE_WHITESPACE |
    doctest.REPORT_NDIFF)


def additional_tests():
    "Run the doc tests (README.txt and docs/*, if any exist)"
    doctest_files = [
        os.path.abspath(
            pkg_resources.resource_filename('lazr.uri', 'README.txt'))]
    if pkg_resources.resource_exists('lazr.uri', 'docs'):
        for name in pkg_resources.resource_listdir('lazr.uri', 'docs'):
            if name.endswith('.txt'):
                doctest_files.append(
                    os.path.abspath(
                        pkg_resources.resource_filename(
                            'lazr.uri', 'docs/%s' % name)))
    kwargs = dict(module_relative=False, optionflags=DOCTEST_FLAGS)
    atexit.register(pkg_resources.cleanup_resources)
    return unittest.TestSuite((
        doctest.DocFileSuite(*doctest_files, **kwargs)))
