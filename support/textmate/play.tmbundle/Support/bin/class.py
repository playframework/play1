#!/usr/bin/env python

import os

package_name = os.environ['TM_FILEPATH']
class_name = os.environ['TM_FILENAME']

package_name = package_name[package_name.index('app/')+4:-len(class_name)-1].replace('/', '.')
class_name = class_name[0:-5]

print '''package %s;

import java.util.*;

public class %s {

	$0
    
}
''' % (package_name, class_name)