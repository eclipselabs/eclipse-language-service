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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This registry aims at providing a good language server connection (as {@link StreamConnectionProvider}
 * for a given input.
 * At the moment, registry content are hardcoded but we'll very soon need a way
 * to contribute to it via plugin.xml (for plugin developers) and from Preferences
 * (for end-users to directly register a new server).
 *
 */
public class LSPStreamConnectionProviderRegistry {
	
	private static final String CONTENT_TYPE_TO_LSP_LAUNCH_PREF_KEY = "contentTypeToLSPLauch";
	
	private static LSPStreamConnectionProviderRegistry INSTANCE = null;
	public static LSPStreamConnectionProviderRegistry getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LSPStreamConnectionProviderRegistry();
		}
		return INSTANCE;
	}
	
	private List<Entry<IContentType, ILaunchConfiguration>> connections = new ArrayList<>();
	private IPreferenceStore preferenceStore;
	
	private LSPStreamConnectionProviderRegistry() {
		this.preferenceStore = LanguageServerPluginActivator.getDefault().getPreferenceStore();
		initialize();
	}
	
	private void initialize() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		String prefs = preferenceStore.getString(CONTENT_TYPE_TO_LSP_LAUNCH_PREF_KEY);
		if (prefs != null && !prefs.isEmpty()) {
			String[] entries = prefs.split(",");
			for (String entry : entries) {
				String[] parts = entry.split(":");
				String contentTypeId = parts[0];
				String[] launchParts = parts[1].split("/");
				String launchType = launchParts[0];
				String launchName = launchParts[1];
				IContentType contentType = contentTypeManager.getContentType(contentTypeId);
				if (contentType != null) {
					ILaunchConfiguration config = LaunchConfigurationStreamProvider.findLaunchConfiguration(launchType, launchName);
					if (config != null) {
						connections.add(new SimpleEntry<>(contentType, config));
					}
				}
			}
		}
	}
	
	private void persist() {
		StringBuilder builder = new StringBuilder();
		for (Entry<IContentType, ILaunchConfiguration> entry : connections) {
			builder.append(entry.getKey().getId());
			builder.append(':');
			try {
				builder.append(entry.getValue().getType().getIdentifier());
			} catch (CoreException e) {
				e.printStackTrace();
			}
			builder.append('/');
			builder.append(entry.getValue().getName());
			builder.append(',');
		}
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}
		this.preferenceStore.setValue(CONTENT_TYPE_TO_LSP_LAUNCH_PREF_KEY, builder.toString());
		if (this.preferenceStore instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) this.preferenceStore).save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<StreamConnectionProvider> findProviderFor(final IContentType contentType) {
		return Arrays.asList(connections
			.stream()
			.filter(entry -> { return entry.getKey().equals(contentType); })
			.map(entry -> { return new LaunchConfigurationStreamProvider(entry.getValue()); })
			.toArray(StreamConnectionProvider[]::new));
	}
	
	public void registerAssociation(IContentType contentType, ILaunchConfiguration launchConfig) {
		connections.add(new SimpleEntry<>(contentType, launchConfig));
		persist();
	}

	public List<Entry<IContentType, ILaunchConfiguration>> getContentTypeToLSPLaunches() {
		return Collections.unmodifiableList(this.connections);
	}

	public void setAssociations(List<Entry<IContentType, ILaunchConfiguration>> wc) {
		this.connections.clear();
		this.connections.addAll(wc);
		persist();
	}

}
