import inspect

def message(level=0):
    text = ''
    frames  = inspect.getouterframes(inspect.currentframe())[2:]
    if level==0:
        text += '%s\n'% ('>'.join([x[3] for x in frames]))
    elif level == 1:
        i   = 0
        for f in frames:
            if i>0:text += ' '
            text += '%s %s\n'%(':'.join([str(x) for x in f[1:3]]),f[4][0].strip())
            i   += 1
    return text
            
def frame(level=0):
    print message(level)