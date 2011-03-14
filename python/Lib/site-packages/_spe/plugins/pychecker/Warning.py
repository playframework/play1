#!/usr/bin/env python

# Copyright (c) 2001, MetaSlash Inc.  All rights reserved.
# Portions Copyright (c) 2005, Google, Inc. All rights reserved.

"""
Warning class to hold info about each warning.
"""


class Warning :
    "Class which holds error information."

    def __init__(self, file, line, err) :
        if hasattr(file, "function") :
            file = file.function.func_code.co_filename
        elif hasattr(file, "co_filename") :
            file = file.co_filename
        elif hasattr(line, "co_filename") :
            file = line.co_filename
        if file[:2] == './' :
            file = file[2:]
        self.file = file

        if hasattr(line, "co_firstlineno") :
            line = line.co_firstlineno
        if line == None :
            line = 1
        self.line = line
        self.err = err
        self.level = err.level

    def __cmp__(self, warn) :
        if warn == None :
            return 1
        if not self.file and not self.line:
            return 1
        if self.file != warn.file :
            return cmp(self.file, warn.file)
        if self.line != warn.line :
            return cmp(self.line, warn.line)
        return cmp(self.err, warn.err)

    def format(self) :
        if not self.file and not self.line:
            return str(self.err)
        return "%s:%d: %s" % (self.file, self.line, self.err)

    def output(self, stream) :
        stream.write(self.format() + "\n")
