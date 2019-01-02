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

import org.apache.ratis.thirdparty.io.grpc.Server;
import org.apache.ratis.thirdparty.io.grpc.ServerBuilder;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Demo server with shaded ratis thirdparty jar.
 */
public class GrpcServer {
  private static final Logger LOG =
      LoggerFactory.getLogger(GrpcServer.class.getName());

  private Server server;
  private int port;

  public GrpcServer(int port) {
    this.port = port;
  }

  void start() throws IOException {
    server = ServerBuilder.forPort(port)
        .addService(new GreeterImpl())
        .build()
        .start();
    LOG.info("GrpcServer started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        LOG.info("*** shutting down gRPC server since JVM is " +
            "shutting down");
        GrpcServer.this.stop();
      LOG.info("*** gRPC demo server shut down complete");
    }));
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    final GrpcServer server = new GrpcServer(50001);
    server.start();
    server.blockUntilShutdown();
  }
}