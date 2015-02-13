#!/usr/bin/python

import unittest
import sys
import os
import mock

from tests import step

sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'framework', 'pym'))

from play.application import PlayApplication

# --- TESTS

class JvmVersionFlag(unittest.TestCase):

    def setUp(self):
        self.common = {
            'basedir':  os.path.normpath(os.path.dirname(os.path.join(os.path.dirname(__file__), '..', '..', '..'))),
            'version': 'whatever',
            'id': '',
            'jpda.port': 8000
        }

    @mock.patch('play.application.getJavaVersion', return_value='')
    def testWithFlag(self, mock):
        play_env = self.common.copy()
        # pass the jvm_version flag in
        play_env.update({'jvm_version': '42'})

        play_app = PlayApplication('fake', play_env, True)
        play_app.java_cmd([])

        step('Assert getJavaVersion was not called')
        self.assert_(not mock.called)

    @mock.patch('play.application.getJavaVersion', return_value='')
    def testWithoutFlag(self, mock):
        play_env = self.common.copy()

        play_app = PlayApplication('fake', play_env, True)
        play_app.java_cmd([])

        step('Assert getJavaVersion was called once')
        self.assert_(mock.called)


if __name__ == '__main__':
    unittest.main()
