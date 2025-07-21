#!/bin/bash
set -exo pipefail
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_DIR/_ci
docker build --no-cache -t anka-build-plugin .
cd $SCRIPT_DIR
docker run \
--rm --name anka-build-plugin -it \
-v ${SCRIPT_DIR}:/root/anka-build-plugin \
-v ${HOME}/.mvn:/root/.mvn \
-v ${HOME}/.m2:/root/.m2 \
anka-build-plugin /bin/bash -c "cd /root/anka-build-plugin && mvn clean && mvn package"
