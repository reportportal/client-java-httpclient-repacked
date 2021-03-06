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

package com.epam.reportportal.apache.http.impl.client;

import java.io.Closeable;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.epam.reportportal.apache.http.Header;
import com.epam.reportportal.apache.http.HttpHost;
import com.epam.reportportal.apache.http.HttpRequestInterceptor;
import com.epam.reportportal.apache.http.HttpResponseInterceptor;
import com.epam.reportportal.apache.http.annotation.NotThreadSafe;
import com.epam.reportportal.apache.http.auth.AuthScheme;
import com.epam.reportportal.apache.http.auth.AuthSchemeProvider;
import com.epam.reportportal.apache.http.client.config.AuthSchemes;
import com.epam.reportportal.apache.http.client.config.CookieSpecs;
import com.epam.reportportal.apache.http.client.config.RequestConfig;
import com.epam.reportportal.apache.http.config.ConnectionConfig;
import com.epam.reportportal.apache.http.config.Lookup;
import com.epam.reportportal.apache.http.config.RegistryBuilder;
import com.epam.reportportal.apache.http.config.SocketConfig;
import com.epam.reportportal.apache.http.conn.ConnectionKeepAliveStrategy;
import com.epam.reportportal.apache.http.conn.HttpClientConnectionManager;
import com.epam.reportportal.apache.http.conn.SchemePortResolver;
import com.epam.reportportal.apache.http.conn.routing.HttpRoutePlanner;
import com.epam.reportportal.apache.http.conn.socket.ConnectionSocketFactory;
import com.epam.reportportal.apache.http.conn.socket.LayeredConnectionSocketFactory;
import com.epam.reportportal.apache.http.conn.socket.PlainConnectionSocketFactory;
import com.epam.reportportal.apache.http.conn.ssl.SSLContexts;
import com.epam.reportportal.apache.http.conn.ssl.X509HostnameVerifier;
import com.epam.reportportal.apache.http.cookie.CookieSpec;
import com.epam.reportportal.apache.http.cookie.CookieSpecProvider;
import com.epam.reportportal.apache.http.impl.DefaultConnectionReuseStrategy;
import com.epam.reportportal.apache.http.impl.NoConnectionReuseStrategy;
import com.epam.reportportal.apache.http.util.TextUtils;
import com.epam.reportportal.apache.http.util.VersionInfo;
import com.epam.reportportal.apache.http.ConnectionReuseStrategy;
import com.epam.reportportal.apache.http.client.AuthenticationStrategy;
import com.epam.reportportal.apache.http.client.BackoffManager;
import com.epam.reportportal.apache.http.client.ConnectionBackoffStrategy;
import com.epam.reportportal.apache.http.client.CookieStore;
import com.epam.reportportal.apache.http.client.CredentialsProvider;
import com.epam.reportportal.apache.http.client.HttpRequestRetryHandler;
import com.epam.reportportal.apache.http.client.RedirectStrategy;
import com.epam.reportportal.apache.http.client.ServiceUnavailableRetryStrategy;
import com.epam.reportportal.apache.http.client.UserTokenHandler;
import com.epam.reportportal.apache.http.client.protocol.RequestAcceptEncoding;
import com.epam.reportportal.apache.http.client.protocol.RequestAddCookies;
import com.epam.reportportal.apache.http.client.protocol.RequestAuthCache;
import com.epam.reportportal.apache.http.client.protocol.RequestClientConnControl;
import com.epam.reportportal.apache.http.client.protocol.RequestDefaultHeaders;
import com.epam.reportportal.apache.http.client.protocol.ResponseContentEncoding;
import com.epam.reportportal.apache.http.client.protocol.ResponseProcessCookies;
import com.epam.reportportal.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.epam.reportportal.apache.http.impl.auth.BasicSchemeFactory;
import com.epam.reportportal.apache.http.impl.auth.DigestSchemeFactory;
import com.epam.reportportal.apache.http.impl.auth.KerberosSchemeFactory;
import com.epam.reportportal.apache.http.impl.auth.NTLMSchemeFactory;
import com.epam.reportportal.apache.http.impl.auth.SPNegoSchemeFactory;
import com.epam.reportportal.apache.http.impl.conn.DefaultProxyRoutePlanner;
import com.epam.reportportal.apache.http.impl.conn.DefaultRoutePlanner;
import com.epam.reportportal.apache.http.impl.conn.DefaultSchemePortResolver;
import com.epam.reportportal.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.epam.reportportal.apache.http.impl.conn.SystemDefaultRoutePlanner;
import com.epam.reportportal.apache.http.impl.cookie.BestMatchSpecFactory;
import com.epam.reportportal.apache.http.impl.cookie.BrowserCompatSpecFactory;
import com.epam.reportportal.apache.http.impl.cookie.IgnoreSpecFactory;
import com.epam.reportportal.apache.http.impl.cookie.NetscapeDraftSpecFactory;
import com.epam.reportportal.apache.http.impl.cookie.RFC2109SpecFactory;
import com.epam.reportportal.apache.http.impl.cookie.RFC2965SpecFactory;
import com.epam.reportportal.apache.http.impl.execchain.BackoffStrategyExec;
import com.epam.reportportal.apache.http.impl.execchain.ClientExecChain;
import com.epam.reportportal.apache.http.impl.execchain.MainClientExec;
import com.epam.reportportal.apache.http.impl.execchain.ProtocolExec;
import com.epam.reportportal.apache.http.impl.execchain.RedirectExec;
import com.epam.reportportal.apache.http.impl.execchain.RetryExec;
import com.epam.reportportal.apache.http.impl.execchain.ServiceUnavailableRetryExec;
import com.epam.reportportal.apache.http.protocol.HttpProcessor;
import com.epam.reportportal.apache.http.protocol.HttpProcessorBuilder;
import com.epam.reportportal.apache.http.protocol.HttpRequestExecutor;
import com.epam.reportportal.apache.http.protocol.RequestContent;
import com.epam.reportportal.apache.http.protocol.RequestTargetHost;
import com.epam.reportportal.apache.http.protocol.RequestUserAgent;

