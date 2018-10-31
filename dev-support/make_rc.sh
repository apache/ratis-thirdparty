#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# Script that assembles all you need to make an RC. Creates a source tarball
# and instruct you how to deploy the jar to central.
#
# To finish, check what was build.  If good copy to dist.apache.org and
# close the maven repos.  Call a vote.
#
# Presumes your settings.xml all set up so can sign artifacts published to mvn, etc.

set -e

# Set mvn and mvnopts
mvn=mvn
if [ "$MAVEN" != "" ]; then
  mvn="${MAVEN}"
fi
mvnopts="-Xmx1g"
if [ "$MAVEN_OPTS" != "" ]; then
  mvnopts="${MAVEN_OPTS}"
fi

mvnGet() {
  ${mvn} -q -Dexec.executable="echo" -Dexec.args="\${${1}}" --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.6.0:exec 2>/dev/null
}

mvnFun() {
  set -x
  MAVEN_OPTS="${mvnopts}" ${mvn} -Dmaven.repo.local=${repodir} $@
  set +x
}

# Check project name
projectname=$(mvnGet project.name)
if [ "${projectname}" = "Apache Ratis Thirdparty" ]; then
  echo
  echo "Prepare release artifacts for $projectname"
  echo
else
  echo "Unexpected project name \"${projectname}\"."
  echo
  echo "Please run this script ($0) under the root directory of Apache Ratis Thirdparty."
  exit 1;
fi

# Set projectdir and archivedir
projectdir=$(pwd)
echo "Project dir: ${projectdir}"
archivedir="${projectdir}/../`basename ${projectdir}`.`date -u +"%Y%m%d-%H%M%S"`"
echo "Archive dir: ${archivedir}"
if [ -d "${archivedir}" ]; then
  echo "${archivedir} already exists"
  exit 1;
fi

# Set repodir
repodir="${projectdir}/../`basename ${projectdir}`.repository"
mkdir "${repodir}"
echo "Repo dir: ${repodir}"
repodir=`cd ${repodir} > /dev/null; pwd`

mkdir "${archivedir}"
archivedir=`cd ${archivedir} > /dev/null; pwd`
artifactid=$(mvnGet project.artifactId)
# Need to include "incubating" in the artifact
version="$(mvnGet project.version)-incubating"

# Make sure to clean up all state before building the src-tarball
mvnFun clean

# Generate source tar.gz into archive
git archive --format=tar.gz --output="${archivedir}/${artifactid}-${version}-src.tar.gz" --prefix="${artifactid}-${version}/" HEAD

# Build and install Ratis-Thirdparty for the eventual mvn-deploy
# No "bin tarball"
mvnFun install -DskipTests -Papache-release

echo
echo "Generated artifacts successfully."
ls -l ${archivedir}
echo
echo "Check the content of ${archivedir}"
echo "If good, sign and push to dist.apache.org"
echo "  cd ${archivedir}"
echo '  for i in *.tar.gz; do echo $i; gpg --print-mds $i > $i.mds ; done'
echo '  for i in *.tar.gz; do echo $i; gpg --print-md SHA512 $i > $i.sha ; done'
echo '  for i in *.tar.gz; do echo $i; gpg --armor --output $i.asc --detach-sig $i ; done'
echo
echo "Check the content deployed to maven."
echo "If good, close the repo and record links of temporary staging repo"
echo "  ${mvn} deploy -DskipTests -Papache-release -Dmaven.repo.local=${repodir}"
echo
echo "If all good tag the RC"
echo
echo "Finally, you may want to remove archive dir and repo dir"
echo "  rm -rf ${archivedir}"
echo "  rm -rf ${repodir}"
