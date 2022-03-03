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

import org.apache.ratis.thirdparty.io.netty.bootstrap.ServerBootstrap;
import org.apache.ratis.thirdparty.io.netty.buffer.ByteBuf;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelFuture;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelHandlerContext;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelInboundHandler;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelInitializer;
import org.apache.ratis.thirdparty.io.netty.channel.ChannelPipeline;
import org.apache.ratis.thirdparty.io.netty.channel.EventLoopGroup;
import org.apache.ratis.thirdparty.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.ratis.thirdparty.io.netty.channel.socket.SocketChannel;
import org.apache.ratis.thirdparty.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.ratis.thirdparty.io.netty.handler.logging.LogLevel;
import org.apache.ratis.thirdparty.io.netty.handler.logging.LoggingHandler;
import org.apache.ratis.thirdparty.io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * gRPC Demo server with shaded ratis thirdparty jar.
 */
public class NettyServer implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(NettyServer.class);

  private final EventLoopGroup bossGroup = new NioEventLoopGroup(3);
  private final EventLoopGroup workerGroup = new NioEventLoopGroup(3);
  private final ChannelFuture channelFuture;

  public NettyServer(int port, SslContext sslContext) {
    this.channelFuture = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(getClass(), LogLevel.INFO))
        .childHandler(newChannelInitializer(sslContext))
        .bind(port)
        .syncUninterruptibly();
  }

  private ChannelInitializer<SocketChannel> newChannelInitializer(SslContext sslContext){
    return new ChannelInitializer<SocketChannel>(){
      @Override
      public void initChannel(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        if (sslContext != null) {
          p.addLast("ssl", sslContext.newHandler(ch.alloc()));
        }
        p.addLast(newServerHandler());
      }
    };
  }

  private ChannelInboundHandler newServerHandler(){
    return new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object obj) {
        if (obj instanceof ByteBuf) {
          final String s = NettyUtils.buffer2String((ByteBuf) obj);
          LOG.info("channelRead: " + s);
          for(String word : s.split(" ")) {
            ctx.writeAndFlush(NettyUtils.unpooledBuffer(toReply(word) + " "));
          }
        }
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        LOG.error(NettyServer.this.getClass().getSimpleName() + ": exceptionCaught", throwable);
        ctx.close();
      }
    };
  }

  static String toReply(String request) {
    return "[" + request + "]";
  }


  @Override
  public void close() {
    channelFuture.channel().close();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }
}