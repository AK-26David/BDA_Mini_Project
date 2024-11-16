/**
 * 
 */
package com.suchit.utils;

import java.net.UnknownHostException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author IBM_ADMIN
 */
public class PostURL {
	static Logger log = LoggerFactory.getLogger(com.suchit.utils.PostURL.class);
	private CloseableHttpClient httpClient;
	private final HttpPost httpPost;
	private final HttpClientContext httpContext;
	private final String URI;
	private int statusCode;
	Header[] responseHeaders;
	private String redirectedURI;

	public PostURL(CloseableHttpClient httpClient, String uri) {
		this.httpClient = httpClient;
		this.URI = uri;
		this.httpContext = HttpClientContext.create();
		httpPost = new HttpPost(URI);
	}

	public PostURL(CloseableHttpClient httpClient, String uri, HttpClientContext clientContext) {
		this.httpClient = httpClient;
		this.URI = uri;
		this.httpContext = clientContext;
		httpPost = new HttpPost(URI);
	}

	public byte[] executePost() {
		byte[] responseData = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost, httpContext);
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
				log.error("Unknown host exception raised for URI {} , will init httpClient and Retry", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException || e instanceof HttpHostConnectException
					|| e instanceof MalformedChunkCodingException) {
				log.error("Request Sent for URI {} but no response received , Retrying", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSL Handshake error occured for URI {}, reiniting the client and retrying ", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}

	public byte[] postURLDataWithRedirection() {
		byte[] responseData = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost, httpContext);
			try {
				setStatusCode(response.getStatusLine().getStatusCode());
				responseHeaders = response.getAllHeaders();
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					responseData = EntityUtils.toByteArray(entity);
					EntityUtils.consume(entity);
				}
				HttpHost target = httpContext.getTargetHost();
				List<java.net.URI> redirectLocations = httpContext.getRedirectLocations();
				redirectedURI = URIUtils.resolve(httpPost.getURI(), target, redirectLocations).toString();
			} finally {
				response.close();
			}
			if (responseData == null) {
				log.debug("Request Sent but no response received");
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			}
		} catch (Exception e) {
			if (e instanceof UnknownHostException) {
				log.error("Unknown host exception raised for URI {} , will init httpClient and Retry", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof NoHttpResponseException || e instanceof HttpHostConnectException
					|| e instanceof MalformedChunkCodingException) {
				log.error("Request Sent for URI {} but no response received , Retrying", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else if (e instanceof javax.net.ssl.SSLHandshakeException) {
				log.error("SSL Handshake error occured for URI {}, reiniting the client and retrying ", URI);
				setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
			} else {
				log.error("{}", e);
			}
		}
		return responseData;
	}

	/**
	 * @param formEntity
	 */
	public void setEntity(UrlEncodedFormEntity formEntity) {
		httpPost.setEntity(formEntity);
	}
	public void setEntity(StringEntity stringEntity) {
		httpPost.setEntity(stringEntity);
	}
	

	public void setReferer(String referrer) {
		httpPost.setHeader("Referer", referrer);
	}

	public void addHeader(String name, String value) {
		this.httpPost.addHeader(name, value);
	}

	public String getRedirectedURI() {
		return redirectedURI;
	}

	public String getResponseHeaderValue(String name) {
		for (Header header : responseHeaders) {
			if (header.getName().equals(name)) {
				return header.getValue();
			}
		}
		return "";
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getURI() {
		return URI;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public HttpPost getHttpPost() {
		return httpPost;
	}

	public HttpClientContext getHttpContext() {
		return httpContext;
	}
}
