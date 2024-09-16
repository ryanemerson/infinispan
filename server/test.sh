#!/bin/bash
reset
set -e
RELEASES=~/Releases
ROOT=~/workspace/RedHat/infinispan/infinispan
SERVER_ROOT=$ROOT/server

cd ${SERVER_ROOT}/testdriver
mvn clean install -DskipTests
cd ${SERVER_ROOT}/tests
mvn clean install -DskipTests

