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

import org.apache.ratis.thirdparty.io.grpc.netty.GrpcSslContexts;
import org.apache.ratis.thirdparty.io.grpc.netty.NettyServerBuilder;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.ClientAuth;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.ratis.thirdparty.io.netty.handler.ssl.SslProvider.OPENSSL;

/**
 * gRPC Demo SSL server with shaded ratis thirdparty jar.
 */
public class GrpcSslServer {
  private static final Logger LOG =
      LoggerFactory.getLogger(GrpcServer.class.getName());

  private Server server;
  private int port;
  private GrpcSslServerConfig conf;

  public GrpcSslServer(int port, GrpcSslServerConfig conf) {
    this.port = port;
    this.conf = conf;
  }

  void start() throws IOException {
    NettyServerBuilder nettyServerBuilder =
        NettyServerBuilder.forPort(port).addService(new GreeterImpl());
    SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(
        conf.getServerCertChain(), conf.getPrivateKey());
    if (conf.isMutualAuthn()) {
      sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
      sslContextBuilder.trustManager(conf.getClientCertChain());
    }
    sslContextBuilder =
        GrpcSslContexts.configure(sslContextBuilder, OPENSSL);
    if (conf.encryptionEnabled()) {
      sslContextBuilder.ciphers(conf.getTlsCipherSuitesWithEncryption());
    } else {
      sslContextBuilder.ciphers(conf.getTlsCipherSuitesNoEncryption());
    }

    nettyServerBuilder.sslContext(sslContextBuilder.build());

    server = nettyServerBuilder.build().start();
    LOG.info("GrpcSslServer started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("*** shutting down GrpcSslServer since JVM is " +
          "shutting down");
      GrpcSslServer.this.stop();
      LOG.info("*** GrpcSslServer shut down complete");
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
}