/**
 * Builder for {@link CloseableHttpClient} instances.
 * <p/>
 * When a particular component is not explicitly this class will
 * use its default implementation. System properties will be taken
 * into account when configuring the default implementations when
 * {@link #useSystemProperties()} method is called prior to calling
 * {@link #build()}.
 * <ul>
 *  <li>ssl.TrustManagerFactory.algorithm</li>
 *  <li>javax.net.ssl.trustStoreType</li>
 *  <li>javax.net.ssl.trustStore</li>
 *  <li>javax.net.ssl.trustStoreProvider</li>
 *  <li>javax.net.ssl.trustStorePassword</li>
 *  <li>ssl.KeyManagerFactory.algorithm</li>
 *  <li>javax.net.ssl.keyStoreType</li>
 *  <li>javax.net.ssl.keyStore</li>
 *  <li>javax.net.ssl.keyStoreProvider</li>
 *  <li>javax.net.ssl.keyStorePassword</li>
 *  <li>https.protocols</li>
 *  <li>https.cipherSuites</li>
 *  <li>http.proxyHost</li>
 *  <li>http.proxyPort</li>
 *  <li>http.nonProxyHosts</li>
 *  <li>http.keepAlive</li>
 *  <li>http.maxConnections</li>
 *  <li>http.agent</li>
 * </ul>
 * <p/>
 * Please note that some settings used by this class can be mutually
 * exclusive and may not apply when building {@link CloseableHttpClient}
 * instances.
 *
 * @since 4.3
 */
@NotThreadSafe
public class HttpClientBuilder {

