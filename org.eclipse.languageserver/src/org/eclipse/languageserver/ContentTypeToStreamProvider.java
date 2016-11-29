/*******************************************************************************
 * Copyright (c) 2016 Rogue Wave Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver;

import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.NonNull;

public class ContentTypeToStreamProvider {

	private IContentType contentType;
	private StreamConnectionProvider provider;

	public ContentTypeToStreamProvider(@NonNull IContentType contentType, StreamConnectionProvider provider) {
		this.contentType = contentType;
		this.provider = provider;
	}

	public IContentType getContentType() {
		return contentType;
	}

	public StreamConnectionProvider getStreamConnectionProvider() {
		return provider;
	}

	public void appendTo(StringBuilder builder){	
	}

}
