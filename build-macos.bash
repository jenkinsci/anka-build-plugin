#!/bin/bash
set -exo pipefail
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_DIR/_ci
docker build -t anka-build-plugin .
cd $SCRIPT_DIR
docker run \
--rm --name anka-build-plugin -it \
-v ${SCRIPT_DIR}:/home/ubuntu/anka-build-plugin \
-v ${HOME}/.mvn:/home/ubuntu/.mvn \
anka-build-plugin /bin/bash -c "cd /home/ubuntu/anka-build-plugin && mvn -Dmaven.repo.local=\${HOME}/.mvn package"
