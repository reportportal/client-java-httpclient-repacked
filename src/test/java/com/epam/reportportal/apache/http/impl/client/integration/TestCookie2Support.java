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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.epam.reportportal.apache.http.Header;
import com.epam.reportportal.apache.http.HttpEntity;
import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpStatus;
import com.epam.reportportal.apache.http.ProtocolVersion;
import com.epam.reportportal.apache.http.client.CookieStore;
import com.epam.reportportal.apache.http.client.methods.HttpGet;
import com.epam.reportportal.apache.http.client.protocol.HttpClientContext;
import com.epam.reportportal.apache.http.cookie.Cookie;
import com.epam.reportportal.apache.http.cookie.SM;
import com.epam.reportportal.apache.http.cookie.SetCookie2;
import com.epam.reportportal.apache.http.entity.StringEntity;
import com.epam.reportportal.apache.http.impl.client.BasicCookieStore;
import com.epam.reportportal.apache.http.impl.client.HttpClients;
import com.epam.reportportal.apache.http.message.BasicHeader;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.protocol.HttpRequestHandler;
import com.epam.reportportal.apache.http.util.EntityUtils;

/**
 * Cookie2 support tests.
 */
public class TestCookie2Support extends IntegrationTestBase {

    @Before
    public void setUp() throws Exception {
        startServer();
        this.httpclient = HttpClients.createDefault();
    }

    private static class CookieVer0Service implements HttpRequestHandler {

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final ProtocolVersion httpversion = request.getRequestLine().getProtocolVersion();
            response.setStatusLine(httpversion, HttpStatus.SC_OK);
            response.addHeader(new BasicHeader("Set-Cookie", "name1=value1; path=/test"));
            final StringEntity entity = new StringEntity("whatever");
            response.setEntity(entity);
        }

    }

    @Test
    public void testCookieVersionSupportHeader1() throws Exception {
        this.localServer.register("*", new CookieVer0Service());

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpget = new HttpGet("/test/");

        final HttpResponse response1 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e1 = response1.getEntity();
        EntityUtils.consume(e1);

        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(1, cookies.size());

        final HttpResponse response2 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e2 = response2.getEntity();
        EntityUtils.consume(e2);

        final HttpRequest reqWrapper = context.getRequest();

        final Header cookiesupport = reqWrapper.getFirstHeader("Cookie2");
        Assert.assertNotNull(cookiesupport);
        Assert.assertEquals("$Version=1", cookiesupport.getValue());
    }

    private static class CookieVer1Service implements HttpRequestHandler {

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final ProtocolVersion httpversion = request.getRequestLine().getProtocolVersion();
            response.setStatusLine(httpversion, HttpStatus.SC_OK);
            response.addHeader(new BasicHeader("Set-Cookie", "name1=value1; Path=\"/test\"; Version=1"));
            response.addHeader(new BasicHeader("Set-Cookie2", "name2=value2; Path=\"/test\"; Version=1"));
            final StringEntity entity = new StringEntity("whatever");
            response.setEntity(entity);
        }

    }

    @Test
    public void testCookieVersionSupportHeader2() throws Exception {
        this.localServer.register("*", new CookieVer1Service());

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpget = new HttpGet("/test/");

        final HttpResponse response1 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e1 = response1.getEntity();
        EntityUtils.consume(e1);

        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(2, cookies.size());

        final HttpResponse response2 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e2 = response2.getEntity();
        EntityUtils.consume(e2);

        final HttpRequest reqWrapper = context.getRequest();

        final Header cookiesupport = reqWrapper.getFirstHeader(SM.COOKIE2);
        Assert.assertNotNull(cookiesupport);
        Assert.assertEquals("$Version=1", cookiesupport.getValue());
    }

    private static class CookieVer2Service implements HttpRequestHandler {

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final ProtocolVersion httpversion = request.getRequestLine().getProtocolVersion();
            response.setStatusLine(httpversion, HttpStatus.SC_OK);
            response.addHeader(new BasicHeader("Set-Cookie2", "name2=value2; Path=\"/test\"; Version=2"));
            final StringEntity entity = new StringEntity("whatever");
            response.setEntity(entity);
        }

    }

    @Test
    public void testCookieVersionSupportHeader3() throws Exception {
        this.localServer.register("*", new CookieVer2Service());

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpget = new HttpGet("/test/");

        final HttpResponse response1 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e1 = response1.getEntity();
        EntityUtils.consume(e1);

        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(1, cookies.size());

        final HttpResponse response2 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e2 = response2.getEntity();
        EntityUtils.consume(e2);

        final HttpRequest reqWrapper = context.getRequest();

        final Header cookiesupport = reqWrapper.getFirstHeader("Cookie2");
        Assert.assertNotNull(cookiesupport);
        Assert.assertEquals("$Version=1", cookiesupport.getValue());
    }

    private static class SetCookieVersionMixService implements HttpRequestHandler {

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final ProtocolVersion httpversion = request.getRequestLine().getProtocolVersion();
            response.setStatusLine(httpversion, HttpStatus.SC_OK);
            response.addHeader(new BasicHeader("Set-Cookie", "name=wrong; Path=/test"));
            response.addHeader(new BasicHeader("Set-Cookie2", "name=right; Path=\"/test\"; Version=1"));
            final StringEntity entity = new StringEntity("whatever");
            response.setEntity(entity);
        }

    }

    @Test
    public void testSetCookieVersionMix() throws Exception {
        this.localServer.register("*", new SetCookieVersionMixService());

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpget = new HttpGet("/test/");

        final HttpResponse response1 = this.httpclient.execute(getServerHttp(), httpget, context);
        final HttpEntity e1 = response1.getEntity();
        EntityUtils.consume(e1);

        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(1, cookies.size());
        Assert.assertEquals("right", cookies.get(0).getValue());
        Assert.assertTrue(cookies.get(0) instanceof SetCookie2);
    }

}
