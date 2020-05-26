/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.auth;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.UPGRADE;

import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.auth.Authenticator;
import org.apache.tinkerpop.gremlin.server.handler.HttpBasicAuthenticationHandler;
import org.apache.tinkerpop.gremlin.server.handler.SaslAuthenticationHandler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMessage;

/**
 * An Authentication Handler for doing WebSocket and Http Basic auth
 * TODO: remove this class after fixed SaslAndHttpBasicAuthenticationHandler
 */
@ChannelHandler.Sharable
public class WsAndHttpBasicAuthHandler extends SaslAuthenticationHandler {

    private static final String AUTHENTICATOR = "authenticator";
    private static final String HTTP_AUTH = "http-authentication";

    public WsAndHttpBasicAuthHandler(Authenticator authenticator,
                                     Settings.AuthenticationSettings settings) {
        super(authenticator, settings);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object obj)
                            throws Exception {
        if (obj instanceof HttpMessage && !isWebSocket((HttpMessage) obj)) {
            ChannelPipeline pipeline = ctx.pipeline();
            ChannelHandler authHandler = pipeline.get(HTTP_AUTH);
            if (authHandler != null) {
                pipeline.remove(HTTP_AUTH);
            }
            authHandler = new HttpBasicAuthenticationHandler(
                          this.authenticator,  this.authenticationSettings);
            pipeline.addAfter(AUTHENTICATOR, HTTP_AUTH, authHandler);
            ctx.fireChannelRead(obj);
        } else {
            super.channelRead(ctx, obj);
        }
    }

    public static boolean isWebSocket(final HttpMessage msg) {
        final String connectionHeader = msg.headers().get(CONNECTION);
        final String upgradeHeader = msg.headers().get(UPGRADE);
        return "Upgrade".equalsIgnoreCase(connectionHeader) ||
               "WebSocket".equalsIgnoreCase(upgradeHeader);
    }
}
