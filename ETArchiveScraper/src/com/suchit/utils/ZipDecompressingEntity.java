package com.suchit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class ZipDecompressingEntity extends DecompressingEntity {
	public ZipDecompressingEntity(HttpEntity wrapped) {
		super(wrapped);
	}

	@Override
	InputStream decorate(InputStream wrapped) throws IOException {
		ZipInputStream zin = new ZipInputStream(wrapped);
		// Moves the stream position to the zip file
		// contents
		zin.getNextEntry();
		return zin;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Header getContentEncoding() {
		/* This HttpEntityWrapper has dealt with the Content-Encoding. */
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentLength() {
		/* length of ungzipped content is not known */
		return -1;
	}
}
