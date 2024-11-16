package com.suchit.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.RFC6265LaxSpec;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetThread {
	static Logger log = LoggerFactory.getLogger(com.suchit.utils.GetThread.class);
	private final CloseableHttpClient httpClient;
	private final HttpGet httpget;
	private final String URI;
	private final HttpClientContext httpContext;
	private Header[] responseHeaders;
	private String redirectedURI;
	private int statusCode;
	private List<Cookie> responseCookies ;
	CookieSpec cookieSpec = new RFC6265LaxSpec();

	public GetThread(CloseableHttpClient httpClient, String uri) {
		this.URI = uri;
		this.httpClient = httpClient;
		this.httpContext = HttpClientContext.create();
		this.httpget = new HttpGet(URI);
	}

	public GetThread(CloseableHttpClient httpClient, String uri, HttpClientContext clientContext) {
		this.URI = uri;
		this.httpClient = httpClient;
		this.httpContext = clientContext;
		this.httpget = new HttpGet(URI);
	}
	
	public GetThread(CloseableHttpClient httpClient, String uri, List<Cookie> cookieList) {
		this.URI = uri;
		this.httpClient = httpClient;
		this.httpContext = HttpClientContext.create();
		this.httpContext.setCookieStore(new BasicCookieStore());
		cookieList.forEach(cookie -> {
			this.httpContext.getCookieStore().addCookie(cookie);
		});
		this.httpget = new HttpGet(URI);
		
	}

	public byte[] executeGet() {
		byte[] responseData = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpget, httpContext);
			try {
				setStatusCode(response.getStatusLine().getStatusCode());
				responseHeaders = response.getAllHeaders();
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseData = EntityUtils.toByteArray(entity);
					EntityUtils.consume(entity);
				}
			} finally {
				response.close();
			}
			if (responseData == null) {
				log.debug("Request Sent but no response received");
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			}
		} catch (Exception e) {
			if (e instanceof UnknownHostException) {
				log.error("UnknownHostException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException) {
				log.error("NoHttpResponseException for URI {}", URI);
				setStatusCode(HttpStatus.SC_NO_CONTENT);
			} else if (e instanceof HttpHostConnectException) {
				log.error("HttpHostConnectException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof MalformedChunkCodingException) {
				log.error("MalformedChunkCodingException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSLHandshakeException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof java.net.SocketTimeoutException) {
				log.error("Socket Timeout Exception for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof ClientProtocolException) {
				log.error("HTTP Client Protocol Exception for URI {}" , URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}
	
	public String executeGetString() {
		String responseData = null;
		try(CloseableHttpResponse response = httpClient.execute(httpget, httpContext)) {
			setStatusCode(response.getStatusLine().getStatusCode());
			responseHeaders = response.getAllHeaders();
			 if (statusCode >= 200 && statusCode < 300) {
	             HttpEntity entity = response.getEntity();
	             if(entity!=null) {
	            	 responseData = EntityUtils.toString(entity);
	            	 EntityUtils.consume(entity);
	             }
	         }
		} catch (Exception e) {
			if (e instanceof UnknownHostException) {
				log.error("UnknownHostException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException || e instanceof ConnectionClosedException) {
				log.error("NoHttpResponseException or ConnectionClosed Exception for URI {}", URI);
				setStatusCode(HttpStatus.SC_NO_CONTENT);
			} else if (e instanceof HttpHostConnectException) {
				log.error("HttpHostConnectException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof MalformedChunkCodingException) {
				log.error("MalformedChunkCodingException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSLHandshakeException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof java.net.SocketTimeoutException) {
				log.error("Socket Timeout Exception for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof ClientProtocolException) {
				log.error("HTTP Client Protocol Exception for URI {}" , URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof SSLException){
				log.error("HTTP SSLException for URI {}" , URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}
	

	public byte[] executeGetWithRedirection() {
		byte[] responseData = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpget, httpContext);
			try {
				setStatusCode(response.getStatusLine().getStatusCode());
				responseHeaders = response.getAllHeaders();
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseData = EntityUtils.toByteArray(entity);
					EntityUtils.consume(entity);
				}
			} finally {
				response.close();
			}
			if (responseData == null) {
				log.debug("Request Sent but no response received");
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			}
			HttpHost target = httpContext.getTargetHost();
			List<java.net.URI> redirectLocations = httpContext.getRedirectLocations();
			this.redirectedURI = URIUtils.resolve(httpget.getURI(), target, redirectLocations).toString();
		} catch (Exception e) {
			if (e instanceof UnknownHostException) {
				log.error("UnknownHostException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException) {
				log.error("NoHttpResponseException for URI {}", URI);
				setStatusCode(HttpStatus.SC_NO_CONTENT);
			} else if (e instanceof HttpHostConnectException) {
				log.error("HttpHostConnectException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof MalformedChunkCodingException) {
				log.error("MalformedChunkCodingException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSLHandshakeException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}
	
	public String executeGetWithRedirectionString() {
		String responseData = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpget, httpContext);
			try {
				setStatusCode(response.getStatusLine().getStatusCode());
				responseHeaders = response.getAllHeaders();
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseData = EntityUtils.toString(entity);
					EntityUtils.consume(entity);
				}
			} finally {
				response.close();
			}
			if (responseData == null) {
				log.debug("Request Sent but no response received");
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			}
			HttpHost target = httpContext.getTargetHost();
			List<java.net.URI> redirectLocations = httpContext.getRedirectLocations();
			this.redirectedURI = URIUtils.resolve(httpget.getURI(), target, redirectLocations).toString();
		} catch (Exception e) {
			if (e instanceof UnknownHostException) {
				log.error("UnknownHostException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException) {
				log.error("NoHttpResponseException for URI {}", URI);
				setStatusCode(HttpStatus.SC_NO_CONTENT);
			} else if (e instanceof HttpHostConnectException) {
				log.error("HttpHostConnectException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof MalformedChunkCodingException) {
				log.error("MalformedChunkCodingException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSLHandshakeException for URI {}", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}

	public void addCookies(List<BasicClientCookie> cookieList) {
		for (BasicClientCookie basicClientCookie : cookieList) {
			httpContext.getCookieStore().addCookie(basicClientCookie);
		}
	}

	public HttpClientContext getHttpContext() {
		return httpContext;
	}

	public String getRedirectedURI() {
		return redirectedURI;
	}

	public void setReferer(String referrer) {
		httpget.setHeader("Referer", referrer);
	}

	public void setHeader(String headerName, String value) {
		httpget.setHeader(headerName, value);
	}

	public String getResponseHeaderValue(String name) {
		for (Header header : responseHeaders) {
			if (header.getName().equals(name)) {
				return header.getValue();
			}
		}
		return "";
	}

	public String getURI() {
		return URI;
	}

	public int statusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public Header[] getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Header[] responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public List<Cookie> getResponseCookies() {
		List<Cookie> cookies = new ArrayList<>();
		CookieOrigin ckOrg = cookieOriginFromUri(this.URI);
		List<Header> cookieHeaders = Arrays.asList(getResponseHeaders()).stream().filter(header -> 
		header.getName().equals("Set-Cookie")).collect(Collectors.toList());
		cookieHeaders.forEach(responseHeader -> {
			Header setCookieHeader = new BasicHeader("Set-Cookie", responseHeader.getValue());
			try {
				cookies.addAll(cookieSpec.parse(setCookieHeader, ckOrg));
			} catch (MalformedCookieException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		responseCookies = cookies;
		return responseCookies;
	}

	public void setResponseCookies(List<Cookie> responseCookies) {
		this.responseCookies = responseCookies;
	}

	private CookieOrigin cookieOriginFromUri(String uri) {
        try {
            URL parsedUrl = new URL(uri);
            int port = parsedUrl.getPort() != -1 ? parsedUrl.getPort() : 80;
            return new CookieOrigin(
                    parsedUrl.getHost(), port, parsedUrl.getPath(), "https".equals(parsedUrl.getProtocol()));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
	
	public static void main(String[] args) {
		try {
			HttpClientFactory.getInstance();
			CloseableHttpClient httpClient = HttpClientFactory.getHttpClient("www.moneycontrol.com");
			GetThread newGet = new GetThread(httpClient, "http://www.moneycontrol.com/terminal/?index=11");
			byte[] response = newGet.executeGet();
			System.err.println(new String(response));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
