import shutil
import tempfile
import unittest


class SetupStack(object):

    def __init__(self):
        self._on_teardown = []

    def add_teardown(self, teardown):
        self._on_teardown.append(teardown)

    def tear_down(self):
        for func in reversed(self._on_teardown):
            func()


class TearDownConvenience(object):

    def __init__(self, setup_stack=None):
        self._own_setup_stack = setup_stack is None
        if setup_stack is None:
            setup_stack = SetupStack()
        self._setup_stack = setup_stack

    # only call this convenience method if no setup_stack was supplied to c'tor
    def tear_down(self):
        assert self._own_setup_stack
        self._setup_stack.tear_down()


class TempDirMaker(TearDownConvenience):

    def make_temp_dir(self):
        temp_dir = tempfile.mkdtemp(prefix="tmp-%s-" % self.__class__.__name__)
        def tear_down():
            shutil.rmtree(temp_dir)
        self._setup_stack.add_teardown(tear_down)
        return temp_dir


class MonkeyPatcher(TearDownConvenience):

    def monkey_patch(self, obj, name, value):
        orig_value = getattr(obj, name)
        setattr(obj, name, value)
        def reverse_patch():
            setattr(obj, name, orig_value)
        self._setup_stack.add_teardown(reverse_patch)


class TestCase(unittest.TestCase):

    def setUp(self):
        self._setup_stack = SetupStack()

    def tearDown(self):
        self._setup_stack.tear_down()

    def make_temp_dir(self, *args, **kwds):
        return TempDirMaker(self._setup_stack).make_temp_dir(*args, **kwds)

    def monkey_patch(self, *args, **kwds):
        return MonkeyPatcher(self._setup_stack).monkey_patch(*args, **kwds)

    def assert_contains(self, container, containee):
        self.assertTrue(containee in container, "%r not in %r" %
                        (containee, container))

    def assert_less_than(self, got, expected):
        self.assertTrue(got < expected, "%r >= %r" %
                        (got, expected))
