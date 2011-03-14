#!/usr/bin/env python
"""Shortcut to launch spe in Windows."""
if __name__=='__main__':
    import sys
    if not '--debug' in sys.argv: sys.argv.append('--debug')
    import _spe.SPE
