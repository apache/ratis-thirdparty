/**
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
package org.apache.ratis.thirdparty.demo.grpc;

import org.apache.ratis.thirdparty.demo.common.SslClientConfig;
import org.apache.ratis.thirdparty.demo.common.SslServerConfig;
import org.apache.ratis.thirdparty.demo.common.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class GrpcSslTest {
  private final static Logger LOG = LoggerFactory.getLogger(GrpcSslTest.class);


  @Test
  public void testSslClientServer() throws InterruptedException, IOException {
    final int port = TestUtils.randomPort();
    final SslServerConfig sslServerConf = TestUtils.newSslServerConfig(true, false);
    GrpcSslServer server = new GrpcSslServer(port, sslServerConf);
    try {
      server.start();
    } catch (Throwable t) {
      LOG.error("error starting server", t);
      throw t;
    }

    final SslClientConfig sslClientConfig = TestUtils.newSslClientConfig(true, false);

    GrpcSslClient client = new GrpcSslClient(
        "localhost", port, sslClientConfig);
    try {
      /* Access a service running on the local machine on port 50051 */
      String user = "testuser";
      String response = client.greet(user);
      LOG.info("Greet result: {}", response);
      Assert.assertEquals("Hello " + user, response);
    } finally {
      client.shutdown();
    }
  }
}
