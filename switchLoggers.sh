#!/bin/bash

#TESTING
# We tested all loggers and in play's default implementation jdk logs out of order with the rest(so if you use a library that is jdk logging...well, you are screwed and 
# that would be a separate bug/issue than what I am trying to solve.  The loggers tested in the controllers and libraries are
# Also of note is the jdk logging format is incorrect and not matching everyone else
#
#	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Application.class);
#	private static final java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(Application.class.getName());
#	private static final org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(Application.class);
#	private static final org.apache.commons.logging.Log commonsLogger = org.apache.commons.logging.LogFactory.getLog(Application.class);
#
#
#		log.info("slf4j logger");
#		jdkLogger.info("jdk logger here");
#		log4jLogger.info("log4j logger");
#		commonsLogger.info("commons logger here");
#		Logger.info("play logger");
#
# RESULTS:
#11:02:19,400 INFO  ~ slf4j logger
#11:02:19,403 INFO  ~ log4j logger
#11:02:19,403 INFO  ~ commons logger here
#11:02:19,403 INFO  ~ play logger
#Nov 22, 2013 11:02:19 AM controllers.Application index
#INFO: jdk logger here
#
#
#


#This one is used to proxy classes that playframework is calling into which prevents switching out
#libraries
cp framework/lib-logging/emptylog4j.jar framework/lib

#####################
#this if for logback
#####################
#first copy the adapters for other logging libs to slf4j
cp framework/lib-logging/jcl-over-slf4j-1.7.5.jar framework/lib
cp framework/lib-logging/jul-to-slf4j-1.7.5.jar framework/lib
cp framework/lib-logging/log4j-over-slf4j-1.7.5.jar framework/lib

#Now copy the implementation of slf4j
cp framework/lib-logging/logback-classic-1.0.13.jar framework/lib
cp framework/lib-logging/logback-core-1.0.13.jar	 framework/lib

cp framework/lib-logging/logback.xml resources/application-skel/conf
cp framework/lib-logging/logback.xml.prod resources/application-skel/conf

#Now delete commons-logging which conflicts with jcl-over-slf4j classes
rm framework/lib/commons-logging-1.1.3.jar
rm framework/lib/slf4j-log4j12-1.7.5.jar
rm framework/lib/log4j-1.2.17.jar

#######################
# End changes needed for logback installation
######################



