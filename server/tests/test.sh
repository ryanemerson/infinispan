#!/bin/bash
reset
set -e
set -x
ROOT=~/workspace/RedHat/infinispan/infinispan-alt

function untilFailure() {
    #  If pipefail is enabled, the pipeline's return status is the value of the last (rightmost) command to exit with a non-zero status, or zero if all commands exit successfully.
    set -o pipefail
    count=1
    COMMAND="reset && $@"
    while eval $COMMAND; do
        ((count++))
        echo $count > '../testIterations.txt'
        docker kill $(docker ps -q)
    done
    echo "Number of executions: $count"
}

docker kill $(docker ps -q) || true

cd $ROOT/core
#mvn clean install -DskipTests -Dcheckstyle.skip=true

cd $ROOT/query/
#mvn clean install -DskipTests -Dcheckstyle.skip=true

cd $ROOT/server/runtime
#mvn clean install -DskipTests -Dcheckstyle.skip=true

cd $ROOT/server/testdriver/core
#mvn clean install -DskipTests -Dcheckstyle.skip=true

cd $ROOT/server/tests
untilFailure "mvn verify -Dit.test=GracefulShutdownRestartIT -Dcheckstyle.skip=true -Dorg.infinispan.test.server.container.logFile=log4j2-trace.xml"
