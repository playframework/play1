import sys
root = sys.exec_prefix

# set path (avoid other Python installations)
sys.path = []
for dir in ('App', 'Lib', 'Lib\\lib-tk', 'Lib\\plat-win', 'DLLs'):
    sys.path.append('%s\\%s' % (root, dir))

# run application
if __name__ == '__main__':
    import main
    main.main()
