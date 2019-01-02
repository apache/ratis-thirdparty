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
package org.apache.ratis.thirdparty.demo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Unit test for demo gRPC client/server with ratis thirdparty.
 */
@RunWith(JUnit4.class)
public class GrpcTest {

  private final static Logger LOG = LoggerFactory.getLogger(GrpcTest.class);
  @Test
  public void testClientServer() throws InterruptedException {
    Thread serverThread = new Thread(() -> {
      GrpcServer server = new GrpcServer(50001);
      try {
        server.start();
        server.blockUntilShutdown();
      } catch (InterruptedException | IOException e) {
        e.printStackTrace();
      }
    });
    serverThread.start();

    GrpcClient client = new GrpcClient("localhost", 50001);
    try {
      /* Access a service running on the local machine on port 50051 */
      String user = "testuser";
      String response = client.greet(user);
      LOG.info("Greet result: {}", response);
      Assert.assertTrue(response.equals("Hello " + user));
    } finally {
      client.shutdown();
    }
  }
}
