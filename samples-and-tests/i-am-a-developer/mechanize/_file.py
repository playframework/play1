try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import mimetools
import os
import socket
import urllib
from urllib2 import BaseHandler, URLError


class FileHandler(BaseHandler):
    # Use local file or FTP depending on form of URL
    def file_open(self, req):
        url = req.get_selector()
        if url[:2] == '//' and url[2:3] != '/':
            req.type = 'ftp'
            return self.parent.open(req)
        else:
            return self.open_local_file(req)

    # names for the localhost
    names = None
    def get_names(self):
        if FileHandler.names is None:
            try:
                FileHandler.names = (socket.gethostbyname('localhost'),
                                    socket.gethostbyname(socket.gethostname()))
            except socket.gaierror:
                FileHandler.names = (socket.gethostbyname('localhost'),)
        return FileHandler.names

    # not entirely sure what the rules are here
    def open_local_file(self, req):
        try:
            import email.utils as emailutils
        except ImportError:
            import email.Utils as emailutils
        import mimetypes
        host = req.get_host()
        file = req.get_selector()
        localfile = urllib.url2pathname(file)
        try:
            stats = os.stat(localfile)
            size = stats.st_size
            modified = emailutils.formatdate(stats.st_mtime, usegmt=True)
            mtype = mimetypes.guess_type(file)[0]
            headers = mimetools.Message(StringIO(
                'Content-type: %s\nContent-length: %d\nLast-modified: %s\n' %
                (mtype or 'text/plain', size, modified)))
            if host:
                host, port = urllib.splitport(host)
            if not host or \
                (not port and socket.gethostbyname(host) in self.get_names()):
                return urllib.addinfourl(open(localfile, 'rb'),
                                  headers, 'file:'+file)
        except OSError, msg:
            # urllib2 users shouldn't expect OSErrors coming from urlopen()
            raise URLError(msg)
        raise URLError('file not on local host')
