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
package org.apache.ratis.thirdparty.demo.netty;

import org.apache.ratis.thirdparty.demo.common.SslClientConfig;
import org.apache.ratis.thirdparty.demo.common.SslServerConfig;
import org.apache.ratis.thirdparty.demo.common.TestUtils;
import org.apache.ratis.thirdparty.io.netty.buffer.ByteBuf;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.SslContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for demo netty client/server with ratis thirdparty.
 */
public class NettySslTest {
  private final static Logger LOG = LoggerFactory.getLogger(NettySslTest.class);

  @Test
  public void testNoSsl() throws Exception {
    testNetty(TestUtils.randomPort(), null, null);
  }

  @Test
  public void testSsl() throws Exception {
    testNetty(TestUtils.randomPort(),
        TestUtils.newSslServerConfig(true, false),
        TestUtils.newSslClientConfig(true, false));
  }

  static void testNetty(int port, SslServerConfig serverSslConf, SslClientConfig clientSslConf) throws Exception {
    final SslContext serverSslContext = serverSslConf == null? null
        : NettyUtils.newServerSslContextBuilder(serverSslConf).build();
    final SslContext clientSslContext = clientSslConf == null? null
        : NettyUtils.newClientSslContextBuilder(clientSslConf).build();

    final String message = "Hey, how are you?";
    final String[] words = message.split(" ");
    try (NettyServer server = new NettyServer(port, serverSslContext);
         NettyClient client = new NettyClient("localhost", port, clientSslContext)) {
      final List<CompletableFuture<String>> replyFutures = new ArrayList<>();
      for(String word : words) {
        final ByteBuf buf = NettyUtils.unpooledBuffer(word + " ");
        final CompletableFuture<String> f = client.writeAndFlush(buf);
        replyFutures.add(f);
      }
      for(int i = 0; i < replyFutures.size(); i++) {
        final CompletableFuture<String> future = replyFutures.get(i);
        final String reply = future.get(3, TimeUnit.SECONDS);
        LOG.info(reply);
        Assert.assertEquals(NettyServer.toReply(words[i]), reply);
      }
    }
  }
}
