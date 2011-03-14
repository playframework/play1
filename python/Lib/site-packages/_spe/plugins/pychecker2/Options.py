class Error(Exception): pass

class Opt:

    def __init__(self, object, longName, description, default):
        self.object = object
        self.longName = longName
        self.description = description
        setattr(object, longName, default)
        self.default = default
        
    def set_value(self, value):
        setattr(self.object, self.longName, value)

    def get_value(self):
        return getattr(self.object, self.longName)

    def get_description(self):
        return self.description

    def is_boolean(self):
        return None
    
    def reset(self):
        setattr(self.object, self.longName, self.default)

class BoolOpt(Opt):

    def __init__(self, object, longName, description, default = None):
        Opt.__init__(self, object, longName, description, default)

    def set_value(self, unused):
        setattr(self.object, self.longName, not self.get_value())
        
    def is_boolean(self):
        return 1


MAJOR = 'Major'
ERROR = 'Error'
MISC = 'Miscellaneous'
Categories = [MAJOR, ERROR, MISC]

class Options:

    def __init__(self):
        self.options = {}
        for c in Categories:
            self.options[c] = []
        self.add(BoolOpt(self, 'verbose', 'turn on verbose messages'), MISC)
        self.add(BoolOpt(self, 'incremental', 'print warnings as they are created'), MISC)
        self.add(BoolOpt(self, 'profile', 'print a profile of pychecker', 0), MISC)

    def add(self, option, category=ERROR):
        self.options[category].append(option)
        
    def process_options(self, args):
        import getopt
        try:
            longopts = {}
            for opts in self.options.values():
                for opt in opts:
                    opt.reset()
                    optname = opt.longName
                    if opt.is_boolean() and opt.get_value():
                        optname = "no-" + opt.longName
                    longopts[optname] = opt
            specs = []
            for k, v in longopts.items():
                if not v.is_boolean():
                    k += '='
                specs.append(k)
            opts, args = getopt.getopt(args, '', specs)
        except getopt.GetoptError, detail:
            raise Error(detail)
        
        for opt, arg in opts:
            longopts[opt[2:]].set_value(arg)

        from pychecker2.File import File
        return [ File(f) for f in args ]

    def usage(self, argv0, stream):
        indent = " "
        over = 20
        print >> stream, "Usage:"
        print >> stream, \
              "%s%s [options] [--] file1.py file2.py ..." % (indent, argv0)
        print >> stream, "available options:"
        for c in Categories:
            if not self.options[c]:
                continue
            print >> stream
            print >> stream, "%s:" % c
            opts = self.options[c]
            opts.sort(lambda x, y: cmp(x.longName, y.longName))
            for opt in opts:
                name = opt.longName
                if opt.is_boolean() and opt.get_value():
                        name = "no-" + name
                print >> stream, "%s--%*s %s" % (
                    indent, -over, name, opt.get_description())
                if not opt.is_boolean():
                    print >> stream, "%s  %*s %s" % (
                        indent, -over, '', opt.get_value())
            
