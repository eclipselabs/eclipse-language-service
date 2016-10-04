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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import io.typefox.lsapi.ServerCapabilities;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

/**
 * The entry-point to retrieve a Language Server for a given resource/project.
 * Deals with instantiations and caching of underlying {@link ProjectSpecificLanguageServerWrapper}.
 *
 */
public class LanguageServiceAccessor {

	static class WrapperEntryKey {
		final IProject project;
		final IContentType contentType;

		public WrapperEntryKey(IProject project, IContentType contentType) {
			this.project = project;
			this.contentType = contentType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
			result = prime * result + ((project == null) ? 0 : project.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WrapperEntryKey other = (WrapperEntryKey) obj;
			if (contentType == null) {
				if (other.contentType != null)
					return false;
			} else if (!contentType.equals(other.contentType))
				return false;
			if (project == null) {
				if (other.project != null)
					return false;
			} else if (!project.equals(other.project))
				return false;
			return true;
		}


	}

	private static Map<WrapperEntryKey, ProjectSpecificLanguageServerWrapper> projectServers = new HashMap<>();

	public static class LSPDocumentInfo {

		public final URI fileUri;
		public final IFile file;
		public final IDocument document;
		public final LanguageClientEndpoint languageClient;

		public LSPDocumentInfo(URI fileUri, IFile file, IDocument document, LanguageClientEndpoint languageClient) {
			this.fileUri = fileUri;
			this.file = file;
			this.document = document;
			this.languageClient = languageClient;
		}
	}

	public static LSPDocumentInfo getLSPDocumentInfoFor(ITextViewer viewer, Predicate<ServerCapabilities> capability) {
		IDocument document = viewer.getDocument();
		final IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(document).getLocation();
		final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
		URI fileUri = null;
		LanguageClientEndpoint languageClient = null;
		if (file.exists()) { // TODO, also support non resource file
			fileUri = file.getLocation().toFile().toURI();
			try {
				languageClient = getLanguageServer(file, document, capability);
			} catch (final IOException e) {
				// TODO report?
				e.printStackTrace();
			}
		} else {
			fileUri = location.toFile().toURI();
		}
		return new LSPDocumentInfo(fileUri, file, document, languageClient);
	}

	public static LSPDocumentInfo getLSPDocumentInfoFor(ITextEditor editor, Predicate<ServerCapabilities> capability) {
		IEditorInput input = editor.getEditorInput();
		LanguageClientEndpoint languageClient = null;
		URI fileUri = null;
		IFile file = null;
		IDocument document = null;
		try {
			if (input instanceof IFileEditorInput) {
				// TODO, also support non resource file
				file = ((IFileEditorInput) input).getFile();
				fileUri = file.getLocation().toFile().toURI();
				document = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).getDocument();
				languageClient = LanguageServiceAccessor.getLanguageServer(file, document, ServerCapabilities::isReferencesProvider);
			} else if (input instanceof IURIEditorInput) {
				fileUri = ((IURIEditorInput) input).getURI();
				document = FileBuffers.getTextFileBufferManager().getTextFileBuffer(new Path(fileUri.getPath()), LocationKind.LOCATION).getDocument();
				// TODO server
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new LSPDocumentInfo(fileUri, file, document, languageClient);
	}

	public static LanguageClientEndpoint getLanguageServer(IFile file, IDocument document,
			Predicate<ServerCapabilities> request) throws IOException {
		ProjectSpecificLanguageServerWrapper wrapper = getLSWrapper(file, request);
		if (wrapper != null) {
			wrapper.connect(file, document);
			return wrapper.getServer();
		}
		return null;
	}

	private static ProjectSpecificLanguageServerWrapper getLSWrapper(IFile file, Predicate<ServerCapabilities> request) throws IOException {
		IProject project = file.getProject();
		IContentType[] fileContentTypes = null;
		try (InputStream contents = file.getContents()) {
			fileContentTypes = Platform.getContentTypeManager().findContentTypesFor(contents, file.getName()); //TODO consider using document as inputstream
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ProjectSpecificLanguageServerWrapper wrapper = null;

		// 1st: search existing server for that file
		for (IContentType contentType : fileContentTypes) {
			WrapperEntryKey key = new WrapperEntryKey(project, contentType);
			wrapper = projectServers.get(key);
			if (wrapper != null && (request == null
					|| wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
					|| request.test(wrapper.getServerCapabilities())
				)) {
				break;
			} else {
				wrapper = null;
			}
		}

		if (wrapper == null) {
			// try to create one for available content type
			for (IContentType contentType : fileContentTypes) {
				for (StreamConnectionProvider connection : LSPStreamConnectionProviderRegistry.getInstance().findProviderFor(contentType)) {
					wrapper = new ProjectSpecificLanguageServerWrapper(project, connection);
					WrapperEntryKey key = new WrapperEntryKey(project, contentType);
					projectServers.put(key, wrapper);
					if (request == null
						|| wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
						|| request.test(wrapper.getServerCapabilities())) {
						return wrapper;
					}
				}
			}
		}
		return wrapper;
	}

}
