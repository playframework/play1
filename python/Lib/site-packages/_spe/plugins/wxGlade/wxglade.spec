%define name wxGlade
%define version 0.3.5.1
%define release 1

Summary: 	wxWidgets/wxPython/wxPerl GUI designer
Name: 		%{name}
Version: 	%{version}
Release: 	%{release}
Source0: 	%{name}-%{version}.tar.gz
License: 	MIT
Group: 		Development/Tools
BuildRoot: 	%{_tmppath}/%{name}-buildroot
Prefix: 	%{_prefix}
BuildArch: 	noarch
Requires: 	python
Requires: 	wxPython

%description
wxGlade is a GUI designer written in Python with the popular GUI
toolkit wxPython, that helps you create wxWidgets/wxPython user
interfaces. At the moment it can generate Python, C++, Perl and XRC
(wxWidgets' XML resources) code.

#'

%prep
%setup -q -n wxGlade-%{version}

%build
# nothing to do


%install
# cleanup
rm -rf $RPM_BUILD_ROOT

# make dirs
mkdir -p $RPM_BUILD_ROOT%{_prefix}/bin
mkdir -p $RPM_BUILD_ROOT%{_prefix}/lib/%{name}

# copy files needed at runtime
cp -p *.py               $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -p credits.txt        $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -p license.txt        $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -pr codegen           $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -pr docs              $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -pr edit_sizers       $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -pr icons             $RPM_BUILD_ROOT%{_prefix}/lib/%{name}
cp -pr widgets           $RPM_BUILD_ROOT%{_prefix}/lib/%{name}

# make a launcher script
cat > $RPM_BUILD_ROOT%{_prefix}/bin/wxglade <<EOF
#!/bin/bash
exec python %{_prefix}/lib/%{name}/wxglade.py \$@
EOF
chmod +x $RPM_BUILD_ROOT%{_prefix}/bin/wxglade

# compile the python sources
# don't do this, it doubles the size of the rpm, and will be done automatically
# at the first execution of wxGlade...
#PYLIB=`python -c "import sys; print '%s/lib/python%s' % (sys.prefix, sys.version[:3])"`
#python $PYLIB/compileall.py $RPM_BUILD_ROOT%{_prefix}/lib/%{name}


%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root)
%doc docs examples CHANGES.txt README.txt TODO.txt credits.txt license.txt
%{_prefix}/bin/wxglade
%{_prefix}/lib/%{name}


%changelog
* Wed Oct 27 2004 Alberto Griggio <agriggio@users.sf.net> 0.3.5-1
- Updated to version 0.3.5

* Wed Mar 10 2004 Alberto Griggio <agriggio@users.sf.net> 0.3.4-1
- Updated to version 0.3.4

* Wed Mar 10 2004 Alberto Griggio <albgrig@tiscalinet.it> 0.3.2-1
- Updated to version 0.3.2

* Tue Sep 02 2003 Alberto Griggio <albgrig@tiscalinet.it> 0.3.1-1
- Updated to version 0.3.1

* Fri Aug 29 2003 Robin Dunn <robind@alldunn.com> 0.3-5
- Initial version
