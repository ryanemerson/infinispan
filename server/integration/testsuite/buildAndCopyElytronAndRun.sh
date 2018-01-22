#!/bin/bash
reset

set -e

INIT_DIR=$(pwd)
cd ..
mvn clean install -DskipTests -Dcheckstyle.skip=true
cd $RH/wildfly-elytron
sh buildAndCopyToServer.sh
cd $RH/JGroups
sh buildAndCopyToServer.sh
cd $INIT_DIR
mvn clean verify -Dit.test=org.infinispan.server.test.security.jgroups.sasl.SaslAuthIT -Dtrace=org.infinispan.server,org.widlfly.security -P suite-manual

