import os
import sys
import unittest
import glob

def test(modules, verbosity):
    for m in modules:
        s = unittest.defaultTestLoader.loadTestsFromName(m)
        result = unittest.TextTestRunner(verbosity=verbosity).run(s)
        if not result.wasSuccessful():
            return 1
    return 0


def _modules(root):
    modules = []
    files = glob.glob(os.path.join(root, 'utest', '*.py'))
    files.sort()
    for fname in files:
        fname = os.path.split(fname)[1] # remove path
        module = 'pychecker2.utest.' + os.path.splitext(fname)[0]
        if not module.endswith('_'):    # ignore __init__
            modules.append(module)
    return modules

class Usage(Exception): pass

def main(args):
    import getopt
    verbosity = 1
    try:
        opts, files = getopt.getopt(args, 'v')
        for opt, arg in opts:
            if opt == '-v':
                verbosity += 1
            else:
                raise Usage('unknown option ' + opt)
    except getopt.GetoptError, detail:
        raise Usage(str(detail))

    root = os.path.dirname(os.path.realpath(sys.argv[0]))
    pychecker2 = os.path.split(root)[0]
    sys.path.append(pychecker2)

    return test(_modules(root), verbosity)
        

if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv[1:]))
    except Usage, error:
        err = sys.stderr
        print >>err, "Error: " + str(error)
        print >>err, "Usage: %s [-v]" % sys.argv[0]
        sys.exit(1)
