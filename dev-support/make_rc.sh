#!/usr/bin/env bash

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


# Script that assembles artifacts for release candidates.
# Run without arguments for help.
#
# Presumes your settings.xml all set up so can sign artifacts published to mvn, etc.

set -e -u
# Set mvn and mvnopts
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
source "${DIR}/find_maven.sh"
mvnopts="-Xmx1g"
if [ "$MAVEN_OPTS" != "" ]; then
  mvnopts="${MAVEN_OPTS}"
fi

mvnGet() {
  ${MVN} -q -Dexec.executable="echo" -Dexec.args="\${${1}}" --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:exec 2>/dev/null
}

if [[ -z "${RATISVERSION:-}" ]]; then
  echo "Please set the RATISVERSION environment variable (eg. export RATISVERSION=0.3.0)"
  exit 1
fi

if [[ -n "${RC:-}" ]]; then
  RC=${RC#-} # accept with dash, but remove it
fi

if [[ -z "${RC:-}" ]]; then
  echo "Please set the RC number (without dash). (eg. export RC='rc2')"
  exit 1
fi

if [[ -z "${CODESIGNINGKEY:-}" ]]; then
  echo "Please specify your signing key ID in the CODESIGNINGKEY environment variable"
  exit 1
fi

# Check project name
projectname=$(mvnGet project.name)
if [ "${projectname}" = "Apache Ratis Thirdparty" ]; then
  echo
  echo "Prepare release artifacts for $projectname ${RATISVERSION}-${RC}"
  echo
else
  echo "Unexpected project name \"${projectname}\"."
  echo
  echo "Please run this script ($0) in the root directory of Apache Ratis Thirdparty."
  exit 1
fi


# Set projectdir and archivedir
projectdir=$(pwd)
echo "Project dir: ${projectdir}"

# Set repodir
repodir=${MVN_REPO_DIR:-${projectdir}/../$(basename "${projectdir}").repository}
mkdir -p "${repodir}"
repodir=`cd ${repodir} > /dev/null; pwd`
echo "Repo dir: ${repodir}"

SVNDISTDIR=${SVNDISTDIR:-$projectdir/../svndist-ratis-thirdparty}
if [ ! -d "$SVNDISTDIR" ]; then
  svn co https://dist.apache.org/repos/dist/dev/ratis/thirdparty "$SVNDISTDIR"
fi

artifactid=$(mvnGet project.artifactId)
src_tar="${projectdir}/target/${artifactid}-${RATISVERSION}-src.tar.gz"

mvnFun() {
  MAVEN_OPTS="${mvnopts}" ${MVN} -Dmaven.repo.local="${repodir}" "$@"
}

1-prepare-src() {
  cd "$projectdir"
  git reset --hard
  git clean -fdx
  mvnFun versions:set -DnewVersion="$RATISVERSION" -DprocessAllModules
  git commit -a -m "[RELEASE] Update version to $RATISVERSION" || echo "<version> up to date, nothing to commit"

  git config user.signingkey "${CODESIGNINGKEY}"
  git tag -s -m "Release $RATISVERSION $RC" "${RATISVERSION}-${RC}"
  git reset --hard "${RATISVERSION}-${RC}"

  git clean -fdx

  mvnFun clean install -DskipTests=true -Prelease -Papache-release -Dgpg.keyname="${CODESIGNINGKEY}"

  # Generate source tar.gz into archive
  git archive --format=tar.gz --output="${src_tar}" --prefix="${artifactid}-${RATISVERSION}/" HEAD
}

2-verify-bin() {
  WORKINGDIR="${projectdir}/../$(basename "${projectdir}").${RATISVERSION}-${RC}"
  rm -rf "$WORKINGDIR"
  mkdir -p "$WORKINGDIR"
  echo "Extracting source tarball to $(pwd)"
  cd "$WORKINGDIR"
  tar zvxf "${src_tar}"
  cd "${artifactid}-${RATISVERSION}"

  mvnFun clean verify -DskipTests=true -Prelease -Papache-release -Dgpg.keyname="${CODESIGNINGKEY}" "$@"
}

3-publish-mvn() {
  cd "$projectdir"
  mvnFun verify artifact:compare deploy:deploy -DdeployAtEnd=true -DskipTests=true -Prelease -Papache-release -Dgpg.keyname="${CODESIGNINGKEY}" "$@"
}

4-assembly() {
  cd "$SVNDISTDIR"
  RCDIR="${SVNDISTDIR}/${RATISVERSION}/${RC}"
  mkdir -p "$RCDIR"
  cd "$RCDIR"
  cp -v "${src_tar}" ./
  for i in *.tar.gz; do
    gpg -u "${CODESIGNINGKEY}" --armor --output "${i}.asc" --detach-sig "${i}"
    gpg --print-md SHA512 "${i}" > "${i}.sha512"
    gpg --print-mds "${i}" > "${i}.mds"
  done
  cd "$SVNDISTDIR"
  # skip svn add in CI
  if [[ -z "${CI:-}" ]]; then
    svn add "${RATISVERSION}" || svn add "${RATISVERSION}/${RC}"
  fi
}

5-publish-git(){
  cd "$projectdir"
  git push git@github.com:apache/ratis-thirdparty.git HEAD:"release-${RATISVERSION}"
  git push git@github.com:apache/ratis-thirdparty.git "${RATISVERSION}-${RC}"
}

6-publish-svn() {
  cd "${SVNDISTDIR}"
  svn commit -m "Propose Ratis Thirdparty release ${RATISVERSION}-${RC}"
}

7-draft-vote() {
  local staging_repo="${1:-}"
  if [[ -z "${staging_repo}" ]]; then
    echo "Missing required argument: repository ID"
    echo "Example: orgapacheratis-1179"
    exit 1
  fi
  cd "$projectdir"
  cat <<EOF
Subject: [VOTE] Apache Ratis Thirdparty Release ${RATISVERSION} ${RC}

Hi Apache Ratis community,

I am calling a vote for Apache Ratis Thirdparty Release ${RATISVERSION} ${RC}.

The git tag to be voted upon:
https://github.com/apache/ratis-thirdparty/tree/${RATISVERSION}-${RC}

The git commit hash:
$(git rev-parse HEAD)

Source tarball can be found at:
https://dist.apache.org/repos/dist/dev/ratis/thirdparty/${RATISVERSION}/${RC}/

The fingerprint of the PGP key the release artifacts are signed with:
$(gpg --fingerprint "${CODESIGNINGKEY}" | grep -o '\w\{4\} \w\{4\} \w\{4\} \w\{4\} \w\{4\}  \w\{4\} \w\{4\} \w\{4\} \w\{4\} \w\{4\}')

My public key to verify signatures can be found in:
https://dist.apache.org/repos/dist/release/ratis/KEYS

Maven artifacts are staged at:
https://repository.apache.org/content/repositories/${staging_repo}

This vote will remain open for at least 72 hours.

Please vote on releasing this RC. Thank you in advance.

[ ] +1 approve
[ ] +0 no opinion
[ ] -1 disapprove (and reason why)
EOF
}

if [ "$#" -lt 1 ]; then
  cat << EOF

Please choose from available phases (eg. make_rc.sh 1-prepare-src):

   1-prepare-src:  This is the first step. It modifies the mvn version, creates the git tag and
                   builds the project to create the source artifacts.
                   IT INCLUDES A GIT RESET + CLEAN. ALL LOCAL CHANGES WILL BE LOST!

   2-verify-bin:   The source artifact is copied to $WORKINGDIR and the project built from source.
                   This is an additional check as the the released source artifact should be enough to build the whole project.

   3-publish-mvn:  Performs the final build, and uploads the artifacts to the maven staging repository

   4-assembly:     This step copies all the required artifacts to the svn directory ($SVNDISTDIR) and creates the signatures/checksum files.

   5-publish-git:  Only do it if everything is fine. It pushes the rc tag and release branch to the repository.

   6-publish-svn:  Uploads the artifacts to the apache dev staging area.

   7-draft-vote:   Prints draft vote email.  Requires argument: staging Maven repository ID (e.g. orgapacheratis-1179).

The next steps of the release process are not scripted:

   7. Close the staging maven repository at https://repository.apache.org/

   8. Send out the vote mail to the ratis-dev list

   9. Summarize the vote after the given period.

   10. (If the vote passed): Move the staged artifacts in svn from the dev area to the dist area.

   11. Publish maven repository at https://repository.apache.org/





EOF
else
  set -x
  func="$1"
  shift
  eval "$func" "$@"
fi
