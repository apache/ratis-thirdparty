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
import org.apache.ratis.thirdparty.io.grpc.netty.GrpcSslContexts;
import org.apache.ratis.thirdparty.io.netty.buffer.ByteBuf;
import org.apache.ratis.thirdparty.io.netty.buffer.Unpooled;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.ClientAuth;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.SslContextBuilder;

import java.nio.charset.StandardCharsets;

public interface NettyUtils {
  static ByteBuf unpooledBuffer(String s) {
    final ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(s.getBytes(StandardCharsets.UTF_8));
    return buf;
  }

  static String buffer2String(ByteBuf buf){
    try {
      return buf.toString(StandardCharsets.UTF_8);
    } finally {
      buf.release();
    }
  }

  static SslContextBuilder newServerSslContextBuilder(SslServerConfig conf) {
    final SslContextBuilder b = SslContextBuilder.forServer(conf.getServerCertChain(), conf.getPrivateKey());
    if (conf.isMutualAuthn()) {
      b.clientAuth(ClientAuth.REQUIRE);
      b.trustManager(conf.getClientCertChain());
    }
    if (conf.encryptionEnabled()) {
      b.ciphers(conf.getTlsCipherSuitesWithEncryption());
    } else {
      b.ciphers(conf.getTlsCipherSuitesNoEncryption());
    }
    return b;
  }

  static SslContextBuilder newClientSslContextBuilder(SslClientConfig conf) {
    final SslContextBuilder b = GrpcSslContexts.forClient();
    if (conf.getTrustCertCollection() != null) {
      b.trustManager(conf.getTrustCertCollection());
    }
    if (conf.isMutualAuthn()) {
      b.keyManager(conf.getCertChain(), conf.getPrivateKey());
    }
    if (conf.encryptionEnabled()) {
      b.ciphers(conf.getTlsCipherSuitesWithEncryption());
    } else {
      b.ciphers(conf.getTlsCipherSuitesNoEncryption());
    }
    return b;
  }
}