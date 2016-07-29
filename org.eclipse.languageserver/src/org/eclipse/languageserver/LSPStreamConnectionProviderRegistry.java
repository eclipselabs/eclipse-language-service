/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.languageserver.languages.TweakedVSCodeCSSConnectionProvider;
import org.eclipse.languageserver.languages.TweakedVSCodeJsonConnectionProvider;
import org.eclipse.languageserver.languages.csharp.OmnisharpConnectionProvider;

/**
 * This registry aims at providing a good language server connection (as {@link StreamConnectionProvider}
 * for a given input.
 * At the moment, registry content are hardcoded but we'll very soon need a way
 * to contribute to it via plugin.xml (for plugin developers) and from Preferences
 * (for end-users to directly register a new server).
 *
 */
public class LSPStreamConnectionProviderRegistry {
	
	private static LSPStreamConnectionProviderRegistry INSTANCE = null;
	public static LSPStreamConnectionProviderRegistry getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LSPStreamConnectionProviderRegistry();
		}
		return INSTANCE;
	}
	
	
	Map<IContentType, StreamConnectionProvider> connections = new HashMap<>();
	
	private LSPStreamConnectionProviderRegistry() {
		initialize();
	}
	
	private void initialize() {
		IContentTypeManager manager = Platform.getContentTypeManager();
		// TODO initialize from extension registry and/or preference or other settigns
		connections.put(manager.getContentType("org.eclipse.wst.css.core.csssource"), new TweakedVSCodeCSSConnectionProvider());
		connections.put(manager.getContentType("org.eclipse.wst.jsdt.core.jsonSource"), new TweakedVSCodeJsonConnectionProvider());
		connections.put(manager.getContentType("org.eclipse.languageserver.csharp"), new OmnisharpConnectionProvider());
	}
	
	public StreamConnectionProvider findProviderFor(IContentType contentType) {
		return this.connections.get(contentType);
	}

}
