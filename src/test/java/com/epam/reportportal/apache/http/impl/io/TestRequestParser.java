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
package com.epam.reportportal.apache.http.impl.io;

import java.io.InterruptedIOException;

import org.junit.Assert;
import org.junit.Test;

import com.epam.reportportal.apache.http.ConnectionClosedException;
import com.epam.reportportal.apache.http.Consts;
import com.epam.reportportal.apache.http.Header;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpVersion;
import com.epam.reportportal.apache.http.RequestLine;
import com.epam.reportportal.apache.http.impl.SessionInputBufferMock;
import com.epam.reportportal.apache.http.io.SessionInputBuffer;

/**
 * Unit tests for {@link DefaultHttpRequestParser}.
 */
public class TestRequestParser {

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConstructorInput() throws Exception {
        new DefaultHttpRequestParser(null);
    }

    @Test
    public void testBasicMessageParsing() throws Exception {
        final String s =
            "GET / HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "User-Agent: whatever\r\n" +
            "Cookie: c1=stuff\r\n" +
            "\r\n";
        final SessionInputBuffer inbuffer = new SessionInputBufferMock(s, Consts.ASCII);

        final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(inbuffer);
        final HttpRequest httprequest = parser.parse();

        final RequestLine reqline = httprequest.getRequestLine();
        Assert.assertNotNull(reqline);
        Assert.assertEquals("GET", reqline.getMethod());
        Assert.assertEquals("/", reqline.getUri());
        Assert.assertEquals(HttpVersion.HTTP_1_1, reqline.getProtocolVersion());
        final Header[] headers = httprequest.getAllHeaders();
        Assert.assertEquals(3, headers.length);
    }

    @Test
    public void testConnectionClosedException() throws Exception {
        final SessionInputBuffer inbuffer = new SessionInputBufferMock(new byte[] {});

        final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(inbuffer);
        try {
            parser.parse();
            Assert.fail("ConnectionClosedException should have been thrown");
        } catch (final ConnectionClosedException expected) {
        }
    }

    @Test
    public void testMessageParsingTimeout() throws Exception {
        final String s =
            "GET \000/ HTTP/1.1\r\000\n" +
            "Host: loca\000lhost\r\n" +
            "User-Agent: whatever\r\n" +
            "Coo\000kie: c1=stuff\r\n" +
            "\000\r\n";
        final SessionInputBuffer inbuffer = new SessionInputBufferMock(
                new TimeoutByteArrayInputStream(s.getBytes("US-ASCII")), 16);

        final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(inbuffer);

        int timeoutCount = 0;

        HttpRequest httprequest = null;
        for (int i = 0; i < 10; i++) {
            try {
                httprequest = parser.parse();
                break;
            } catch (final InterruptedIOException ex) {
                timeoutCount++;
            }

        }
        Assert.assertNotNull(httprequest);
        Assert.assertEquals(5, timeoutCount);

        @SuppressWarnings("null") // httprequest cannot be null here
        final
        RequestLine reqline = httprequest.getRequestLine();
        Assert.assertNotNull(reqline);
        Assert.assertEquals("GET", reqline.getMethod());
        Assert.assertEquals("/", reqline.getUri());
        Assert.assertEquals(HttpVersion.HTTP_1_1, reqline.getProtocolVersion());
        final Header[] headers = httprequest.getAllHeaders();
        Assert.assertEquals(3, headers.length);
    }

}

