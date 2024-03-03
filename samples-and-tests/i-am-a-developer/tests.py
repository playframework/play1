#!/usr/bin/python

from __future__ import print_function

import os
import shutil
import ssl
import subprocess
import sys
import threading
import time
import unittest
import urllib
from signal import SIGTERM

import mechanize

# --- TESTS
DEFAULTS = {
    'host': 'localhost',
    'http.port': '9001',
}


class IamADeveloper(unittest.TestCase):
    play = None

    # FIXME
    def skipTest_testSSLConfig(self):

        # Testing ssl config
        step('Hello, I am testing SSL config')

        self.working_directory = bootstrap_working_directory('i-am-testing-ssl-config-here')

        # play new job-app
        step('Create a new project')

        with call_play(self, ['new', '%s/sslconfigapp' % self.working_directory, '--name=SSLCONFIGAPP']) as self.play:
            self.assertTrue(wait_for(self.play, 'The new application will be created'))
            self.assertTrue(wait_for(self.play, 'OK, the application is created'))
            self.assertTrue(wait_for(self.play, 'Have fun!'))

            self.play.wait()
            step("stop play")
            kill_play(self.play, 'https')

        app = '%s/sslconfigapp' % self.working_directory

        step('Add config and files')
        insert(app, "app/controllers/Application.java", 13, '        Logger.info("I am ssl secured!");')

        edit(app, "conf/application.conf", 32, 'http.port=-1')
        edit(app, "conf/application.conf", 33, 'https.port=' + DEFAULTS['http.port'])
        edit(app, "conf/application.conf", 232,
             'play.ssl.netty.pipeline = play.server.FlashPolicyHandler,org.jboss.netty.handler.codec.http.HttpRequestDecoder,play.server.StreamChunkAggregator,org.jboss.netty.handler.codec.http.HttpResponseEncoder,org.jboss.netty.handler.codec.http.HttpContentCompressor,org.jboss.netty.handler.stream.ChunkedWriteHandler,play.server.ssl.SslPlayHandler')
        create(app, 'conf/host.key')
        insert(app, "conf/host.key", 1, '-----BEGIN RSA PRIVATE KEY-----')
        insert(app, "conf/host.key", 2, 'MIIEpQIBAAKCAQEAoOx9pCR7rZ50S9FotKVD2+aC36Hj4TkXZTZwEnh/fWyuiH2O')
        insert(app, "conf/host.key", 3, 'Paj/dTw60Jvll4jshlnRHfJ6yfc/o7YlDUanLrQJm7I3/t3YNgqYg3WXeUTl+GrN')
        insert(app, "conf/host.key", 4, 'Hn/3QgFGYqKobu8kfrwP4IapQRqlq4ZSdlR/bWpxnYSCZoXeeoimoSUcLlqD5dw7')
        insert(app, "conf/host.key", 5, '7v2BlG2gqL5+lr5Fx4mDC12vczoUMRg88+VuA1ezU4cuXDe2MbpJMd7rqGN0xK4b')
        insert(app, "conf/host.key", 6, 'CwkFtSJqBM1TH/Czr1S52hKrDTTys9PVw+eZSKO7BCk+PDq5jjx337XOWiO0kSHf')
        insert(app, "conf/host.key", 7, 'V64x68xTojfzTzF304byr2Ytq6DjNbpZKwdYBwIDAQABAoIBAQCc6z7w6mp3uIWq')
        insert(app, "conf/host.key", 8, '0P6K+ISdT7/aliCCJIu9tEHAoSOgiHQAwH4NflfsV9j6RqqxA2Gw+LBDxYkanDDA')
        insert(app, "conf/host.key", 9, 'UQL8WSL5FbIw0q5rpqQIvnhN6ELWi+q8PFjcHuhawqeB0x7vXd52fqf0xxsQUw2t')
        insert(app, "conf/host.key", 10, 'noOWw3qmlR9I/Eez9WImlk314RwDzc/bUsfBQhMKbNVHxstR8Q9YQQMp+xb9dqbL')
        insert(app, "conf/host.key", 11, '3lfz3O70Q/Xc/JxXIOkqcfyoIT9CvpJf2MT1tkd1xolAV+4UJQwKQURlMKqcp7Yi')
        insert(app, "conf/host.key", 12, 'NIxqv27ZGuhdzPCSFy3zcCIYMxXVvU+oSncGMlBpyf8ONDH2wZ7/nOtaz4Kf9tNZ')
        insert(app, "conf/host.key", 13, 'ZcqtXd1RAoGBAM7DFMBd78hkJhLztXO5PqB3O87f438aDlQfIGDzi9/KD+Jy1TRz')
        insert(app, "conf/host.key", 14, 'tJMLjmhPIOuy477k6+P3MmF3KeIjFzZg2Je56++rdpdX+E09Ts4s1gZkUAAfEyeI')
        insert(app, "conf/host.key", 15, 'QJ53lrXJu0ShmXODSyEc+rtaUgsM0geL7EtacmrUQQI9yKbrUHmHw0glAoGBAMc+')
        insert(app, "conf/host.key", 16, '9D13ne8bFLQ7upY6GuidgvG+ilAKaJ1YWNWjolTIV86RCEYNmgqxF0BzGT2Db55L')
        insert(app, "conf/host.key", 17, 'Myt5epDOKJr0RRi7ddidUJFOAOfm/yciPbr+D34LCnj6rkdauAhYsjfjuWDNLHyf')
        insert(app, "conf/host.key", 18, 'hjpBvvtMfqWE79vfIwVCKOy9xUVjqfZY2KDBu4G7AoGBAMSmjooXzgOOHRhRavdR')
        insert(app, "conf/host.key", 19, '7Nq6DMxJ7RnqMk6X/De57ANBL7J0/YsRsWFZ0GwtNmZ2kl3xZNpBNk21BMTsExvJ')
        insert(app, "conf/host.key", 20, 'KLfGQTyGnBh9ts/fy6AUzMrvhZdX9uPWl38gxtrHr7Eq8cQHz+ECqwaedQHFg81h')
        insert(app, "conf/host.key", 21, 'q7BPqhspHVuAX+NCVBwCoB1xAoGBAME20mC9G6GgUE6LUWCXDjsfa7kEPlpqDZLv')
        insert(app, "conf/host.key", 22, '9o2ONkAjW8sMJ8rPK99MZjDwrLxTNi153TA+iFXeJdBGKq9WMmyR+Ww/CW/ZOPt5')
        insert(app, "conf/host.key", 23, 'IAWyk9F14Xz6E4FMfwRRBtpd8gnmTUq449CgqxRE1Ner93Hvi6VwyADz8lZc1Jf5')
        insert(app, "conf/host.key", 24, 'BnG2DSA7AoGAAWRtgCEkhR/9GyLyAqoUd45FQdRdwIiDwRUsuazSMF2g+FSIfXqR')
        insert(app, "conf/host.key", 25, 'MgEidXuKYTIRgsiDmgy6fy3XkSzaR1ehjC1uUyyiUzEd+guG9tURrRygR8S6VGw3')
        insert(app, "conf/host.key", 26, 'mxX+1gneJnzA2cBminkc28ohIQegHEqKKif5gRsc2md+LsvDNR93io4=')
        insert(app, "conf/host.key", 27, '-----END RSA PRIVATE KEY-----')
        create(app, 'conf/host.pass.key')
        insert(app, "conf/host.pass.key", 1, '-----BEGIN RSA PRIVATE KEY-----')
        insert(app, "conf/host.pass.key", 2, 'Proc-Type: 4,ENCRYPTED')
        insert(app, "conf/host.pass.key", 3, 'DEK-Info: DES-EDE3-CBC,FC6F4AA83014298F')
        insert(app, "conf/host.pass.key", 4, '')
        insert(app, "conf/host.pass.key", 5, 'ZxpC4NYQsMYCOfpMg3iRbQ5UQDBp50NGnT+wBgHnhTqXVUsIZ0x4eFvFKmIoGFne')
        insert(app, "conf/host.pass.key", 6, 'hX2pnIMFpOJs4tRIItFyvjcwAARRZxg9KCkjL8cPBhNL4LNExYOTKE8QfTzTb9/l')
        insert(app, "conf/host.pass.key", 7, 'DoF5EJraNwvXKlVNh9wrROW7oMJFqhkVRQN+lMnczTGPznnjbBvOr69ypU8/NWX/')
        insert(app, "conf/host.pass.key", 8, 'JFgLYqBUnOPUKCaqxEuNzP632jOkhSdXmtl4ft1JFx/uoJG4rCGw5zOVHnTsCMbs')
        insert(app, "conf/host.pass.key", 9, 'aWfzfYgnreKvSmwk+5J/0aHR14sXoJpPOk1KvJ3U347cJ/RB1hnnShAdEmYxqPmc')
        insert(app, "conf/host.pass.key", 10, '7Hp2BXt86qlFs9SEBwptPtGmF+YAW7HdcgU0M1ONJ0/GysT4RWFJr5VO4QQWpQT/')
        insert(app, "conf/host.pass.key", 11, 'DrX8odwKVSQHekmsJz4hD0CXj2v8KU7crbEtTemj3koxnbEn7gcZoGtTMmz37hZS')
        insert(app, "conf/host.pass.key", 12, 'qJOolpPqHFV7WtheZ/+5ztSJ91eUgRqKTt1gLgQ6wbaCFfgsPIIRAjuklWnAyKxM')
        insert(app, "conf/host.pass.key", 13, '0dxRb7pTCDLewZ7V2g9MzkF46r+eTCIw31NJC6EUsOYaj46bYbmdK5Smjqgc1z5S')
        insert(app, "conf/host.pass.key", 14, 'jQGSFUUA+MRlLhx0e/old3fK1oUY1kujcDZcz57arykFDxNHSseFIauJOUeiw0Tp')
        insert(app, "conf/host.pass.key", 15, '5nZJYtg4yWTEbLMi+iegu/pYZSbuy8APojIgPupg0FiFOED23J2ziXQs8ZxaG7w6')
        insert(app, "conf/host.pass.key", 16, 'oc6SxWrubxCGt0dlEHAQnAB5eVZGcKCH4hVaF4w85j/oWf0Tw/kFAD1MqyiBPes3')
        insert(app, "conf/host.pass.key", 17, 'BcrDyO4AJWpmocMZ5ERVkPhx1rqyRrpaYBMdTJ2LoQaKIGeDucfW3Iap0mk+jT31')
        insert(app, "conf/host.pass.key", 18, 'RTVYNlCqoU1+oACqpV4mRQGW0BDIENvazCb+VJ0qHkedrM/Bx0Gxnx7jrlptOYEn')
        insert(app, "conf/host.pass.key", 19, '2rU53bOIdwGw9+MjDV+jLKnxuwh56SI5wJzSBCr38jLlA/SgPDM+8K9AjeCJg0w5')
        insert(app, "conf/host.pass.key", 20, 'C4Na4pDa3tSRwV2WsDJcLnWN+L1NoFNNMnePGzZHCBWaFI9WM2sZI5LsM+gZt37k')
        insert(app, "conf/host.pass.key", 21, 'EnR/r8rn5Vig7hwxntW7D6IAka2Tkfl0Y+uvl373EGIv9d61/x6cxomPbYGwH0Sn')
        insert(app, "conf/host.pass.key", 22, '6Emz3so5pXUuP8w2Gx7FNI9m7r+xOAfe87Eplc5DZiwtWyeSLOKDOnkwTxNdFMhk')
        insert(app, "conf/host.pass.key", 23, 'GerNKG4RrMB5GEU0oI1rkMPlK4vf/K9ynHqLq5HjH839EzWH7aeqlo8059WMZ0Jz')
        insert(app, "conf/host.pass.key", 24, 'qecDXcEZ2K9RkUPqGC2wdAGTyea/ElEWmplAWfqVHkD497IShQfTgJ23oLxFTDhd')
        insert(app, "conf/host.pass.key", 25, 'IUso3Xj50N1U2+4JbYABv9zaXLRK+qTEPkTmeQHo+CJC0iIVQwGtQS9p3IcuLzKd')
        insert(app, "conf/host.pass.key", 26, 's3wqL1Durxe+YVfHNqTYh2uC6eclSwA/21uDa59B37oK9Aymdzujps7IJQ147QWN')
        insert(app, "conf/host.pass.key", 27, '4e39vDDrfPMthKiQAWm4f3+vduLxzShDgzLyVPDaYVfPAlD7UETz0x6eNCTZXDjg')
        insert(app, "conf/host.pass.key", 28, 'S4JMnjhH8EFrzKdnUH40oeWa9RKKo5RwvRRRGNgR23OzcibI+54kl5DsMTI229+G')
        insert(app, "conf/host.pass.key", 29, 'PDd5V4m+ahdfaPsM9DMr1mWGSN/hoLDJtMFPOiZP5R6OSTi99Tj5KJiglSdjmb6u')
        insert(app, "conf/host.pass.key", 30, '-----END RSA PRIVATE KEY-----')
        create(app, 'conf/host.cert')
        insert(app, "conf/host.cert", 1, '-----BEGIN CERTIFICATE-----')
        insert(app, "conf/host.cert", 2, 'MIID4DCCAsgCCQCdj5qAy7MGoTANBgkqhkiG9w0BAQsFADCBsTEfMB0GA1UECAwW')
        insert(app, "conf/host.cert", 3, 'VGVzdCBTdGF0ZSBvciBQcm92aW5jZTEWMBQGA1UEBwwNVGVzdCBMb2NhbGl0eTEa')
        insert(app, "conf/host.cert", 4, 'MBgGA1UECgwRT3JnYW5pemF0aW9uIE5hbWUxITAfBgNVBAsMGE9yZ2FuaXphdGlv')
        insert(app, "conf/host.cert", 5, 'bmFsIFVuaXQgTmFtZTEUMBIGA1UEAwwLQ29tbW9uIE5hbWUxITAfBgkqhkiG9w0B')
        insert(app, "conf/host.cert", 6, 'CQEWEnRlc3RAZW1haWwuYWRkcmVzczAeFw0xNzA1MjkxMjUyMDVaFw0yNzA1Mjcx')
        insert(app, "conf/host.cert", 7, 'MjUyMDVaMIGxMR8wHQYDVQQIDBZUZXN0IFN0YXRlIG9yIFByb3ZpbmNlMRYwFAYD')
        insert(app, "conf/host.cert", 8, 'VQQHDA1UZXN0IExvY2FsaXR5MRowGAYDVQQKDBFPcmdhbml6YXRpb24gTmFtZTEh')
        insert(app, "conf/host.cert", 9, 'MB8GA1UECwwYT3JnYW5pemF0aW9uYWwgVW5pdCBOYW1lMRQwEgYDVQQDDAtDb21t')
        insert(app, "conf/host.cert", 10, 'b24gTmFtZTEhMB8GCSqGSIb3DQEJARYSdGVzdEBlbWFpbC5hZGRyZXNzMIIBIjAN')
        insert(app, "conf/host.cert", 11, 'BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoOx9pCR7rZ50S9FotKVD2+aC36Hj')
        insert(app, "conf/host.cert", 12, '4TkXZTZwEnh/fWyuiH2OPaj/dTw60Jvll4jshlnRHfJ6yfc/o7YlDUanLrQJm7I3')
        insert(app, "conf/host.cert", 13, '/t3YNgqYg3WXeUTl+GrNHn/3QgFGYqKobu8kfrwP4IapQRqlq4ZSdlR/bWpxnYSC')
        insert(app, "conf/host.cert", 14, 'ZoXeeoimoSUcLlqD5dw77v2BlG2gqL5+lr5Fx4mDC12vczoUMRg88+VuA1ezU4cu')
        insert(app, "conf/host.cert", 15, 'XDe2MbpJMd7rqGN0xK4bCwkFtSJqBM1TH/Czr1S52hKrDTTys9PVw+eZSKO7BCk+')
        insert(app, "conf/host.cert", 16, 'PDq5jjx337XOWiO0kSHfV64x68xTojfzTzF304byr2Ytq6DjNbpZKwdYBwIDAQAB')
        insert(app, "conf/host.cert", 17, 'MA0GCSqGSIb3DQEBCwUAA4IBAQAw+cuEp3wbLcTIzKCrZ7KzH3zaMtzIU5ZAjTkt')
        insert(app, "conf/host.cert", 18, '66QSFALq/ZvAswAybpWKb+2EZZ8iV477W0nFJUkHIOrOav4qWJfmPtdp2k6d2Eey')
        insert(app, "conf/host.cert", 19, 'cYQjrD9ghV7aKtKCstFdXo4h23FNaKb+kHSXjvEuf8EuDWilXKrjczmJAmGpBeSE')
        insert(app, "conf/host.cert", 20, 'nUVGGYYMAKf+ndkuSYYnJs/V823o9npSiy0Ke83Z64Co04+yos+BMIuDIhP/+LOp')
        insert(app, "conf/host.cert", 21, 'pesqro66VwKswcG9O/sjSCaiFgljlQARB4xKBSwR5py8hKDBKfoWnvCpaFPLS34P')
        insert(app, "conf/host.cert", 22, 'rGPQp900aMtDjORTe2ZP2EP/rMSm7w/PL8djNVMtgFKzY2Tc')
        insert(app, "conf/host.cert", 23, '-----END CERTIFICATE-----')

        # Run the newly created application
        step('Run our ssl-application')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTPS on port ' + DEFAULTS['http.port']))

            step("Send request to https")
            response = browser_open('https://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that ssl message is logged")
            self.assertTrue(wait_for(self.play, 'I am ssl secured!'))

            step("stop play")
            kill_play(self.play, 'https')

        # now we're going to manually configure log4j to log debug messages
        step('using key file with password')

        insert(app, "conf/application.conf", 236,
               'certificate.key.file = conf/host.pass.key')

        # re-run the application with new setting
        step('re-run our ssl-application')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTPS on port ' + DEFAULTS['http.port']))

            step("Send request to https")

            response = browser_open('https://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that ssl message is logged")
            self.assertTrue(wait_for(self.play, 'I am ssl secured!'))

            step("stop play")
            kill_play(self.play, 'https')

        step("done testing ssl config")

    def testLogLevelsAndLog4jConfig(self):

        # Testing job developing
        step('Hello, I am testing loglevels')

        self.working_directory = bootstrap_working_directory('i-am-testing-log-levels-here')

        # play new job-app
        step('Create a new project')

        with call_play(self, ['new', '%s/loglevelsapp' % self.working_directory, '--name=LOGLEVELSAPP']) as self.play:
            self.assertTrue(wait_for(self.play, 'The new application will be created'))
            self.assertTrue(wait_for(self.play, 'OK, the application is created'))
            self.assertTrue(wait_for(self.play, 'Have fun!'))

            self.play.wait()

            app = '%s/loglevelsapp' % self.working_directory
            edit(app, "conf/application.conf", 32, 'http.port=' + DEFAULTS['http.port'])

            # inserting some log-statements in our controller
            insert(app, "app/controllers/Application.java", 13, '        Logger.debug("I am a debug message");')
            insert(app, "app/controllers/Application.java", 14, '        Logger.info("I am an info message");')

            # kill_play(self.play)
            self.play = None

        # Run the newly created application
        step('Run our logger-application')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTP on port ' + DEFAULTS['http.port']))

            step("Send request to trigger some logging")

            response = browser_open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that only info log message is logged")
            self.assertTrue(wait_for_with_fail(self.play, 'I am an info message', 'I am a debug message'))

            step("stop play")
            kill_play(self.play)

        # now we're going to manually configure log4j to log debug messages
        step('Writing log4j config file')

        create(app, 'conf/log4j.xml')

        insert(app, "conf/log4j.xml", 1, '<?xml version="1.0" encoding="UTF-8" ?>')
        insert(app, "conf/log4j.xml", 2, '<Configuration name="ConfigTest" status="ERROR" monitorInterval="5">')
        insert(app, "conf/log4j.xml", 3, '    <Appenders>')
        insert(app, "conf/log4j.xml", 4, '        <Console name="Out">')
        insert(app, "conf/log4j.xml", 5, '            <PatternLayout pattern="%m%n"/>')
        insert(app, "conf/log4j.xml", 6, '        </Console>')
        insert(app, "conf/log4j.xml", 7, '    </Appenders>')
        insert(app, "conf/log4j.xml", 8, '    <Loggers>')
        insert(app, "conf/log4j.xml", 9, '        <Logger name="play" level="debug" >')
        insert(app, "conf/log4j.xml", 10, '            <AppenderRef ref="Out"/>')
        insert(app, "conf/log4j.xml", 11, '        </Logger>')
        insert(app, "conf/log4j.xml", 12, '    </Loggers>')
        insert(app, "conf/log4j.xml", 13, '</Configuration>')

        # Run the newly created application
        step('re-run our logger-application')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTP on port ' + DEFAULTS['http.port']))

            step("Send request to trigger some logging")

            response = browser_open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that both debug and info message is logged")
            self.assertTrue(wait_for(self.play, 'I am a debug message'))
            self.assertTrue(wait_for(self.play, 'I am an info message'))
            step("stop play")
            kill_play(self.play)

        step("done testing logging")

    def testCreateAndRunForJobProject(self):

        # Testing job developing
        step('Hello, I am a job-developer')

        self.working_directory = bootstrap_working_directory('i-am-creating-jobs-here')

        # play new job-app
        step('Create a new project')

        with call_play(self, ['new', '%s/jobapp' % self.working_directory, '--name=JOBAPP']) as self.play:
            self.assertTrue(wait_for(self.play, 'The new application will be created'))
            self.assertTrue(wait_for(self.play, 'OK, the application is created'))
            self.assertTrue(wait_for(self.play, 'Have fun!'))
            self.play.wait()

        app = '%s/jobapp' % self.working_directory

        edit(app, "conf/application.conf", 32, 'http.port=' + DEFAULTS['http.port'])
        # create our first job - which is executed sync on startup with @OnApplicationStart

        createDir(app, 'app/jobs')
        create(app, 'app/jobs/Job1.java')
        insert(app, 'app/jobs/Job1.java', 1, "package jobs;")
        insert(app, 'app/jobs/Job1.java', 2, "import play.jobs.*;")
        insert(app, 'app/jobs/Job1.java', 3, "import play.*;")
        insert(app, 'app/jobs/Job1.java', 4, "@OnApplicationStart")
        insert(app, 'app/jobs/Job1.java', 5, "public class Job1 extends Job {")
        insert(app, 'app/jobs/Job1.java', 6, "  public void doJob() throws Exception{")
        insert(app, 'app/jobs/Job1.java', 7, '      Logger.info("Job starting");')
        insert(app, 'app/jobs/Job1.java', 8, '      Thread.sleep(5000);')
        insert(app, 'app/jobs/Job1.java', 9, '      Logger.info("Job done");')
        insert(app, 'app/jobs/Job1.java', 10, '  }')
        insert(app, 'app/jobs/Job1.java', 11, '}')

        # modify our controller to log when exeuted
        insert(app, "app/controllers/Application.java", 13, '        Logger.info("Processing request");')

        # Run the newly created application
        step('Run the newly created job-application')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTP on port ' + DEFAULTS['http.port']))

            step("Send request to start app")

            response = browser_open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that job completed before processing request")
            self.assertTrue(wait_for(self.play, 'Job done'))
            self.assertTrue(wait_for(self.play, 'Processing request'))

            step("stop play")
            kill_play(self.play)

        # now we change the job to be async
        step("Change job to async")

        edit(app, 'app/jobs/Job1.java', 4, "@OnApplicationStart(async=true)")

        # start play again
        step('Run the job-application again')

        with call_play(self, ['run', app]) as self.play:
            # wait for play to be ready
            self.assertTrue(wait_for(self.play, 'Listening for HTTP on port ' + DEFAULTS['http.port']))

            step("Send request to start app")
            response = browser_open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])

            step("check that the request is processed before the job finishes")
            self.assertTrue(wait_for(self.play, 'Processing request'))
            self.assertTrue(wait_for(self.play, 'Job done'))
            step("stop play")
            kill_play(self.play)

        step('Done testing testCreateAndRunForJobProject')

    def testSimpleProjectCreation(self):

        # Well
        step('Hello, I\'m a developer')

        self.working_directory = bootstrap_working_directory('i-am-working-here')

        # play new yop
        step('Create a new project')

        with call_play(self, ['new', '%s/yop' % self.working_directory, '--name=YOP']) as self.play:
            self.assertTrue(wait_for(self.play, 'The new application will be created'))
            self.assertTrue(wait_for(self.play, 'OK, the application is created'))
            self.assertTrue(wait_for(self.play, 'Have fun!'))
            self.play.wait()

        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/controllers')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/controllers/Application.java')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/models')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/Application')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/Application/index.html')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/main.html')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/errors/404.html')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/app/views/errors/500.html')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/conf')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/conf/routes')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/conf/messages')))
        self.assertTrue(os.path.exists(os.path.join(self.working_directory, 'yop/conf/application.conf')))

        app = '%s/yop' % self.working_directory

        edit(app, "conf/application.conf", 32, 'http.port=' + DEFAULTS['http.port'])

        # Run the newly created application
        step('Run the newly created application')

        with call_play(self, ['run', app]) as  self.play:
            self.assertTrue(wait_for(self.play, 'Listening for HTTP on port ' + DEFAULTS['http.port']))

            # Start a browser
            step('Start a browser')

            browser = mechanize.Browser()

            # Open the home page
            step('Open the home page')

            response = browser.open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'])
            self.assertTrue(wait_for(self.play, "Application 'YOP' is now started !"))
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Your application is ready !')

            html = response.get_data()
            self.assertTrue(html.count(b'Your application is ready !'))

            # Open the documentation
            step('Open the documentation')

            browser.addheaders = [("Accept-Language", "en")]
            response = browser.open(
                'http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'] + '/@documentation')
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Play manual - Documentation')
            html = response.get_data()
            self.assertTrue(html.count(b'Getting started'))

            # Go back to home
            step('Go back to home')

            response = browser.back()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Your application is ready !')

            # Refresh
            step('Refresh home')
            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Your application is ready !')
            html = response.get_data()
            self.assertTrue(html.count(b'Your application is ready !'))

            # Make a mistake in Application.java and refresh
            step('Make a mistake in Application.java')

            edit(app, 'app/controllers/Application.java', 13, '        render()')
            try:
                browser.reload()
                self.fail()

            except urllib.error.URLError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Compilation error'))
                self.assertTrue(html.count(b'insert ";" to complete BlockStatements'))
                self.assertTrue(html.count(b'In /app/controllers/Application.java (around line 13)'))
                self.assertTrue(html.count(b'       render()'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Compilation error (In /app/controllers/Application.java around line 13)'))
                self.assertTrue(wait_for(self.play, 'Syntax error, insert ";" to complete BlockStatements'))

            # Refresh again
            step('Refresh again')

            try:
                browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Compilation error'))
                self.assertTrue(html.count(b'insert ";" to complete BlockStatements'))
                self.assertTrue(html.count(b'In /app/controllers/Application.java (around line 13)'))
                self.assertTrue(html.count(b'       render()'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Compilation error (In /app/controllers/Application.java around line 13)'))
                self.assertTrue(wait_for(self.play, 'Syntax error, insert ";" to complete BlockStatements'))

            # Correct the error
            step('Correct the error')

            edit(app, 'app/controllers/Application.java', 13, '        render();')
            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Your application is ready !')
            html = response.get_data()
            self.assertTrue(html.count(b'Your application is ready !'))

            # Refresh again
            step('Refresh again')

            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Your application is ready !')
            html = response.get_data()
            self.assertTrue(html.count(b'Your application is ready !'))

            # Let's code hello world
            step('Let\'s code hello world')
            time.sleep(1)

            edit(app, 'app/controllers/Application.java', 12, '  public static void index(String name) {')
            edit(app, 'app/controllers/Application.java', 13, '        render(name);')
            edit(app, 'app/views/Application/index.html', 2, "#{set title:'Hello world app' /}")
            edit(app, 'app/views/Application/index.html', 4, "Hello ${name} !!")
            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Hello world app')
            html = response.get_data()
            self.assertTrue(html.count(b'Hello  !!'))

            response = browser.open(
                'http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'] + '/?name=Guillaume')
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Hello world app')
            html = response.get_data()
            self.assertTrue(html.count(b'Hello Guillaume !!'))

            # Make a mistake in the template
            step('Make a mistake in the template')
            time.sleep(1)

            edit(app, 'app/views/Application/index.html', 4, "Hello ${name !!")
            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Template compilation error'))
                self.assertTrue(html.count(
                    b'The template <strong>/app/views/Application/index.html</strong> does not compile : <strong>Unexpected input: \'{\' </strong>'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Template compilation error (In /app/views/Application/index.html around line 0)'))

            # Refresh again
            step('Refresh again')

            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Template compilation error'))
                self.assertTrue(html.count(
                    b'The template <strong>/app/views/Application/index.html</strong> does not compile : <strong>Unexpected input: \'{\' </strong>'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Template compilation error (In /app/views/Application/index.html around line 0)'))

            # Try a template runtime exception
            step('Try a template runtime exception ')
            time.sleep(1)

            edit(app, 'app/views/Application/index.html', 4, "Hello ${user.name}")
            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Template execution error '))
                self.assertTrue(html.count(b'In /app/views/Application/index.html (around line 4)'))
                self.assertTrue(html.count(b'Cannot get property \'name\' on null object'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Template execution error (In /app/views/Application/index.html around line 4)'))
                self.assertTrue(
                    wait_for(self.play, 'Execution error occurred in template /app/views/Application/index.html.'))
                self.assertTrue(wait_for(self.play, 'at /app/views/Application/index.html.(line:4)'))
                self.assertTrue(wait_for(self.play, '...'))

            # Refresh again
            step('Refresh again')

            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Template execution error '))
                self.assertTrue(html.count(b'In /app/views/Application/index.html (around line 4)'))
                self.assertTrue(html.count(b'Cannot get property \'name\' on null object'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Template execution error (In /app/views/Application/index.html around line 4)'))
                self.assertTrue(
                    wait_for(self.play, 'Execution error occurred in template /app/views/Application/index.html.'))
                self.assertTrue(wait_for(self.play, 'at /app/views/Application/index.html.(line:4)'))
                self.assertTrue(wait_for(self.play, '...'))

            # Fix it
            step('Fix it')
            time.sleep(1)

            edit(app, 'app/views/Application/index.html', 4, "Hello ${name} !!")
            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Hello world app')
            html = response.get_data()
            self.assertTrue(html.count(b'Hello Guillaume !!'))

            # Make a Java runtime exception
            step('Make a Java runtime exception')

            insert(app, 'app/controllers/Application.java', 13, '        int a = 9/0;')
            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Execution exception'))
                self.assertTrue(html.count(b'/ by zero'))
                self.assertTrue(html.count(b'In /app/controllers/Application.java (around line 13)'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Execution exception (In /app/controllers/Application.java around line 13)'))
                self.assertTrue(wait_for(self.play, 'ArithmeticException occurred : / by zero'))
                self.assertTrue(wait_for(self.play, 'at controllers.Application.index(Application.java:13)'))
                self.assertTrue(wait_for(self.play, '...'))

            # Refresh again
            step('Refresh again')

            try:
                response = browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Execution exception'))
                self.assertTrue(html.count(b'/ by zero'))
                self.assertTrue(html.count(b'In /app/controllers/Application.java (around line 13)'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(
                    wait_for(self.play, 'Execution exception (In /app/controllers/Application.java around line 13)'))
                self.assertTrue(wait_for(self.play, 'ArithmeticException occurred : / by zero'))
                self.assertTrue(wait_for(self.play, 'at controllers.Application.index(Application.java:13)'))
                self.assertTrue(wait_for(self.play, '...'))

            # Fix it
            step('Fix it')
            time.sleep(1)

            delete(app, 'app/controllers/Application.java', 13)
            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Hello world app')
            html = response.get_data()
            self.assertTrue(html.count(b'Hello Guillaume !!'))

            # Refresh again
            step('Refresh again')

            response = browser.reload()
            self.assertTrue(browser.viewing_html())
            self.assertTrue(browser.title() == 'Hello world app')
            html = response.get_data()
            self.assertTrue(html.count(b'Hello Guillaume !!'))

            # Create a new route
            step('Create a new route')

            insert(app, 'conf/routes', 7, "GET      /hello          Hello.hello")
            try:
                response = browser.open('http://' + DEFAULTS['host'] + ':' + DEFAULTS['http.port'] + '/hello')
                self.fail()
            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Not found')

            # Create the new controller
            step('Create the new controller')
            time.sleep(1)

            create(app, 'app/controllers/Hello.java')
            insert(app, 'app/controllers/Hello.java', 1, "package controllers;")
            insert(app, 'app/controllers/Hello.java', 2, "import play.mvc.*;")
            insert(app, 'app/controllers/Hello.java', 3, "public class Hello extends Application {")
            insert(app, 'app/controllers/Hello.java', 4, "  public static void hello() {")
            insert(app, 'app/controllers/Hello.java', 5, '      renderText("Hello");')
            insert(app, 'app/controllers/Hello.java', 6, '  }')
            insert(app, 'app/controllers/Hello.java', 7, '}')

            # Retry
            step('Retry')

            browser.reload()
            self.assertTrue(not browser.viewing_html())
            html = response.get_data()
            self.assertTrue(html.count(b'Hello'))

            # Rename the Hello controller
            step('Rename the Hello controller')
            time.sleep(1)

            rename(app, 'app/controllers/Hello.java', 'app/controllers/Hello2.java')
            edit(app, 'app/controllers/Hello2.java', 3, "public class Hello2 extends Application {")

            try:
                browser.reload()
                self.fail()
            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Not found')

            # Refresh again
            step('Refresh again')

            try:
                browser.reload()
                self.fail()
            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Not found')

            # Correct the routes file
            step('Correct the routes file')
            time.sleep(1)

            edit(app, 'conf/routes', 7, "GET      /hello          Hello2.hello")

            browser.reload()
            self.assertTrue(not browser.viewing_html())
            html = response.get_data()
            self.assertTrue(html.count(b'Hello'))

            # Retry
            step('Retry')

            browser.reload()
            self.assertTrue(not browser.viewing_html())
            html = response.get_data()
            self.assertTrue(html.count(b'Hello'))

            # Rename again
            step('Rename again')
            time.sleep(1)

            rename(app, 'app/controllers/Hello2.java', 'app/controllers/Hello3.java')
            edit(app, 'conf/routes', 7, "GET      /hello          Hello3.hello")

            try:
                browser.reload()
                self.fail()

            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Compilation error'))
                self.assertTrue(html.count(b'/app/controllers/Hello3.java</strong> could not be compiled'))
                self.assertTrue(html.count(b'The public type Hello2 must be defined in its own file'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(wait_for(self.play, 'Compilation error (In /app/controllers/Hello3.java around line 3)'))

            # Refresh again
            step('Refresh again')

            try:
                browser.reload()
                self.fail()
            except urllib.error.HTTPError as error:
                self.assertTrue(browser.viewing_html())
                self.assertTrue(browser.title() == 'Application error')
                html = b''.join(error.readlines())
                self.assertTrue(html.count(b'Compilation error'))
                self.assertTrue(html.count(b'/app/controllers/Hello3.java</strong> could not be compiled'))
                self.assertTrue(html.count(b'The public type Hello2 must be defined in its own file'))
                self.assertTrue(wait_for(self.play, 'ERROR play'))
                self.assertTrue(wait_for(self.play, 'Compilation error (In /app/controllers/Hello3.java around line 3)'))

            # Fix it
            step('Fix it')

            edit(app, 'app/controllers/Hello3.java', 3, "public class Hello3 extends Application {")
            browser.reload()
            self.assertTrue(not browser.viewing_html())
            html = response.get_data()
            self.assertTrue(html.count(b'Hello'))

            step('Kill play')
            kill_play(self.play)

        step('Done testing testSimpleProjectCreation')

    def tearDown(self):
        if self.play:
            kill_play(self.play)


# --- UTILS

def bootstrap_working_directory(folder):
    test_base = os.path.normpath(os.path.dirname(os.path.realpath(sys.argv[0])))
    working_directory = os.path.join(test_base, folder)
    if (os.path.exists(working_directory)):
        shutil.rmtree(working_directory)
    os.mkdir(working_directory)
    return working_directory


def call_play(self, args):
    play_script = os.path.join(self.working_directory, '../../../play')
    if sys.platform.startswith('win32'):
        play_script += "".join('.bat')

    process_args = [play_script] + args

    print('RUN  %s' % process_args)

    # encode subprocess output with system default codec
    play_process = subprocess.Popen(process_args,
                                    shell=False,
                                    stdout=subprocess.PIPE,
                                    universal_newlines=True)

    print('RUNNING  %s' % play_process.pid)
    return play_process


# returns true when pattern is seen
def wait_for(process, pattern):
    return wait_for_with_fail(process, pattern, "")


# returns true when pattern is seen, but false if failPattern is not seen or if timeout
def wait_for_with_fail(process, pattern, fail_pattern):
    timer = threading.Timer(90, timeout, [process])
    timer.start()
    while True:
        sys.stdout.flush()
        line = process.stdout.readline().strip()
        sys.stdout.flush()
        # print timeoutOccurred
        if timeoutOccurred:
            timer.cancel()
            return False
        if line == '@KILLED':
            timer.cancel()
            return False
        if line: print(line)
        if fail_pattern != "" and line.count(fail_pattern):
            timer.cancel()
            return False
        if line.count(pattern):
            timer.cancel()
            return True


timeoutOccurred = False


def timeout(process):
    global timeoutOccurred
    print('@@@@ TIMEOUT !')
    kill_play(process)
    timeoutOccurred = True


def kill_play(process, http='http', host=DEFAULTS['host'], port=DEFAULTS['http.port']):
    print("kill play PID=%s" % process.pid)
    try:
        print("Call %s" % "{}://{}:{}/@kill".format(http, host, port))
        urllib.request.urlopen("{}://{}:{}/@kill".format(http, host, port), timeout=5)

        print("terminate")
        process.terminate()

        print("process 1 (thread): kill process 2 (pid %s)" % process.pid)
        process.kill()
        print("process 1 (thread): close process 2 stdout pipe (fd %s)" % process.stdout.fileno())

        if process.stdout:
            process.stdout.close()

        if process.stderr:
            process.stderr.close()

        if process.stdin:
            process.stdin.close()

        print("wait")
        process.wait(10)
        # kill subprocess tree, because calling urllib.urlopen(f"{http}://localhost:9000/@kill") is not enough
        while True:
            if process.poll() is None:
                print("Kill Play subprocess")
                os.kill(process.pid, SIGTERM)
                process.wait(3)
                print("play is KILLED")
            else:
                print("play is KILLED")
                return
    except Exception as ex:
        print(ex)
        print("play is KILLED with exception")
        pass


def step(msg):
    print()
    print('# --- %s' % msg)
    print()


def edit(app, file, line, text):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    lines[line - 1] = '%s\n' % text
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)


def insert(app, file, line, text):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    lines[line - 1:line - 1] = '%s\n' % text
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)


def create(app, file):
    fname = os.path.join(app, file)
    source = open(fname, 'w')
    source.close()
    os.utime(fname, None)


def createDir(app, file):
    fname = os.path.join(app, file)
    os.mkdir(fname)


def delete(app, file, line):
    fname = os.path.join(app, file)
    source = open(fname, 'r')
    lines = source.readlines()
    del lines[line - 1]
    source.close()
    source = open(fname, 'w')
    source.write(''.join(lines))
    source.close()
    os.utime(fname, None)


def rename(app, fro, to):
    os.rename(os.path.join(app, fro), os.path.join(app, to))


def browser_open(url):
    browser = mechanize.Browser()
    browser.set_handle_robots(False)

    # context = ssl.SSLContext(ssl.PROTOCOL_TLS)
    # context.verify_mode = ssl.CERT_NONE
    # context.check_hostname = False
    # context.load_default_certs()
    # browser.set_ca_data(context=context)

    response = browser.open(url)
    browser.close()

    return response


if __name__ == '__main__':
    # thanks to: https://stackoverflow.com/a/35960702/3221476
    # try:
    #     _create_unverified_https_context = ssl._create_unverified_context
    # except AttributeError:
    #     # Legacy Python that doesn't verify HTTPS certificates by default
    #     pass
    # else:
    #     # Handle target environment that doesn't support HTTPS verification
    #     ssl._create_default_https_context = _create_unverified_https_context
    unittest.main()
