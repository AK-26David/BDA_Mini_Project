package com.suchit.utils;

import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientFactory {
	private volatile static HttpClientFactory uniqueInstance = getInstance();
	static Logger log = LoggerFactory.getLogger(com.suchit.utils.HttpClientFactory.class);
	private static final int DEFAULT_CONNECTION_TIMEOUT = 5* 60 * 1000; // 1
	private static final int DEFAULT_SOCKET_TIMEOUT = 5 * 60 * 1000; // 5
	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 300;
	private static final int DEFAULT_MAX_TOTAL_PER_ROUTE = 100;
	private static PoolingHttpClientConnectionManager poolingConnectionManager;

	public static HttpClientFactory getInstance() {
		if (uniqueInstance == null) {
			synchronized (HttpClientFactory.class) {
				if (uniqueInstance == null) {
					uniqueInstance = new HttpClientFactory();
				}
			}
		}
		return uniqueInstance;
	}

	private HttpClientFactory() {
		// Use custom message parser / writer to customize the way HTTP
		// messages are parsed from and written out to the data stream.
		HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {
			@Override
			public HttpMessageParser<HttpResponse> create(SessionInputBuffer buffer, MessageConstraints constraints) {
				LineParser lineParser = new BasicLineParser() {
					@Override
					public Header parseHeader(final CharArrayBuffer buffer) {
						try {
							return super.parseHeader(buffer);
						} catch (ParseException ex) {
							return new BasicHeader(buffer.toString(), null);
						}
					}
				};
				return new DefaultHttpResponseParser(buffer, lineParser, DefaultHttpResponseFactory.INSTANCE,
						constraints) {
					@Override
					protected boolean reject(final CharArrayBuffer line, int count) {
						// try to ignore all garbage preceding a status line
						// infinitely
						return false;
					}
				};
			}
		};
		HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();
		HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(requestWriterFactory, responseParserFactory);
		SSLContext sslContext = SSLContexts.createSystemDefault();
   	 	SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
   		        new String[]{"TLSv1","TLSv1.1","TLSv1.2"},null,SSLConnectionSocketFactory.getDefaultHostnameVerifier());
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", sslsf).build();
		// Use custom DNS resolver to override the system DNS resolution.
		DnsResolver dnsResolver = new SystemDefaultDnsResolver();
		poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory,dnsResolver);
		SocketConfig socketConfig = SocketConfig.custom()
									.setTcpNoDelay(true)
									.setSoTimeout(DEFAULT_SOCKET_TIMEOUT)
									.setSoKeepAlive(true)
									.build();
		poolingConnectionManager.setDefaultSocketConfig(socketConfig);
		// Create message constraints
        MessageConstraints messageConstraints = MessageConstraints.custom()
        										.setMaxHeaderCount(200)
        										.setMaxLineLength(20000)
        										.build();
        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
        									.setMalformedInputAction(CodingErrorAction.IGNORE)
        									.setUnmappableInputAction(CodingErrorAction.IGNORE)
        									.setCharset(Consts.UTF_8)
        									.setMessageConstraints(messageConstraints)
        									.build();
        poolingConnectionManager.setDefaultConnectionConfig(connectionConfig);
		poolingConnectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
		poolingConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_TOTAL_PER_ROUTE);
		poolingConnectionManager.closeExpiredConnections();
		poolingConnectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
		poolingConnectionManager.setValidateAfterInactivity(5000);
	}

	static /* Creates a new HttpClient object for the given host. */
	// TODO , Need to set a different retry handler (Standard handler with
	// retryRequest set to true for GET) and false for POST methods
	  CookieStore cookieStore = new BasicCookieStore();
	  
	private static CloseableHttpClient createHttpClient() {
		RequestConfig requestConfig = RequestConfig.custom()
//				.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
//				.setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT)
//				.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT)
				.setCookieSpec(CookieSpecs.DEFAULT)
				.setExpectContinueEnabled(true)
				.setCircularRedirectsAllowed(true)
				.build();
		CloseableHttpClient httpClient = HttpClients
				.custom()
			    .setDefaultCookieStore(cookieStore)
				.addInterceptorFirst(requestInterceptor)
				.addInterceptorFirst(gzipIntercept)
				.setUserAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) like Gecko")
				.setConnectionManager(poolingConnectionManager)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(3, false))
				.setDefaultRequestConfig(requestConfig).build();
		return httpClient;
	}

	private final static HttpRequestInterceptor requestInterceptor = new HttpRequestInterceptor() {
		@Override
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
			if (!request.containsHeader("Accept")) {
				request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			}
		}
	};
	private final static HttpResponseInterceptor gzipIntercept = new HttpResponseInterceptor() {
		@Override
		public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Header ceheader = entity.getContentType();
				if (ceheader != null) {
					HeaderElement[] codecs = ceheader.getElements();
					for (int i = 0; i < codecs.length; i++) {
						if (codecs[i].getName().equalsIgnoreCase("gzip")) {
							response.setEntity(new GzipDecompressingEntity(response.getEntity()));
							return;
						}
						if (codecs[i].getName().equalsIgnoreCase("application/zip")) {
							response.setEntity(new ZipDecompressingEntity(response.getEntity()));
							return;
						}
					}
				}
			}
		}
	};

	/* Returns an HttpClient object associated with the host. */
	public static CloseableHttpClient getHttpClient(String host) {
		return createHttpClient();
	}
	
	public static CloseableHttpClient getHttpClient() {
		return createHttpClient();
	}

}
