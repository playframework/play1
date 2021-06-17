# Play1

[![travis-ci](https://travis-ci.org/playframework/play1.svg?branch=master)](https://travis-ci.org/github/playframework/play1) [![gitter chat](https://badges.gitter.im/playframework/play1.svg)](https://gitter.im/playframework/play1?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

The Play Framework makes it easy to build web applications on the JVM using the Model-View-Controller pattern.
It's bundles [Hibernate](https://hibernate.org) as ORM, [Netty](https://netty.io) as web server and provides a templating system that uses [Groovy](https://groovy-lang.org).

Version 2 of the [Play Framework](https://www.playframework.com) was a complete rewrite and is written in Scala (contrary to version 1 which is written in Java).
Porting an application over from version 1 to version 2 takes considerable effort,
and since version 2 is written in Scala it makes less sense to do so for applications built with Play version 1 that are written in Java.

This repository contains a fork of the Play Framework version 1, named Play1.
Here several organizations that have Play1 applications running collaborate in maintaining this framework.


## Documentation

Getting started with Play1 is easy. With a few lines of shell script you have a running instance:

    export PLAY_VERSION=1.6.0
    wget https://github.com/playframework/play1/releases/download/$PLAY_VERSION/play-$PLAY_VERSION.zip -O /tmp
    unzip /tmp/play-$PLAY_VERSION.zip -d /opt/play
    export PATH=$PATH:/opt/play
    play new /opt/myFirstApp
    play run /opt/myFirstApp

If all went well browsing to [localhost:9000](http://localhost:9000) shows the Play1 welcome page.

Further reading (for version `1.5.x`, mostly the same as for `1.6.x`):

* [Complete installation guide](https://www.playframework.com/documentation/1.5.x/install)
* [Your first application — the ‘Hello World’ tutorial](https://www.playframework.com/documentation/1.5.x/firstapp)
* [Tutorial — Play guide, a real world app step-by-step](https://www.playframework.com/documentation/1.5.x/guide1)
* [The essential documentation](https://www.playframework.com/documentation/1.5.x/home)
* [API documentation (Javadoc)](https://www.playframework.com/documentation/1.5.x/api/index.html)


## Get the source code

Get it with:

    git clone https://github.com/playframework/play1.git 

The project history is pretty big.  To avoid downloading it all try:

    git clone https://github.com/playframework/play1.git --depth 10

Building the project from source is [documented](https://www.playframework.com/documentation/1.5.x/install#build).

To share improvements fork the source code and create a pull request.


## Reporting bugs

Please report bugs in this project's [Github Issues](https://github.com/playframework/play1/issues) section.


## Learn More

* [Browse the collection of Play1 modules](https://www.playframework.com/modules)
* [Our Code of Conduct](https://www.playframework.com/conduct)
* [RePlay — A minimalistic Play1 fork that uses Gradle](https://github.com/codeborne/replay)


## Licence

Play1 is distributed under the [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).
