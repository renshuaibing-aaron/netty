/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.websocketx.extensions;


/**
 * Handshakes a client extension with the httpserver.
 */
public interface WebSocketClientExtensionHandshaker {

    /**
     * Return extension configuration to submit to the httpserver.
     *
     * @return the desired extension configuration.
     */
    WebSocketExtensionData newRequestData();

    /**
     * Handshake based on httpserver response. It should always succeed because httpserver response
     * should be a request acknowledge.
     *
     * @param extensionData
     *          the extension configuration sent by the httpserver.
     * @return an initialized extension if handshake phase succeed or null if failed.
     */
    WebSocketClientExtension handshakeExtension(WebSocketExtensionData extensionData);

}
