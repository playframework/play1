DESTDIR=

PACKAGE=python-wxglade
PYVER=2.3

all: debian/wxglade.1

clean:
	find . -name "*.pyc" -exec rm -f {} \;
	find . -name "*~" -exec rm -f {} \;

DB2MAN=/usr/share/sgml/docbook/stylesheet/xsl/nwalsh/manpages/docbook.xsl
XP=xsltproc --nonet

debian/wxglade.1: debian/manpage.xml
	cd debian && $(XP) $(DB2MAN) manpage.xml

install: all install-doc
	cp -a *.py codegen edit_sizers res widgets \
	  $(DESTDIR)/usr/lib/python$(PYVER)/site-packages/wxglade
	# fix executable flags
	for f in configUI.py zwxglade.py; do \
	  chmod 755 $(DESTDIR)/usr/lib/python$(PYVER)/site-packages/wxglade/$$f; \
	done
	for f in edit_widget.py config.py; do \
	  chmod 644 $(DESTDIR)/usr/lib/python$(PYVER)/site-packages/wxglade/$$f; \
	done
	cp -a icons $(DESTDIR)/usr/share/$(PACKAGE)
	# get rid of .xvpics subdirectories and .cvsignore files
	find $(DESTDIR)/usr/share/$(PACKAGE) -name '.xvpics' -type d | xargs rm -rf 
	find $(DESTDIR) -name '.cvsignore' -type f | xargs rm -f
	ln -s /usr/share/$(PACKAGE)/icons \
	  $(DESTDIR)/usr/lib/python$(PYVER)/site-packages/wxglade
	install -m 755 wxglade $(DESTDIR)/usr/bin
install-doc: debian/wxglade.1
	gzip -c9 debian/wxglade.1 > $(DESTDIR)/usr/share/man/man1/wxglade.1.gz
	cp -a docs $(DESTDIR)/usr/share/doc/$(PACKAGE)/
