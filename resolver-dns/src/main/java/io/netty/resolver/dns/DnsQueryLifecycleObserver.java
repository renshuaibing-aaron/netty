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

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.util.internal.UnstableApi;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * This interface provides visibility into individual DNS queries. The lifecycle of an objects is as follows:
 * <ol>
 *     <li>Object creation</li>
 *     <li>{@link #queryCancelled(int)}</li>
 * </ol>
 * OR
 * <ol>
 *     <li>Object creation</li>
 *     <li>{@link #queryWritten(InetSocketAddress, ChannelFuture)}</li>
 *     <li>{@link #queryRedirected(List)} or {@link #queryCNAMEd(DnsQuestion)} or
 *     {@link #queryNoAnswer(DnsResponseCode)} or {@link #queryCancelled(int)} or
 *     {@link #queryFailed(Throwable)} or {@link #querySucceed()}</li>
 * </ol>
 * <p>
 * This interface can be used to track metrics for individual DNS servers. Methods which may lead to another DNS query
 * return an object of type {@link DnsQueryLifecycleObserver}. Implementations may use this to build a query tree to
 * understand the "sub queries" generated by a singlereactor query.
 */
@UnstableApi
public interface DnsQueryLifecycleObserver {
    /**
     * The query has been written.
     * @param dnsServerAddress The DNS server address which the query was sent to.
     * @param future The future which represents the status of the write operation for the DNS query.
     */
    void queryWritten(InetSocketAddress dnsServerAddress, ChannelFuture future);

    /**
     * The query may have been written but it was cancelled at some point.
     * @param queriesRemaining The number of queries remaining.
     */
    void queryCancelled(int queriesRemaining);

    /**
     * The query has been redirected to another list of DNS servers.
     * @param nameServers The name servers the query has been redirected to.
     * @return An observer for the new query which we may issue.
     */
    DnsQueryLifecycleObserver queryRedirected(List<InetSocketAddress> nameServers);

    /**
     * The query returned a CNAME which we may attempt to follow with a new query.
     * <p>
     * Note that multiple queries may be encountering a CNAME. For example a if both {@link DnsRecordType#AAAA} and
     * {@link DnsRecordType#A} are supported we may query for both.
     * @param cnameQuestion the question we would use if we issue a new query.
     * @return An observer for the new query which we may issue.
     */
    DnsQueryLifecycleObserver queryCNAMEd(DnsQuestion cnameQuestion);

    /**
     * The response to the query didn't provide the expected response code, but it didn't return
     * {@link DnsResponseCode#NXDOMAIN} so we may try to query again.
     * @param code the unexpected response code.
     * @return An observer for the new query which we may issue.
     */
    DnsQueryLifecycleObserver queryNoAnswer(DnsResponseCode code);

    /**
     * The following criteria are possible:
     * <ul>
     *     <li>IO Error</li>
     *     <li>Server responded with an invalid DNS response</li>
     *     <li>Server responded with a valid DNS response, but it didn't progress the resolution</li>
     * </ul>
     * @param cause The cause which for the failure.
     */
    void queryFailed(Throwable cause);

    /**
     * The query received the expected results.
     */
    void querySucceed();
}
