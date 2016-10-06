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
package com.epam.reportportal.apache.http.client.protocol;

import com.epam.reportportal.apache.http.client.protocol.ResponseContentEncoding;
import org.junit.Assert;
import org.junit.Test;

import com.epam.reportportal.apache.http.HttpEntity;
import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpResponseInterceptor;
import com.epam.reportportal.apache.http.HttpVersion;
import com.epam.reportportal.apache.http.client.entity.DeflateDecompressingEntity;
import com.epam.reportportal.apache.http.client.entity.GzipDecompressingEntity;
import com.epam.reportportal.apache.http.entity.StringEntity;
import com.epam.reportportal.apache.http.message.BasicHttpResponse;
import com.epam.reportportal.apache.http.protocol.BasicHttpContext;
import com.epam.reportportal.apache.http.protocol.HttpContext;

public class TestResponseContentEncoding {

    @Test
    public void testContentEncodingNoEntity() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNull(entity);
    }

    @Test
    public void testNoContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("plain stuff");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof StringEntity);
    }

    @Test
    public void testGzipContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("encoded stuff");
        original.setContentEncoding("GZip");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof GzipDecompressingEntity);
    }

    @Test
    public void testGzipContentEncodingZeroLength() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("");
        original.setContentEncoding("GZip");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof StringEntity);
    }

    @Test
    public void testXGzipContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("encoded stuff");
        original.setContentEncoding("x-gzip");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof GzipDecompressingEntity);
    }

    @Test
    public void testDeflateContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("encoded stuff");
        original.setContentEncoding("deFlaTe");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof DeflateDecompressingEntity);
    }

    @Test
    public void testIdentityContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("encoded stuff");
        original.setContentEncoding("identity");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity instanceof StringEntity);
    }

    @Test(expected=HttpException.class)
    public void testUnknownContentEncoding() throws Exception {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        final StringEntity original = new StringEntity("encoded stuff");
        original.setContentEncoding("whatever");
        response.setEntity(original);
        final HttpContext context = new BasicHttpContext();

        final HttpResponseInterceptor interceptor = new ResponseContentEncoding();
        interceptor.process(response, context);
    }

}
