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
package com.epam.reportportal.apache.http.impl.auth;

import com.epam.reportportal.apache.http.impl.auth.BasicScheme;
import org.junit.Assert;
import org.junit.Test;

import com.epam.reportportal.apache.commons.codec.binary.Base64;
import com.epam.reportportal.apache.http.Consts;
import com.epam.reportportal.apache.http.Header;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.auth.AUTH;
import com.epam.reportportal.apache.http.auth.AuthScheme;
import com.epam.reportportal.apache.http.auth.MalformedChallengeException;
import com.epam.reportportal.apache.http.auth.UsernamePasswordCredentials;
import com.epam.reportportal.apache.http.message.BasicHeader;
import com.epam.reportportal.apache.http.message.BasicHttpRequest;
import com.epam.reportportal.apache.http.protocol.BasicHttpContext;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.util.EncodingUtils;

/**
 * Basic authentication test cases.
 */
public class TestBasicScheme {

    @Test(expected=MalformedChallengeException.class)
    public void testBasicAuthenticationWithNoRealm() throws Exception {
        final String challenge = "Basic";
        final Header header = new BasicHeader(AUTH.WWW_AUTH, challenge);
        final AuthScheme authscheme = new BasicScheme();
        authscheme.processChallenge(header);
    }

    @Test
    public void testBasicAuthenticationWith88591Chars() throws Exception {
        final int[] germanChars = { 0xE4, 0x2D, 0xF6, 0x2D, 0xFc };
        final StringBuilder buffer = new StringBuilder();
        for (final int germanChar : germanChars) {
            buffer.append((char)germanChar);
        }

        final UsernamePasswordCredentials creds = new UsernamePasswordCredentials("dh", buffer.toString());
        final BasicScheme authscheme = new BasicScheme(Consts.ISO_8859_1);

        final HttpRequest request = new BasicHttpRequest("GET", "/");
        final HttpContext context = new BasicHttpContext();
        final Header header = authscheme.authenticate(creds, request, context);
        Assert.assertEquals("Basic ZGg65C32Lfw=", header.getValue());
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        final UsernamePasswordCredentials creds =
            new UsernamePasswordCredentials("testuser", "testpass");

        final Header challenge = new BasicHeader(AUTH.WWW_AUTH, "Basic realm=\"test\"");

        final BasicScheme authscheme = new BasicScheme();
        authscheme.processChallenge(challenge);

        final HttpRequest request = new BasicHttpRequest("GET", "/");
        final HttpContext context = new BasicHttpContext();
        final Header authResponse = authscheme.authenticate(creds, request, context);

        final String expected = "Basic " + EncodingUtils.getAsciiString(
            Base64.encodeBase64(EncodingUtils.getAsciiBytes("testuser:testpass")));
        Assert.assertEquals(AUTH.WWW_AUTH_RESP, authResponse.getName());
        Assert.assertEquals(expected, authResponse.getValue());
        Assert.assertEquals("test", authscheme.getRealm());
        Assert.assertTrue(authscheme.isComplete());
        Assert.assertFalse(authscheme.isConnectionBased());
    }

    @Test
    public void testBasicProxyAuthentication() throws Exception {
        final UsernamePasswordCredentials creds =
            new UsernamePasswordCredentials("testuser", "testpass");

        final Header challenge = new BasicHeader(AUTH.PROXY_AUTH, "Basic realm=\"test\"");

        final BasicScheme authscheme = new BasicScheme();
        authscheme.processChallenge(challenge);

        final HttpRequest request = new BasicHttpRequest("GET", "/");
        final HttpContext context = new BasicHttpContext();
        final Header authResponse = authscheme.authenticate(creds, request, context);

        final String expected = "Basic " + EncodingUtils.getAsciiString(
            Base64.encodeBase64(EncodingUtils.getAsciiBytes("testuser:testpass")));
        Assert.assertEquals(AUTH.PROXY_AUTH_RESP, authResponse.getName());
        Assert.assertEquals(expected, authResponse.getValue());
        Assert.assertEquals("test", authscheme.getRealm());
        Assert.assertTrue(authscheme.isComplete());
        Assert.assertFalse(authscheme.isConnectionBased());
    }

}
