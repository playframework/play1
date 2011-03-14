import Tkinter

def bool(value):
    if value:
        return 1
    return 0

class Base:
    "Base class for all OptionTypes"
    
    def __init__(self, name, default):
        self._name = name
        self._default = default
        self._var = None

    def name(self):
        return self._name

    def set(self, value):
        self._var.set(value)

class Boolean(Base):
    "A option type for editing boolean values"

    def __init__(self, name, default):
        Base.__init__(self, name, default)

    def field(self, w):
        self._var = Tkinter.BooleanVar()
        if self._default:
            self._var.set(1)
        else:
            self._var.set(0)
        frame = Tkinter.Frame(w, name = self._name + "Frame")
        result = Tkinter.Checkbutton(frame,
                                     name=self._name,
                                     text=self._name,
                                     variable=self._var)
        result.grid(sticky=Tkinter.W)
        frame.columnconfigure(0, weight=1)
        return frame

    def arg(self):
        if bool(self._var.get()) != bool(self._default):
            if bool(self._var.get()):
                return "--" + self._name
            return "--no-" + self._name
        return None

class Number(Base):
    "OptionType for editing numbers"

    def __init__(self, name, default):
        Base.__init__(self, name, default)

    def field(self, w):
        self._var = Tkinter.IntVar()
        self._var.set(self._default)
        frame = Tkinter.Frame(w, name = self._name + "Frame")
        label = Tkinter.Label(frame, text=self._name + ":")
        label.grid(row=0, column=0, sticky=Tkinter.W)
        entry = Tkinter.Entry(frame,
                              name=self._name,
                              textvariable=self._var,
                              width=4)
        entry.grid(row=0, column=1, sticky=Tkinter.E)
        for i in range(2):
            frame.columnconfigure(i, weight=1)
        return frame

    def arg(self):
        if self._var.get() != self._default:
            return "--%s=%d" % (self._name, self._var.get())
        return None
    
class Text(Base):
    "OptionType for editing a little bit of text"

    def __init__(self, name, default):
        Base.__init__(self, name, default)

    def width(self):
        return int(min(15, len(self._default) * 1.20))

    def field(self, w):
        self._var = Tkinter.StringVar()
        self._var.set(self._default)
        frame = Tkinter.Frame(w, name = self._name + "Frame")
        label = Tkinter.Label(frame, text=self._name + ":")
        label.grid(row=0, column=0, sticky=Tkinter.W)
        entry = Tkinter.Entry(frame,
                              name=self._name,
                              textvariable=self._var,
                              width=self.width())
        entry.grid(row=0, column=1, sticky=Tkinter.E)
        for i in range(2):
            frame.columnconfigure(i, weight=1)
        return frame

    def arg(self):
        if self._var.get() != self._default:
            return "--%s=%s" % (self._name, self._var.get())
        return None
    
def join(list):
    import string
    return string.join(list, ", ")

class List(Text):
    "OptionType for editing a list of values"

    def __init__(self, name, default):
        Text.__init__(self, name, join(default))

    def set(self, value):
        self._var.set(join(value))

