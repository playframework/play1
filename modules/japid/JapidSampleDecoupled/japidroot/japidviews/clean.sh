#!/bin/bash

rm templates/*.java
rm Application/*.java
rm _tags/*.java
rm _layouts/*.java
rm BaseController/*.java
rm Caches/*.java
rm DummyController/*.java
rm SampleController/*.java
rm _notifiers/TestEmailer/*java
find more -name "*.java" |xargs rm

