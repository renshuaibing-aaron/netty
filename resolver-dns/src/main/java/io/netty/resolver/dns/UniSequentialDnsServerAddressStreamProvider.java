/*
 * Copyright 2017 The Netty Project
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
package io.netty.resolver.dns;

import io.netty.util.internal.ObjectUtil;

/**
 * A {@link DnsServerAddressStreamProvider} which is backed by a singlereactor {@link DnsServerAddresses}.
 */
abstract class UniSequentialDnsServerAddressStreamProvider implements DnsServerAddressStreamProvider {
    private final DnsServerAddresses addresses;

    UniSequentialDnsServerAddressStreamProvider(DnsServerAddresses addresses) {
        this.addresses = ObjectUtil.checkNotNull(addresses, "addresses");
    }

    @Override
    public final DnsServerAddressStream nameServerAddressStream(String hostname) {
        return addresses.stream();
    }
}
