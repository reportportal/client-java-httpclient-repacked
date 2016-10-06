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

package com.epam.reportportal.apache.http.impl.cookie;

import com.epam.reportportal.apache.http.impl.cookie.NetscapeDraftHeaderParser;
import org.junit.Assert;
import org.junit.Test;

import com.epam.reportportal.apache.http.HeaderElement;
import com.epam.reportportal.apache.http.NameValuePair;
import com.epam.reportportal.apache.http.message.ParserCursor;
import com.epam.reportportal.apache.http.util.CharArrayBuffer;

/**
 * Unit tests for {@link NetscapeDraftHeaderParser}.
 */
public class TestNetscapeDraftHeaderParser {

    @Test
    public void testNetscapeCookieParsing() throws Exception {
        final NetscapeDraftHeaderParser parser = NetscapeDraftHeaderParser.DEFAULT;

        String s = "name  = value; test; test1 =  stuff,with,commas   ;" +
                " test2 =  \"stuff, stuff\"; test3=\"stuff";
        CharArrayBuffer buffer = new CharArrayBuffer(16);
        buffer.append(s);
        ParserCursor cursor = new ParserCursor(0, s.length());

        HeaderElement he = parser.parseHeader(buffer, cursor);
        Assert.assertEquals("name", he.getName());
        Assert.assertEquals("value", he.getValue());
        final NameValuePair[] params = he.getParameters();
        Assert.assertEquals("test", params[0].getName());
        Assert.assertEquals(null, params[0].getValue());
        Assert.assertEquals("test1", params[1].getName());
        Assert.assertEquals("stuff,with,commas", params[1].getValue());
        Assert.assertEquals("test2", params[2].getName());
        Assert.assertEquals("\"stuff, stuff\"", params[2].getValue());
        Assert.assertEquals("test3", params[3].getName());
        Assert.assertEquals("\"stuff", params[3].getValue());
        Assert.assertEquals(s.length(), cursor.getPos());
        Assert.assertTrue(cursor.atEnd());

        s = "  ";
        buffer = new CharArrayBuffer(16);
        buffer.append(s);
        cursor = new ParserCursor(0, s.length());
        he = parser.parseHeader(buffer, cursor);
        Assert.assertEquals("", he.getName());
        Assert.assertEquals(null, he.getValue());
    }

}
