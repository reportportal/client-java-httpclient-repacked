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
import java.net.URI;
import java.util.List;

import com.epam.reportportal.apache.http.localserver.LocalTestServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.epam.reportportal.apache.http.HttpEntity;
import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpStatus;
import com.epam.reportportal.apache.http.HttpVersion;
import com.epam.reportportal.apache.http.client.CookieStore;
import com.epam.reportportal.apache.http.client.methods.HttpGet;
import com.epam.reportportal.apache.http.client.protocol.HttpClientContext;
import com.epam.reportportal.apache.http.cookie.Cookie;
import com.epam.reportportal.apache.http.impl.client.BasicCookieStore;
import com.epam.reportportal.apache.http.impl.client.HttpClients;
import com.epam.reportportal.apache.http.message.BasicHeader;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.protocol.HttpRequestHandler;
import com.epam.reportportal.apache.http.util.EntityUtils;

/**
 * This class tests cookie matching when using Virtual Host.
 */
public class TestCookieVirtualHost extends IntegrationTestBase {

    @Before
    public void setUp() throws Exception {
        this.localServer = new LocalTestServer(null, null);
        this.localServer.registerDefaultHandlers();
        this.localServer.start();
    }

    @Test
    public void testCookieMatchingWithVirtualHosts() throws Exception {
        this.localServer.register("*", new HttpRequestHandler() {
            public void handle(
                    final HttpRequest request,
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {

                final int n = Integer.parseInt(request.getFirstHeader("X-Request").getValue());
                switch (n) {
                case 1:
                    // Assert Host is forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    // Respond with Set-Cookie on virtual host domain. This
                    // should be valid.
                    response.addHeader(new BasicHeader("Set-Cookie",
                            "name1=value1; domain=mydomain.fr; path=/"));
                    break;

                case 2:
                    // Assert Host is still forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    // We should get our cookie back.
                    Assert.assertNotNull("We must get a cookie header",
                            request.getFirstHeader("Cookie"));
                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    break;

                case 3:
                    // Assert Host is forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    break;
                }
            }

        });

        this.httpclient = HttpClients.createDefault();

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        // First request : retrieve a domain cookie from remote server.
        URI uri = new URI("http://app.mydomain.fr");
        HttpRequest httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "1");
        final HttpResponse response1 = this.httpclient.execute(getServerHttp(),
                httpRequest, context);
        final HttpEntity e1 = response1.getEntity();
        EntityUtils.consume(e1);

        // We should have one cookie set on domain.
        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(1, cookies.size());
        Assert.assertEquals("name1", cookies.get(0).getName());

        // Second request : send the cookie back.
        uri = new URI("http://app.mydomain.fr");
        httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "2");
        final HttpResponse response2 = this.httpclient.execute(getServerHttp(),
                httpRequest, context);
        final HttpEntity e2 = response2.getEntity();
        EntityUtils.consume(e2);

        // Third request : Host header
        uri = new URI("http://app.mydomain.fr");
        httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "3");
        final HttpResponse response3 = this.httpclient.execute(getServerHttp(),
                httpRequest, context);
        final HttpEntity e3 = response3.getEntity();
        EntityUtils.consume(e3);
    }

}
