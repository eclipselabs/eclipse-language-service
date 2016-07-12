package org.eclipse.languageserver;

import java.io.File;
import java.io.IOException;

import io.typefox.lsapi.services.json.JsonBasedLanguageServer;

public class LanaguageServiceAccessor {

	private static JsonBasedLanguageServer server;

	public static JsonBasedLanguageServer getLanaguageServer() throws IOException {
		if (server == null) {
			// Steps to get a usable Language server (for language supported by VSCode LS implementations)
			// 1. Build VSCode as explained in https://github.com/Microsoft/vscode/wiki/How-to-Contribute#build-and-run-from-source
			//     (don't forget the `npm run watch` after the install script)
			// 2. Then set language server location and adapt process builder.
			
			ProcessBuilder builder = new ProcessBuilder("/usr/bin/node", "/home/mistria/git/vscode/extensions/css/server/out/cssServerMain.js")
				.directory(new File("/home/mistria/git/vscode/extensions/css/server/out/"));
			Process p = builder.start();
			server = new JsonBasedLanguageServer();
			server.connect(p.getInputStream(), p.getOutputStream());
		}
		return server;
	}
}
