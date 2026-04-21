#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}/_ci"
docker build --no-cache -t anka-build-plugin --load .
cd "${SCRIPT_DIR}"

# Optional extra Maven arguments (host env), e.g. MVN_EXTRA_ARGS="-DskipTests"
docker run \
  --rm \
  --name anka-build-plugin \
  -v "${SCRIPT_DIR}:/root/anka-build-plugin" \
  -v "${HOME}/.mvn:/root/.mvn" \
  -v "${HOME}/.m2:/root/.m2" \
  -e MVN_EXTRA_ARGS="${MVN_EXTRA_ARGS:-}" \
  anka-build-plugin \
  /bin/bash -c 'cd /root/anka-build-plugin && mvn -Daether.remoteRepositoryFilter.prefixes=false clean package ${MVN_EXTRA_ARGS}'
