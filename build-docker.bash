#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}/_ci"
docker build --no-cache -t anka-build-plugin --load .
cd "${SCRIPT_DIR}"

# Jenkins' "Manage Plugins" page shows Plugin-Version verbatim from the HPI's
# MANIFEST.MF. When the project is a -SNAPSHOT, maven-hpi-plugin appends
# "(<plugin.version.description>)" to Plugin-Version. By default that's
# "private-<short-git-sha>-<user>", which doesn't tell you WHEN the snapshot
# was built. Override it with a UTC build timestamp + short git sha so the
# Jenkins UI shows e.g.:
#   Version 2.13.0-SNAPSHOT (built-20260422.2122-f9c67980)
PLUGIN_BUILD_TIMESTAMP_UTC="$(date -u +%Y%m%d.%H%M)"
PLUGIN_GIT_SHORT_SHA="$(git rev-parse --short=8 HEAD 2>/dev/null || echo unknown)"
PLUGIN_VERSION_DESCRIPTION="built-${PLUGIN_BUILD_TIMESTAMP_UTC}-${PLUGIN_GIT_SHORT_SHA}"
echo "[build-docker] plugin.version.description: ${PLUGIN_VERSION_DESCRIPTION}"

# Optional extra Maven arguments (host env), e.g. MVN_EXTRA_ARGS="-DskipTests"
docker run \
  --rm \
  --name anka-build-plugin \
  -v "${SCRIPT_DIR}:/root/anka-build-plugin" \
  -v "${HOME}/.mvn:/root/.mvn" \
  -v "${HOME}/.m2:/root/.m2" \
  -e MVN_EXTRA_ARGS="${MVN_EXTRA_ARGS:-}" \
  -e PLUGIN_VERSION_DESCRIPTION="${PLUGIN_VERSION_DESCRIPTION}" \
  anka-build-plugin \
  /bin/bash -c 'cd /root/anka-build-plugin && mvn -Daether.remoteRepositoryFilter.prefixes=false -Dplugin.version.description="${PLUGIN_VERSION_DESCRIPTION}" clean package ${MVN_EXTRA_ARGS}'