    private HttpRequestExecutor requestExec;
    private X509HostnameVerifier hostnameVerifier;
    private LayeredConnectionSocketFactory sslSocketFactory;
    private SSLContext sslcontext;
    private HttpClientConnectionManager connManager;
    private SchemePortResolver schemePortResolver;
    private ConnectionReuseStrategy reuseStrategy;
    private ConnectionKeepAliveStrategy keepAliveStrategy;
    private AuthenticationStrategy targetAuthStrategy;
    private AuthenticationStrategy proxyAuthStrategy;
    private UserTokenHandler userTokenHandler;
    private HttpProcessor httpprocessor;

    private LinkedList<HttpRequestInterceptor> requestFirst;
    private LinkedList<HttpRequestInterceptor> requestLast;
    private LinkedList<HttpResponseInterceptor> responseFirst;
    private LinkedList<HttpResponseInterceptor> responseLast;

    private HttpRequestRetryHandler retryHandler;
    private HttpRoutePlanner routePlanner;
    private RedirectStrategy redirectStrategy;
    private ConnectionBackoffStrategy connectionBackoffStrategy;
    private BackoffManager backoffManager;
    private ServiceUnavailableRetryStrategy serviceUnavailStrategy;
    private Lookup<AuthSchemeProvider> authSchemeRegistry;
    private Lookup<CookieSpecProvider> cookieSpecRegistry;
    private CookieStore cookieStore;
    private CredentialsProvider credentialsProvider;
    private String userAgent;
    private HttpHost proxy;
    private Collection<? extends Header> defaultHeaders;
    private SocketConfig defaultSocketConfig;
    private ConnectionConfig defaultConnectionConfig;
    private RequestConfig defaultRequestConfig;

    private boolean systemProperties;
    private boolean redirectHandlingDisabled;
    private boolean automaticRetriesDisabled;
    private boolean contentCompressionDisabled;
    private boolean cookieManagementDisabled;
    private boolean authCachingDisabled;
    private boolean connectionStateDisabled;

    private int maxConnTotal = 0;
    private int maxConnPerRoute = 0;

    private List<Closeable> closeables;

    static final String DEFAULT_USER_AGENT;
    static {
        final VersionInfo vi = VersionInfo.loadVersionInfo
                ("org.apache.http.client", HttpClientBuilder.class.getClassLoader());
        final String release = (vi != null) ?
                vi.getRelease() : VersionInfo.UNAVAILABLE;
        DEFAULT_USER_AGENT = "Apache-HttpClient/" + release + " (java 1.5)";
    }

    public static HttpClientBuilder create() {
        return new HttpClientBuilder();
    }

    protected HttpClientBuilder() {
        super();
    }

    /**
     * Assigns {@link HttpRequestExecutor} instance.
     */
    public final HttpClientBuilder setRequestExecutor(final HttpRequestExecutor requestExec) {
        this.requestExec = requestExec;
        return this;
    }

