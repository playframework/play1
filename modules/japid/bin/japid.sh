#!/bin/sh
echo \~
echo \~ this command is for use in none-Play! environment.
echo \~ use \"play japid:$1\" for Play applications.
echo \~
dir=$(dirname $(readlink /proc/$$/fd/255))
if [ $OSTYPE == "cygwin" ] ; then
  #echo cygwin
  java -classpath "$dir/../lib/*;$dir/../lib.plain/*" cn.bran.japid.template.JapidRenderer $*
else
  java -classpath "$dir/../lib/*:$dir/../lib.plain/*" cn.bran.japid.template.JapidRenderer $*
fi
