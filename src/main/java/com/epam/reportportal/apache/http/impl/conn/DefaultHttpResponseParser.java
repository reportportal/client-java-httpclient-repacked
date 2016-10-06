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

package com.epam.reportportal.apache.http.impl.conn;

import java.io.IOException;

import com.epam.reportportal.apache.http.annotation.NotThreadSafe;
import com.epam.reportportal.apache.http.config.MessageConstraints;
import com.epam.reportportal.apache.http.io.SessionInputBuffer;
import com.epam.reportportal.apache.http.message.BasicLineParser;
import com.epam.reportportal.apache.http.message.LineParser;
import com.epam.reportportal.apache.http.message.ParserCursor;
import com.epam.reportportal.apache.http.util.Args;
import com.epam.reportportal.apache.http.util.CharArrayBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpResponseFactory;
import com.epam.reportportal.apache.http.NoHttpResponseException;
import com.epam.reportportal.apache.http.ProtocolException;
import com.epam.reportportal.apache.http.StatusLine;
import com.epam.reportportal.apache.http.impl.DefaultHttpResponseFactory;
import com.epam.reportportal.apache.http.impl.io.AbstractMessageParser;
import com.epam.reportportal.apache.http.params.HttpParams;

/**
 * Lenient HTTP response parser implementation that can skip malformed data until
 * a valid HTTP response message head is encountered.
 *
 * @since 4.2
 */
@SuppressWarnings("deprecation")
@NotThreadSafe
public class DefaultHttpResponseParser extends AbstractMessageParser<HttpResponse> {

    private final Log log = LogFactory.getLog(getClass());

    private final HttpResponseFactory responseFactory;
    private final CharArrayBuffer lineBuf;

    /**
     * @deprecated (4.3) use {@link DefaultHttpResponseParser#DefaultHttpResponseParser(
     *   SessionInputBuffer, LineParser, HttpResponseFactory, MessageConstraints)}
     */
    @Deprecated
    public DefaultHttpResponseParser(
            final SessionInputBuffer buffer,
            final LineParser parser,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        super(buffer, parser, params);
        Args.notNull(responseFactory, "Response factory");
        this.responseFactory = responseFactory;
        this.lineBuf = new CharArrayBuffer(128);
    }

    /**
     * Creates new instance of DefaultHttpResponseParser.
     *
     * @param buffer the session input buffer.
     * @param lineParser the line parser. If <code>null</code>
     *   {@link BasicLineParser#INSTANCE} will be used.
     * @param responseFactory HTTP response factory. If <code>null</code>
     *   {@link DefaultHttpResponseFactory#INSTANCE} will be used.
     * @param constraints the message constraints. If <code>null</code>
     *   {@link MessageConstraints#DEFAULT} will be used.
     *
     * @since 4.3
     */
    public DefaultHttpResponseParser(
            final SessionInputBuffer buffer,
            final LineParser lineParser,
            final HttpResponseFactory responseFactory,
            final MessageConstraints constraints) {
        super(buffer, lineParser, constraints);
        this.responseFactory = responseFactory != null ? responseFactory :
                DefaultHttpResponseFactory.INSTANCE;
        this.lineBuf = new CharArrayBuffer(128);
    }

    /**
     * Creates new instance of DefaultHttpResponseParser.
     *
     * @param buffer the session input buffer.
     * @param constraints the message constraints. If <code>null</code>
     *   {@link MessageConstraints#DEFAULT} will be used.
     *
     * @since 4.3
     */
    public DefaultHttpResponseParser(
        final SessionInputBuffer buffer, final MessageConstraints constraints) {
        this(buffer, null, null, constraints);
    }

    /**
     * Creates new instance of DefaultHttpResponseParser.
     *
     * @param buffer the session input buffer.
     *
     * @since 4.3
     */
    public DefaultHttpResponseParser(final SessionInputBuffer buffer) {
        this(buffer, null, null, MessageConstraints.DEFAULT);
    }

    @Override
    protected HttpResponse parseHead(
            final SessionInputBuffer sessionBuffer) throws IOException, HttpException {
        //read out the HTTP status string
        int count = 0;
        ParserCursor cursor = null;
        do {
            // clear the buffer
            this.lineBuf.clear();
            final int i = sessionBuffer.readLine(this.lineBuf);
            if (i == -1 && count == 0) {
                // The server just dropped connection on us
                throw new NoHttpResponseException("The target server failed to respond");
            }
            cursor = new ParserCursor(0, this.lineBuf.length());
            if (lineParser.hasProtocolVersion(this.lineBuf, cursor)) {
                // Got one
                break;
            } else if (i == -1 || reject(this.lineBuf, count)) {
                // Giving up
                throw new ProtocolException("The server failed to respond with a " +
                        "valid HTTP response");
            }
            if (this.log.isDebugEnabled()) {
                this.log.debug("Garbage in response: " + this.lineBuf.toString());
            }
            count++;
        } while(true);
        //create the status line from the status string
        final StatusLine statusline = lineParser.parseStatusLine(this.lineBuf, cursor);
        return this.responseFactory.newHttpResponse(statusline, null);
    }

    protected boolean reject(final CharArrayBuffer line, final int count) {
        return false;
    }

}
