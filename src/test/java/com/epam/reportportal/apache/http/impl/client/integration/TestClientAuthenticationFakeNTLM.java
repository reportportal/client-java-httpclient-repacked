/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.epam.reportportal.apache.http.impl.client.integration;

import java.io.IOException;

import com.epam.reportportal.apache.http.localserver.LocalTestServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpHeaders;
import com.epam.reportportal.apache.http.HttpHost;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpStatus;
import com.epam.reportportal.apache.http.HttpVersion;
import com.epam.reportportal.apache.http.auth.AuthScope;
import com.epam.reportportal.apache.http.auth.NTCredentials;
import com.epam.reportportal.apache.http.client.methods.HttpGet;
import com.epam.reportportal.apache.http.client.protocol.HttpClientContext;
import com.epam.reportportal.apache.http.impl.client.BasicCredentialsProvider;
import com.epam.reportportal.apache.http.impl.client.HttpClients;
import com.epam.reportportal.apache.http.message.BasicStatusLine;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.protocol.HttpRequestHandler;
import com.epam.reportportal.apache.http.util.EntityUtils;

/**
 * Unit tests for some of the NTLM auth functionality..
 */
public class TestClientAuthenticationFakeNTLM extends IntegrationTestBase {

    @Before
    public void setUp() throws Exception {
        this.localServer = new LocalTestServer(null, null);
    }

    static class NtlmResponseHandler implements HttpRequestHandler {

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            response.setStatusLine(new BasicStatusLine(
                    HttpVersion.HTTP_1_1,
                    HttpStatus.SC_UNAUTHORIZED,
                    "Authentication Required"));
            response.setHeader("Connection", "Keep-Alive");
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "NTLM");
        }
    }

    @Test
    public void testNTLMAuthenticationFailure() throws Exception {
        this.localServer.register("*", new NtlmResponseHandler());
        this.localServer.start();

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials("test", "test", null, null));

        this.httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        final HttpContext context = HttpClientContext.create();

        final HttpHost targethost = getServerHttp();
        final HttpGet httpget = new HttpGet("/");

        final HttpResponse response = this.httpclient.execute(targethost, httpget, context);
        EntityUtils.consume(response.getEntity());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED,
                response.getStatusLine().getStatusCode());
    }

    static class NtlmType2MessageResponseHandler implements HttpRequestHandler {

        private final String authenticateHeaderValue;

        public NtlmType2MessageResponseHandler(final String type2Message) {
            this.authenticateHeaderValue = "NTLM " + type2Message;
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            response.setStatusLine(new BasicStatusLine(
                    HttpVersion.HTTP_1_1,
                    HttpStatus.SC_UNAUTHORIZED,
                    "Authentication Required"));
            response.setHeader("Connection", "Keep-Alive");
            if (!request.containsHeader(HttpHeaders.AUTHORIZATION)) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "NTLM");
            } else {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, authenticateHeaderValue);
            }
        }
    }

    @Test
    public void testNTLMv1Type2Message() throws Exception {
        this.localServer.register("*", new NtlmType2MessageResponseHandler("TlRMTVNTUAACAA" +
                "AADAAMADgAAAAzggLiASNFZ4mrze8AAAAAAAAAAAAAAAAAAAAABgBwFwAAAA9T" +
                "AGUAcgB2AGUAcgA="));
        this.localServer.start();

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials("test", "test", null, null));

        this.httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        final HttpContext context = HttpClientContext.create();

        final HttpHost targethost = getServerHttp();
        final HttpGet httpget = new HttpGet("/");

        final HttpResponse response = this.httpclient.execute(targethost, httpget, context);
        EntityUtils.consume(response.getEntity());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED,
                response.getStatusLine().getStatusCode());
    }

    @Test
    public void testNTLMv2Type2Message() throws Exception {
        this.localServer.register("*", new NtlmType2MessageResponseHandler("TlRMTVNTUAACAA" +
                "AADAAMADgAAAAzgoriASNFZ4mrze8AAAAAAAAAACQAJABEAAAABgBwFwAAAA9T" +
                "AGUAcgB2AGUAcgACAAwARABvAG0AYQBpAG4AAQAMAFMAZQByAHYAZQByAAAAAAA="));
        this.localServer.start();

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials("test", "test", null, null));

        this.httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        final HttpContext context = HttpClientContext.create();

        final HttpHost targethost = getServerHttp();
        final HttpGet httpget = new HttpGet("/");

        final HttpResponse response = this.httpclient.execute(targethost, httpget, context);
        EntityUtils.consume(response.getEntity());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED,
                response.getStatusLine().getStatusCode());
    }

    static class NtlmType2MessageOnlyResponseHandler implements HttpRequestHandler {

        private final String authenticateHeaderValue;

        public NtlmType2MessageOnlyResponseHandler(final String type2Message) {
            this.authenticateHeaderValue = "NTLM " + type2Message;
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            response.setStatusLine(new BasicStatusLine(
                    HttpVersion.HTTP_1_1,
                    HttpStatus.SC_UNAUTHORIZED,
                    "Authentication Required"));
            response.setHeader("Connection", "Keep-Alive");
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, authenticateHeaderValue);
        }
    }

    @Test
    public void testNTLMType2MessageOnlyAuthenticationFailure() throws Exception {
        this.localServer.register("*", new NtlmType2MessageOnlyResponseHandler("TlRMTVNTUAACAA" +
                "AADAAMADgAAAAzggLiASNFZ4mrze8AAAAAAAAAAAAAAAAAAAAABgBwFwAAAA9T" +
                "AGUAcgB2AGUAcgA="));
        this.localServer.start();

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials("test", "test", null, null));

        this.httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        final HttpContext context = HttpClientContext.create();

        final HttpHost targethost = getServerHttp();
        final HttpGet httpget = new HttpGet("/");

        final HttpResponse response = this.httpclient.execute(targethost, httpget, context);
        EntityUtils.consume(response.getEntity());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED,
                response.getStatusLine().getStatusCode());
    }

    @Test
    public void testNTLMType2NonUnicodeMessageOnlyAuthenticationFailure() throws Exception {
        this.localServer.register("*", new NtlmType2MessageOnlyResponseHandler("TlRMTVNTUAACAA" +
                "AABgAGADgAAAAyggLiASNFZ4mrze8AAAAAAAAAAAAAAAAAAAAABgBwFwAAAA9T" +
                "ZXJ2ZXI="));
        this.localServer.start();

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new NTCredentials("test", "test", null, null));

        this.httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        final HttpContext context = HttpClientContext.create();

        final HttpHost targethost = getServerHttp();
        final HttpGet httpget = new HttpGet("/");

        final HttpResponse response = this.httpclient.execute(targethost, httpget, context);
        EntityUtils.consume(response.getEntity());
        Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED,
                response.getStatusLine().getStatusCode());
    }

}
