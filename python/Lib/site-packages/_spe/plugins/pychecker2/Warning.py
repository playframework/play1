
class Warning:

    def __init__(self, description, message, value = 1):
        self.message = message
        self.description = description
        self.value = value
        
    def __cmp__(self, other):
        return cmp(self.message, other.message)

    def __repr__(self):
        return repr(self.message)
