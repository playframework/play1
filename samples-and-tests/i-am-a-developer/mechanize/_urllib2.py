# urllib2 work-alike interface
# ...from urllib2...
from urllib2 import \
     URLError, \
     HTTPError, \
     BaseHandler, \
     UnknownHandler, \
     FTPHandler, \
     CacheFTPHandler
# ...and from mechanize
from _auth import \
     HTTPPasswordMgr, \
     HTTPPasswordMgrWithDefaultRealm, \
     AbstractBasicAuthHandler, \
     AbstractDigestAuthHandler, \
     HTTPProxyPasswordMgr, \
     ProxyHandler, \
     ProxyBasicAuthHandler, \
     ProxyDigestAuthHandler, \
     HTTPBasicAuthHandler, \
     HTTPDigestAuthHandler, \
     HTTPSClientCertMgr
from _debug import \
     HTTPResponseDebugProcessor, \
     HTTPRedirectDebugProcessor
from _file import \
     FileHandler
# crap ATM
## from _gzip import \
##      HTTPGzipProcessor
from _http import \
     HTTPHandler, \
     HTTPDefaultErrorHandler, \
     HTTPRedirectHandler, \
     HTTPEquivProcessor, \
     HTTPCookieProcessor, \
     HTTPRefererProcessor, \
     HTTPRefreshProcessor, \
     HTTPErrorProcessor, \
     HTTPRobotRulesProcessor, \
     RobotExclusionError
import httplib
if hasattr(httplib, 'HTTPS'):
    from _http import HTTPSHandler
del httplib
from _opener import OpenerDirector, \
     SeekableResponseOpener, \
     build_opener, install_opener, urlopen
from _request import \
     Request
from _seek import \
     SeekableProcessor
from _upgrade import \
     HTTPRequestUpgradeProcessor, \
     ResponseUpgradeProcessor
