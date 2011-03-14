#(c)www.stani.be (read __doc__ for more information)                            
import sm

INFO=sm.INFO.copy()



INFO['description']=\
"""Internet related """


__doc__=INFO['doc']%INFO
#_______________________________________________________________________________

####IMPORT----------------------------------------------------------------------
import cStringIO, htmllib, httplib, MimeWriter, urllib

####FUNCTIONS-------------------------------------------------------------------
def createhtmlmail (html, text, subject):
    """Create a mime-message that will render HTML in popular MUAs, text in better ones"""

    out = cStringIO.StringIO() # output buffer for our message
    htmlin = cStringIO.StringIO(html)
    txtin = cStringIO.StringIO(text)

    writer = MimeWriter.MimeWriter(out)
    #
    # set up some basic headers... we put subject here
    # because smtplib.sendmail expects it to be in the
    # message body
    #
    writer.addheader("Subject", subject)
    writer.addheader("MIME-Version", "1.0")
    #
    # start the multipart section of the message
    # multipart/alternative seems to work better
    # on some MUAs than multipart/mixed
    #
    writer.startmultipartbody("alternative")
    writer.flushheaders()
    #
    # the plain text section
    #
    subpart = writer.nextpart()
    subpart.addheader("Content-Transfer-Encoding", "quoted-printable")
    pout = subpart.startbody("text/plain", [("charset", 'us-ascii')])
    mimetools.encode(txtin, pout, 'quoted-printable')
    txtin.close()
    #
    # start the html subpart of the message
    #
    subpart = writer.nextpart()
    subpart.addheader("Content-Transfer-Encoding", "quoted-printable")
    #
    # returns us a file-ish object we can write to
    #
    pout = subpart.startbody("text/html", [("charset", 'us-ascii')])
    mimetools.encode(htmlin, pout, 'quoted-printable')
    htmlin.close()
    #
    # Now that we're done, close our writer and
    # return the message body
    #
    writer.lastpart()
    msg = out.getvalue()
    out.close()
    print msg
    return msg

def getSource(http, request='',**keywords):
    result = []
    prevfirsturl = ''
    for c in range(0,maxr,10):
        h = httplib.HTTP(http)
        h.putrequest('GET', request+urllib.urlencode(keywords))
        h.putheader('Accept', 'text/html')
        h.endheaders()
        errcode, errmsg, headers = h.getreply()
        if errcode == 200:
            f = h.getfile()
            source=f.read()
            f.close()
            return source
        else:return ''

def htmlColor2rgb(c):
    "Converts htmlcolor in rgb tuple."
    if c[0]=='#':i=1
    else:i=0
    return (eval('0x'+c[i:i+2]),eval('0x'+c[i+2:i+4]),eval('0x'+c[i+4:i+6]))

def main():
    #createHtmlMail
    import smtplib
    f = open("newsletter.html", 'r')
    html = f.read()
    f.close()
    f = open("newsletter.txt", 'r')
    text = f.read()
    subject = "Today's Newsletter!"
    message = createhtmlmail(html, text, subject)
    server = smtplib.SMTP("localhost")
    server.sendmail('agillesp@i-noSPAMSUCKS.com', 'agillesp@i-noSPAMSUCKS.com', message)
    server.quit()
    #getSource
    getSource('www.google.com',request='/search?',q='Stani Michiels',start=0,filter=0)

if __name__=="__main__":main()
