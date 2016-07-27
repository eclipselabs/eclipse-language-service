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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LanaguageServiceAccessor {

	private static Map<IProject, Map<IContentType, LanguageServerWrapper>> projectServers = new HashMap<>();

	public static JsonBasedLanguageServer getLanaguageServer(IFile file, IDocument document) throws IOException {
		IProject project = file.getProject();
		if (!projectServers.containsKey(project)) {
			projectServers.put(project, new HashMap<>());
		}
		IContentType contentType = null;
		try (InputStream contents = file.getContents()) {
			contentType = Platform.getContentTypeManager().findContentTypeFor(contents, file.getName()); //TODO consider using document as inputstream
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (contentType != null) {
			if (!projectServers.get(project).containsKey(contentType)) {
				LanguageServerWrapper wrapper = new LanguageServerWrapper(project, contentType);
				projectServers.get(project).put(contentType, wrapper);
			}
			LanguageServerWrapper wrapper = projectServers.get(project).get(contentType);
			wrapper.connect(file, document);
			return wrapper.getServer();
		}
		return null;
	}
}
