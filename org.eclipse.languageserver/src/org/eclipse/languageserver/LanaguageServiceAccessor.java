package org.eclipse.languageserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;

import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LanaguageServiceAccessor {

	private static Map<IProject, LanguageServerWrapper> servers = new HashMap<>();

	public static JsonBasedLanguageServer getLanaguageServer(IFile file, IDocument document) throws IOException {
		if (!servers.containsKey(file.getProject())) {
			servers.put(file.getProject(), new LanguageServerWrapper(file.getProject()));
		}
		LanguageServerWrapper wrapper = servers.get(file.getProject());
		wrapper.connect(file, document);
		return wrapper.getServer();
	}
}
