package com.suchit.utils;

/* ====================================================================Licensed to the Apache Software Foundation (ASF) under oneor more contributor license
 * agreements. See the NOTICE filedistributed with this work for additional informationregarding copyright ownership. The ASF licenses this fileto you under the
 * Apache License, Version 2.0 (the"License"); you may not use this file except in compliancewith the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing,software distributed under the License is distributed on
 * an"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANYKIND, either express or implied. See the License for thespecific language governing permissions and
 * limitationsunder the License.====================================================================This software consists of voluntary contributions made by
 * manyindividuals on behalf of the Apache Software Foundation. For moreinformation on the Apache Software Foundation, please see<http://www.apache.org/>. */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.util.Args;

/**
 * Common base class for decompressing {@link HttpEntity} implementations.
 * 
 * @since 4.1
 */
abstract class DecompressingEntity extends HttpEntityWrapper {
	/**
	 * Default buffer size.
	 */
	private static final int BUFFER_SIZE = 1024 * 2;
	/**
	 * {@link #getContent()} method must return the same {@link InputStream}
	 * instance when DecompressingEntity is wrapping a streaming entity.
	 */
	private InputStream content;

	/**
	 * Creates a new {@link DecompressingEntity}.
	 * 
	 * @param wrapped
	 *            the non-null {@link HttpEntity} to be wrapped
	 */
	public DecompressingEntity(final HttpEntity wrapped) {
		super(wrapped);
	}

	abstract InputStream decorate(final InputStream wrapped) throws IOException;

	private InputStream getDecompressingStream() throws IOException {
		final InputStream in = wrappedEntity.getContent();
		try {
			return decorate(in);
		} catch (final IOException ex) {
			in.close();
			throw ex;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getContent() throws IOException {
		if (wrappedEntity.isStreaming()) {
			if (content == null) {
				content = getDecompressingStream();
			}
			return content;
		} else {
			return getDecompressingStream();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeTo(final OutputStream outstream) throws IOException {
		Args.notNull(outstream, "Output stream");
		final InputStream instream = getContent();
		try {
			final byte[] buffer = new byte[BUFFER_SIZE];
			int l;
			while ((l = instream.read(buffer)) != -1) {
				outstream.write(buffer, 0, l);
			}
		} finally {
			instream.close();
		}
	}
}
