from pychecker2 import main
from pychecker2 import Options

import unittest

class WarningTester(unittest.TestCase):
    def __init__(self, arg):
        unittest.TestCase.__init__(self, arg)
        self.options = Options.Options()
        self.checklist = main.create_checklist(self.options)
        self.argv = []

    def check_warning(self, w, expected_line, expected_type, *expected_args):
        warn_line, warn_type, warn_args = w
        try:
            self.assertEqual(warn_type, expected_type)
            self.assertEqual(warn_line, expected_line)
            self.assertEqual(len(warn_args), len(expected_args))
            for i in range(len(expected_args)):
                self.assertEqual(warn_args[i], expected_args[i])
        except AssertionError:          # help w/debugging
            print warn_line, warn_type, warn_args
            print expected_line, expected_type, expected_args
            raise

    def check_file(self, data):
        import tempfile, os
        fname = tempfile.mktemp()
        fp = open(fname, 'wb')
        try:
            fp.write(data)
            fp.close()
            f, = self.options.process_options(self.argv + [fname])
            self.checklist.check_file(f)
        finally:
            fp.close()
            os.unlink(fname)
        return f

    def warning_file(self, f, line, warning, *args):
        assert len(f.warnings) == 1, "Not just one warning:" + `f.warnings`
        self.check_warning(f.warnings[0], line, warning, *args)

    def warning(self, test, line, warning, *args):
        test += '\n'
        f = self.check_file(test)
        self.warning_file(f, line, warning, *args)

    def silent(self, test):
        f = self.check_file(test)
        if f.warnings:
            print f.warnings
        self.assertEqual(len(f.warnings), 0)

    def setUp(self):
        self.argv = []
        
