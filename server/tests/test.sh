#!/bin/bash
reset
set -e

cd $ISPN/infinispan-alt/server/core
mvn clean install -DskipTests -Dcheckstyle.skip=true
cd $ISPN/infinispan-alt/server/runtime
mvn clean install -DskipTests -Dcheckstyle.skip=true
cd ../tests
#mvnDebug verify  -Dit.test=BackupManagerIT#testClusterBackupFromFile -Dlog4j.configurationFile=/home/remerson/workspace/logs/infinispan/log4j2.xml -DforkMode=never -DforkCount=0
mvn verify  -Dit.test=BackupManagerIT#testClusterBackupFromFile
mvn verify  -Dit.test=BackupManagerIT
