/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.thirdparty.demo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public interface TestUtils {
  Logger LOG = LoggerFactory.getLogger(TestUtils.class);

  ClassLoader CLASS_LOADER = TestUtils.class.getClassLoader();

  static File getResource(String name) {
    final File file = Optional.ofNullable(CLASS_LOADER.getResource(name))
        .map(URL::getFile)
        .map(File::new)
        .orElse(null);
    LOG.info("Getting Resource {}: {}", name, file);
    return file;
  }

  static SslServerConfig newSslServerConfig(boolean mutualAuthn, boolean encryption) {
    LOG.info("newSslServerConfig: mutualAuthn? {}, encryption? {}", mutualAuthn, encryption);
    return new SslServerConfig(
        getResource("ssl/server.pem"),
        getResource("ssl/server.crt"),
        getResource("ssl/client.crt"),
        mutualAuthn,
        encryption);
  }

  static SslClientConfig newSslClientConfig(boolean mutualAuthn, boolean encryption) {
    LOG.info("newSslClientConfig: mutualAuthn? {}, encryption? {}", mutualAuthn, encryption);
    return new SslClientConfig(
        getResource("ssl/client.pem"),
        getResource("ssl/ca.crt"),
        getResource("ssl/client.crt"),
        mutualAuthn,
        encryption);
  }

  static int randomPort() {
    final int port = 50000 + ThreadLocalRandom.current().nextInt(10000);
    LOG.info("randomPort: {}", port);
    return port;
  }
}
