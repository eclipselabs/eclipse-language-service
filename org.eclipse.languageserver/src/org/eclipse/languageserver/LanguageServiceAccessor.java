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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

/**
 * The entry-point to retrieve a Language Server for a given resource/project.
 * Deals with instantiations and caching of underlying {@link ProjectSpecificLanguageServerWrapper}.
 *
 */
public class LanguageServiceAccessor {

	private static Map<IProject, Map<IContentType, ProjectSpecificLanguageServerWrapper>> projectServers = new HashMap<>();

	public static LanguageClientEndpoint getLanguageServer(IFile file, IDocument document, Predicate<ServerCapabilities> request) throws IOException {
		ProjectSpecificLanguageServerWrapper wrapper = getLSWrapper(file);
		if (wrapper != null && (request == null
				|| wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
				|| request.test(wrapper.getServerCapabilities())
			)) {
			wrapper.connect(file, document);
			return wrapper.getServer();
		}			
		return null;
	}
	
	@Deprecated
	public static LanguageClientEndpoint getLanguageServer(IFile file, IDocument document) throws IOException {
		return getLanguageServer(file, document, null);
	}

	private static ProjectSpecificLanguageServerWrapper getLSWrapper(IFile file) throws IOException {
		IProject project = file.getProject();
		if (!projectServers.containsKey(project)) {
			projectServers.put(project, new HashMap<>());
		}
		IContentType[] contentTypes = null;
		try (InputStream contents = file.getContents()) {
			contentTypes = Platform.getContentTypeManager().findContentTypesFor(contents, file.getName()); //TODO consider using document as inputstream
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ProjectSpecificLanguageServerWrapper wrapper = null;
		// 1st: search existing server for that file
		for (IContentType contentType : contentTypes) {
			if (wrapper == null) {
				wrapper = projectServers.get(project).get(contentType);
			}
		}
		if (wrapper == null) {
			// try to create one for available content type
			for (IContentType contentType : contentTypes) {
				if (wrapper == null) {
					StreamConnectionProvider connection = LSPStreamConnectionProviderRegistry.getInstance().findProviderFor(contentType);
					if (connection != null) {
						wrapper = new ProjectSpecificLanguageServerWrapper(project, connection);
						projectServers.get(project).put(contentType, wrapper);
					}
				}
			}
		}
		return wrapper;
	}

}