    /**
     * Assigns {@link X509HostnameVerifier} instance.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} and the {@link #setSSLSocketFactory(
     *LayeredConnectionSocketFactory)} methods.
     */
    public final HttpClientBuilder setHostnameVerifier(final X509HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    /**
     * Assigns {@link SSLContext} instance.
     * <p/>
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} and the {@link #setSSLSocketFactory(
     *LayeredConnectionSocketFactory)} methods.
     */
    public final HttpClientBuilder setSslcontext(final SSLContext sslcontext) {
        this.sslcontext = sslcontext;
        return this;
    }

    /**
     * Assigns {@link LayeredConnectionSocketFactory} instance.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} method.
     */
    public final HttpClientBuilder setSSLSocketFactory(
            final LayeredConnectionSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    /**
     * Assigns maximum total connection value.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} method.
     */
    public final HttpClientBuilder setMaxConnTotal(final int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
        return this;
    }

    /**
     * Assigns maximum connection per route value.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} method.
     */
    public final HttpClientBuilder setMaxConnPerRoute(final int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    /**
     * Assigns default {@link SocketConfig}.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} method.
     */
    public final HttpClientBuilder setDefaultSocketConfig(final SocketConfig config) {
        this.defaultSocketConfig = config;
        return this;
    }

    /**
     * Assigns default {@link ConnectionConfig}.
     * <p/>
     * Please note this value can be overridden by the {@link #setConnectionManager(
     *HttpClientConnectionManager)} method.
     */
    public final HttpClientBuilder setDefaultConnectionConfig(final ConnectionConfig config) {
        this.defaultConnectionConfig = config;
        return this;
    }

    /**
     * Assigns {@link HttpClientConnectionManager} instance.
     */
    public final HttpClientBuilder setConnectionManager(
            final HttpClientConnectionManager connManager) {
        this.connManager = connManager;
        return this;
    }

    /**
     * Assigns {@link ConnectionReuseStrategy} instance.
     */
    public final HttpClientBuilder setConnectionReuseStrategy(
            final ConnectionReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
        return this;
    }

    /**
     * Assigns {@link ConnectionKeepAliveStrategy} instance.
     */
    public final HttpClientBuilder setKeepAliveStrategy(
            final ConnectionKeepAliveStrategy keepAliveStrategy) {
        this.keepAliveStrategy = keepAliveStrategy;
        return this;
    }

    /**
     * Assigns {@link AuthenticationStrategy} instance for proxy
     * authentication.
     */
    public final HttpClientBuilder setTargetAuthenticationStrategy(
            final AuthenticationStrategy targetAuthStrategy) {
        this.targetAuthStrategy = targetAuthStrategy;
        return this;
    }

    /**
     * Assigns {@link AuthenticationStrategy} instance for target
     * host authentication.
     */
    public final HttpClientBuilder setProxyAuthenticationStrategy(
            final AuthenticationStrategy proxyAuthStrategy) {
        this.proxyAuthStrategy = proxyAuthStrategy;
        return this;
    }

    /**
     * Assigns {@link UserTokenHandler} instance.
     * <p/>
     * Please note this value can be overridden by the {@link #disableConnectionState()}
     * method.
     */
    public final HttpClientBuilder setUserTokenHandler(final UserTokenHandler userTokenHandler) {
        this.userTokenHandler = userTokenHandler;
        return this;
    }

    /**
     * Disables connection state tracking.
     */
    public final HttpClientBuilder disableConnectionState() {
        connectionStateDisabled = true;
        return this;
    }

    /**
     * Assigns {@link SchemePortResolver} instance.
     */
    public final HttpClientBuilder setSchemePortResolver(
            final SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
        return this;
    }

    /**
     * Assigns <tt>User-Agent</tt> value.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Assigns default request header values.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder setDefaultHeaders(final Collection<? extends Header> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    /**
     * Adds this protocol interceptor to the head of the protocol processing list.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder addInterceptorFirst(final HttpResponseInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (responseFirst == null) {
            responseFirst = new LinkedList<HttpResponseInterceptor>();
        }
        responseFirst.addFirst(itcp);
        return this;
    }

    /**
     * Adds this protocol interceptor to the tail of the protocol processing list.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder addInterceptorLast(final HttpResponseInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (responseLast == null) {
            responseLast = new LinkedList<HttpResponseInterceptor>();
        }
        responseLast.addLast(itcp);
        return this;
    }

    /**
     * Adds this protocol interceptor to the head of the protocol processing list.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder addInterceptorFirst(final HttpRequestInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (requestFirst == null) {
            requestFirst = new LinkedList<HttpRequestInterceptor>();
        }
        requestFirst.addFirst(itcp);
        return this;
    }

    /**
     * Adds this protocol interceptor to the tail of the protocol processing list.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder addInterceptorLast(final HttpRequestInterceptor itcp) {
        if (itcp == null) {
            return this;
        }
        if (requestLast == null) {
            requestLast = new LinkedList<HttpRequestInterceptor>();
        }
        requestLast.addLast(itcp);
        return this;
    }

    /**
     * Disables state (cookie) management.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder disableCookieManagement() {
        this.cookieManagementDisabled = true;
        return this;
    }

    /**
     * Disables automatic content decompression.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder disableContentCompression() {
        contentCompressionDisabled = true;
        return this;
    }

    /**
     * Disables authentication scheme caching.
     * <p/>
     * Please note this value can be overridden by the {@link #setHttpProcessor(
     *HttpProcessor)} method.
     */
    public final HttpClientBuilder disableAuthCaching() {
        this.authCachingDisabled = true;
        return this;
    }

    /**
     * Assigns {@link HttpProcessor} instance.
     */
    public final HttpClientBuilder setHttpProcessor(final HttpProcessor httpprocessor) {
        this.httpprocessor = httpprocessor;
        return this;
    }

    /**
     * Assigns {@link HttpRequestRetryHandler} instance.
     * <p/>
     * Please note this value can be overridden by the {@link #disableAutomaticRetries()}
     * method.
     */
    public final HttpClientBuilder setRetryHandler(final HttpRequestRetryHandler retryHandler) {
        this.retryHandler = retryHandler;
        return this;
    }

    /**
     * Disables automatic request recovery and re-execution.
     */
    public final HttpClientBuilder disableAutomaticRetries() {
        automaticRetriesDisabled = true;
        return this;
    }

    /**
     * Assigns default proxy value.
     * <p/>
     * Please note this value can be overridden by the {@link #setRoutePlanner(
     *HttpRoutePlanner)} method.
     */
    public final HttpClientBuilder setProxy(final HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Assigns {@link HttpRoutePlanner} instance.
     */
    public final HttpClientBuilder setRoutePlanner(final HttpRoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
        return this;
    }

    /**
     * Assigns {@link RedirectStrategy} instance.
     * <p/>
     * Please note this value can be overridden by the {@link #disableRedirectHandling()}
     * method.
`     */
    public final HttpClientBuilder setRedirectStrategy(final RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
        return this;
    }

    /**
     * Disables automatic redirect handling.
     */
    public final HttpClientBuilder disableRedirectHandling() {
        redirectHandlingDisabled = true;
        return this;
    }

    /**
     * Assigns {@link ConnectionBackoffStrategy} instance.
     */
    public final HttpClientBuilder setConnectionBackoffStrategy(
            final ConnectionBackoffStrategy connectionBackoffStrategy) {
        this.connectionBackoffStrategy = connectionBackoffStrategy;
        return this;
    }

    /**
     * Assigns {@link BackoffManager} instance.
     */
    public final HttpClientBuilder setBackoffManager(final BackoffManager backoffManager) {
        this.backoffManager = backoffManager;
        return this;
    }

    /**
     * Assigns {@link ServiceUnavailableRetryStrategy} instance.
     */
    public final HttpClientBuilder setServiceUnavailableRetryStrategy(
            final ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
        this.serviceUnavailStrategy = serviceUnavailStrategy;
        return this;
    }

    /**
     * Assigns default {@link CookieStore} instance which will be used for
     * request execution if not explicitly set in the client execution context.
     */
    public final HttpClientBuilder setDefaultCookieStore(final CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        return this;
    }

    /**
     * Assigns default {@link CredentialsProvider} instance which will be used
     * for request execution if not explicitly set in the client execution
     * context.
     */
    public final HttpClientBuilder setDefaultCredentialsProvider(
            final CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    /**
     * Assigns default {@link AuthScheme} registry which will
     * be used for request execution if not explicitly set in the client execution
     * context.
     */
    public final HttpClientBuilder setDefaultAuthSchemeRegistry(
            final Lookup<AuthSchemeProvider> authSchemeRegistry) {
        this.authSchemeRegistry = authSchemeRegistry;
        return this;
    }

    /**
     * Assigns default {@link CookieSpec} registry which will
     * be used for request execution if not explicitly set in the client execution
     * context.
     */
    public final HttpClientBuilder setDefaultCookieSpecRegistry(
            final Lookup<CookieSpecProvider> cookieSpecRegistry) {
        this.cookieSpecRegistry = cookieSpecRegistry;
        return this;
    }

    /**
     * Assigns default {@link RequestConfig} instance which will be used
     * for request execution if not explicitly set in the client execution
     * context.
     */
    public final HttpClientBuilder setDefaultRequestConfig(final RequestConfig config) {
        this.defaultRequestConfig = config;
        return this;
    }

    /**
     * Use system properties when creating and configuring default
     * implementations.
     */
    public final HttpClientBuilder useSystemProperties() {
        systemProperties = true;
        return this;
    }

    /**
     * For internal use.
     */
    protected ClientExecChain decorateMainExec(final ClientExecChain mainExec) {
        return mainExec;
    }

    /**
     * For internal use.
     */
    protected ClientExecChain decorateProtocolExec(final ClientExecChain protocolExec) {
        return protocolExec;
    }

    /**
     * For internal use.
     */
    protected void addCloseable(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        if (closeables == null) {
            closeables = new ArrayList<Closeable>();
        }
        closeables.add(closeable);
    }

    private static String[] split(final String s) {
        if (TextUtils.isBlank(s)) {
            return null;
        }
        return s.split(" *, *");
    }

    public CloseableHttpClient build() {
        // Create main request executor
        HttpRequestExecutor requestExec = this.requestExec;
        if (requestExec == null) {
            requestExec = new HttpRequestExecutor();
        }
        HttpClientConnectionManager connManager = this.connManager;
        if (connManager == null) {
            LayeredConnectionSocketFactory sslSocketFactory = this.sslSocketFactory;
            if (sslSocketFactory == null) {
                final String[] supportedProtocols = systemProperties ? split(
                        System.getProperty("https.protocols")) : null;
                final String[] supportedCipherSuites = systemProperties ? split(
                        System.getProperty("https.cipherSuites")) : null;
                X509HostnameVerifier hostnameVerifier = this.hostnameVerifier;
                if (hostnameVerifier == null) {
                    hostnameVerifier = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
                }
                if (sslcontext != null) {
                    sslSocketFactory = new SSLConnectionSocketFactory(
                            sslcontext, supportedProtocols, supportedCipherSuites, hostnameVerifier);
                } else {
                    if (systemProperties) {
                        sslSocketFactory = new SSLConnectionSocketFactory(
                                (SSLSocketFactory) SSLSocketFactory.getDefault(),
                                supportedProtocols, supportedCipherSuites, hostnameVerifier);
                    } else {
                        sslSocketFactory = new SSLConnectionSocketFactory(
                                SSLContexts.createDefault(),
                                hostnameVerifier);
                    }
                }
            }
            @SuppressWarnings("resource")
            final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslSocketFactory)
                        .build());
            if (defaultSocketConfig != null) {
                poolingmgr.setDefaultSocketConfig(defaultSocketConfig);
            }
            if (defaultConnectionConfig != null) {
                poolingmgr.setDefaultConnectionConfig(defaultConnectionConfig);
            }
            if (systemProperties) {
                String s = System.getProperty("http.keepAlive", "true");
                if ("true".equalsIgnoreCase(s)) {
                    s = System.getProperty("http.maxConnections", "5");
                    final int max = Integer.parseInt(s);
                    poolingmgr.setDefaultMaxPerRoute(max);
                    poolingmgr.setMaxTotal(2 * max);
                }
            }
            if (maxConnTotal > 0) {
                poolingmgr.setMaxTotal(maxConnTotal);
            }
            if (maxConnPerRoute > 0) {
                poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
            }
            connManager = poolingmgr;
        }
        ConnectionReuseStrategy reuseStrategy = this.reuseStrategy;
        if (reuseStrategy == null) {
            if (systemProperties) {
                final String s = System.getProperty("http.keepAlive", "true");
                if ("true".equalsIgnoreCase(s)) {
                    reuseStrategy = DefaultConnectionReuseStrategy.INSTANCE;
                } else {
                    reuseStrategy = NoConnectionReuseStrategy.INSTANCE;
                }
            } else {
                reuseStrategy = DefaultConnectionReuseStrategy.INSTANCE;
            }
        }
        ConnectionKeepAliveStrategy keepAliveStrategy = this.keepAliveStrategy;
        if (keepAliveStrategy == null) {
            keepAliveStrategy = DefaultConnectionKeepAliveStrategy.INSTANCE;
        }
        AuthenticationStrategy targetAuthStrategy = this.targetAuthStrategy;
        if (targetAuthStrategy == null) {
            targetAuthStrategy = TargetAuthenticationStrategy.INSTANCE;
        }
        AuthenticationStrategy proxyAuthStrategy = this.proxyAuthStrategy;
        if (proxyAuthStrategy == null) {
            proxyAuthStrategy = ProxyAuthenticationStrategy.INSTANCE;
        }
        UserTokenHandler userTokenHandler = this.userTokenHandler;
        if (userTokenHandler == null) {
            if (!connectionStateDisabled) {
                userTokenHandler = DefaultUserTokenHandler.INSTANCE;
            } else {
                userTokenHandler = NoopUserTokenHandler.INSTANCE;
            }
        }
        ClientExecChain execChain = new MainClientExec(
                requestExec,
                connManager,
                reuseStrategy,
                keepAliveStrategy,
                targetAuthStrategy,
                proxyAuthStrategy,
                userTokenHandler);

        execChain = decorateMainExec(execChain);

        HttpProcessor httpprocessor = this.httpprocessor;
        if (httpprocessor == null) {

            String userAgent = this.userAgent;
            if (userAgent == null) {
                if (systemProperties) {
                    userAgent = System.getProperty("http.agent");
                }
                if (userAgent == null) {
                    userAgent = DEFAULT_USER_AGENT;
                }
            }

            final HttpProcessorBuilder b = HttpProcessorBuilder.create();
            if (requestFirst != null) {
                for (final HttpRequestInterceptor i: requestFirst) {
                    b.addFirst(i);
                }
            }
            if (responseFirst != null) {
                for (final HttpResponseInterceptor i: responseFirst) {
                    b.addFirst(i);
                }
            }
            b.addAll(
                    new RequestDefaultHeaders(defaultHeaders),
                    new RequestContent(),
                    new RequestTargetHost(),
                    new RequestClientConnControl(),
                    new RequestUserAgent(userAgent),
                    new com.epam.reportportal.apache.http.client.protocol.RequestExpectContinue());
            if (!cookieManagementDisabled) {
                b.add(new RequestAddCookies());
            }
            if (!contentCompressionDisabled) {
                b.add(new RequestAcceptEncoding());
            }
            if (!authCachingDisabled) {
                b.add(new RequestAuthCache());
            }
            if (!cookieManagementDisabled) {
                b.add(new ResponseProcessCookies());
            }
            if (!contentCompressionDisabled) {
                b.add(new ResponseContentEncoding());
            }
            if (requestLast != null) {
                for (final HttpRequestInterceptor i: requestLast) {
                    b.addLast(i);
                }
            }
            if (responseLast != null) {
                for (final HttpResponseInterceptor i: responseLast) {
                    b.addLast(i);
                }
            }
            httpprocessor = b.build();
        }
        execChain = new ProtocolExec(execChain, httpprocessor);

        execChain = decorateProtocolExec(execChain);

        // Add request retry executor, if not disabled
        if (!automaticRetriesDisabled) {
            HttpRequestRetryHandler retryHandler = this.retryHandler;
            if (retryHandler == null) {
                retryHandler = DefaultHttpRequestRetryHandler.INSTANCE;
            }
            execChain = new RetryExec(execChain, retryHandler);
        }

        HttpRoutePlanner routePlanner = this.routePlanner;
        if (routePlanner == null) {
            SchemePortResolver schemePortResolver = this.schemePortResolver;
            if (schemePortResolver == null) {
                schemePortResolver = DefaultSchemePortResolver.INSTANCE;
            }
            if (proxy != null) {
                routePlanner = new DefaultProxyRoutePlanner(proxy, schemePortResolver);
            } else if (systemProperties) {
                routePlanner = new SystemDefaultRoutePlanner(
                        schemePortResolver, ProxySelector.getDefault());
            } else {
                routePlanner = new DefaultRoutePlanner(schemePortResolver);
            }
        }
        // Add redirect executor, if not disabled
        if (!redirectHandlingDisabled) {
            RedirectStrategy redirectStrategy = this.redirectStrategy;
            if (redirectStrategy == null) {
                redirectStrategy = DefaultRedirectStrategy.INSTANCE;
            }
            execChain = new RedirectExec(execChain, routePlanner, redirectStrategy);
        }

        // Optionally, add service unavailable retry executor
        final ServiceUnavailableRetryStrategy serviceUnavailStrategy = this.serviceUnavailStrategy;
        if (serviceUnavailStrategy != null) {
            execChain = new ServiceUnavailableRetryExec(execChain, serviceUnavailStrategy);
        }
        // Optionally, add connection back-off executor
        final BackoffManager backoffManager = this.backoffManager;
        final ConnectionBackoffStrategy connectionBackoffStrategy = this.connectionBackoffStrategy;
        if (backoffManager != null && connectionBackoffStrategy != null) {
            execChain = new BackoffStrategyExec(execChain, connectionBackoffStrategy, backoffManager);
        }

        Lookup<AuthSchemeProvider> authSchemeRegistry = this.authSchemeRegistry;
        if (authSchemeRegistry == null) {
            authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
                .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
                .build();
        }
        Lookup<CookieSpecProvider> cookieSpecRegistry = this.cookieSpecRegistry;
        if (cookieSpecRegistry == null) {
            cookieSpecRegistry = RegistryBuilder.<CookieSpecProvider>create()
                .register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())
                .register(CookieSpecs.STANDARD, new RFC2965SpecFactory())
                .register(CookieSpecs.BROWSER_COMPATIBILITY, new BrowserCompatSpecFactory())
                .register(CookieSpecs.NETSCAPE, new NetscapeDraftSpecFactory())
                .register(CookieSpecs.IGNORE_COOKIES, new IgnoreSpecFactory())
                .register("rfc2109", new RFC2109SpecFactory())
                .register("rfc2965", new RFC2965SpecFactory())
                .build();
        }

        CookieStore defaultCookieStore = this.cookieStore;
        if (defaultCookieStore == null) {
            defaultCookieStore = new BasicCookieStore();
        }

        CredentialsProvider defaultCredentialsProvider = this.credentialsProvider;
        if (defaultCredentialsProvider == null) {
            if (systemProperties) {
                defaultCredentialsProvider = new SystemDefaultCredentialsProvider();
            } else {
                defaultCredentialsProvider = new BasicCredentialsProvider();
            }
        }

        return new InternalHttpClient(
                execChain,
                connManager,
                routePlanner,
                cookieSpecRegistry,
                authSchemeRegistry,
                defaultCookieStore,
                defaultCredentialsProvider,
                defaultRequestConfig != null ? defaultRequestConfig : RequestConfig.DEFAULT,
                closeables != null ? new ArrayList<Closeable>(closeables) : null);
    }

}
