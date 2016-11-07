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
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.texteditor.AbstractTextEditor;
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

	/**
	 * A bean storing association of a Document/File with a language server.
	 * See {@link LanguageServiceAccessor#getLSPDocumentInfoFor(ITextViewer, Predicate)} 
	 */
	public static class LSPDocumentInfo {

		private final @NonNull URI fileUri;
		private final @NonNull IDocument document;
		private final @Nullable ServerCapabilities capabilities;
		private final @NonNull LanguageClientEndpoint languageClient;

		private LSPDocumentInfo(@NonNull URI fileUri, @NonNull IDocument document, @NonNull LanguageClientEndpoint languageClient, @Nullable ServerCapabilities capabilities) {
			this.fileUri = fileUri;
			this.document = document;
			this.languageClient = languageClient;
			this.capabilities = capabilities;
		}

		public @NonNull IDocument getDocument() {
			return this.document;
		}

		public @NonNull URI getFileUri() {
			return this.fileUri;
		}

		public @NonNull LanguageClientEndpoint getLanguageClient() {
			return this.languageClient;
		}

		public @Nullable ServerCapabilities getCapabilites() {
			return this.capabilities;
		}
	}

	/**
	 * A bean storing association of a IProject with a language server. 
	 */
	public static class LSPServerInfo {

		private final @NonNull IProject project;
		private final @Nullable ServerCapabilities capabilities;
		private final @NonNull LanguageClientEndpoint languageClient;

		private LSPServerInfo(@NonNull IProject project, @NonNull LanguageClientEndpoint languageClient,
		        @Nullable ServerCapabilities capabilities) {
			this.project = project;
			this.languageClient = languageClient;
			this.capabilities = capabilities;
		}

		public @NonNull IProject getProject() {
			return project;
		}

		public @NonNull LanguageClientEndpoint getLanguageClient() {
			return this.languageClient;
		}

		public @Nullable ServerCapabilities getCapabilites() {
			return this.capabilities;
		}
	}

	@Nullable public static LSPDocumentInfo getLSPDocumentInfoFor(ITextViewer viewer, Predicate<ServerCapabilities> capabilityRequest) {
		IDocument document = viewer.getDocument();
		final IPath location = FileBuffers.getTextFileBufferManager().getTextFileBuffer(document).getLocation();
		final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
		URI fileUri = null;
		LanguageClientEndpoint languageClient = null;
		ServerCapabilities capabilities = null;
		if (file.exists()) { // TODO, also support non resource file
			fileUri = file.getLocation().toFile().toURI();
			try {
				ProjectSpecificLanguageServerWrapper wrapper = getLSWrapper(file, capabilityRequest);
				if (wrapper != null) {
					wrapper.connect(file, document);
					languageClient = wrapper.getServer();
					capabilities = wrapper.getServerCapabilities();
					return new LSPDocumentInfo(fileUri, document, languageClient, capabilities);
				}
			} catch (final IOException e) {
				// TODO report?
				e.printStackTrace();
			}
		} else {
			fileUri = location.toFile().toURI();
		}
		// TODO handle case of plain file (no IFile)
		return null;
	}

	public static LSPDocumentInfo getLSPDocumentInfoFor(ITextEditor editor, Predicate<ServerCapabilities> capabilityRequest) {
		// Ugly hack, but not worse than duplication
		try {
			Method getSourceViewerMethod= AbstractTextEditor.class.getDeclaredMethod("getSourceViewer"); //$NON-NLS-1$
			getSourceViewerMethod.setAccessible(true);
			ITextViewer viewer = (ITextViewer) getSourceViewerMethod.invoke(editor);
			return getLSPDocumentInfoFor(viewer, capabilityRequest);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
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

	@Nullable private static ProjectSpecificLanguageServerWrapper getLSWrapper(@NonNull IFile file, @Nullable Predicate<ServerCapabilities> request) throws IOException {
		IProject project = file.getProject();
		IContentType[] fileContentTypes = null;
		try (InputStream contents = file.getContents()) {
			fileContentTypes = Platform.getContentTypeManager().findContentTypesFor(contents, file.getName()); //TODO consider using document as inputstream
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
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

	/**
	 * Gets list of LS initialized for given project. 
	 * 
	 * @param project
	 * @param request
	 * @return list of servers info
	 */
	@NonNull public static List<LSPServerInfo> getLSPServerInfos(@NonNull IProject project,
	        Predicate<ServerCapabilities> request) {
		List<LSPServerInfo> serverInfos = new ArrayList<>();
		for (WrapperEntryKey wrapperEntryKey : projectServers.keySet()) {
			ProjectSpecificLanguageServerWrapper wrapper = projectServers.get(wrapperEntryKey);
			if (wrapperEntryKey.project.equals(project) && (request == null || wrapper
			        .getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
			        || request.test(wrapper.getServerCapabilities()))) {
				serverInfos.add(new LSPServerInfo(project, wrapper.getServer(), wrapper.getServerCapabilities()));
			}
		}
		return serverInfos;
	}

}
