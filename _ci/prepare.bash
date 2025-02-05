#!/usr/bin/env bash
set -exo pipefail
echo "Preparing..."
apt update 
apt install --yes maven
# since 24.04 error: externally-managed-environment, so we need to create a virtualenv
# chown -R 1000:1000 /opt
# export VIRTUAL_ENV="/opt/${UNIQUE_NAME}"
# python3 -m venv $VIRTUAL_ENV
# export PATH="$VIRTUAL_ENV/bin:$PATH"
# echo "export PATH=\"$VIRTUAL_ENV/bin:$PATH\"" > ~/.bashrc
# cat << EOF > /tmp/requirements.txt
# shyaml==0.6.2
# robotframework==6.1.1
# robotframework-pabot==2.16.0
# robotframework-jsonlibrary==0.5.0
# robotframework-pythonlibcore==4.2.0
# robotframework-requests==0.9.5
# robotframework-seleniumlibrary==6.1.2
# robotframework-yamllibrary==0.2.8
# robotframework-stacktrace==0.4.1
# robotframework-sshlibrary==3.6.0
# ansible-core==2.17.1
# six==1.16.0
# paramiko==3.4.1
# EOF
# pip3 install -r /tmp/requirements.txt
# ansible-galaxy collection install community.general && ansible-galaxy collection install ansible.posix
# ansible-galaxy collection list
# chown -R 1000:1000 "${VIRTUAL_ENV}"
