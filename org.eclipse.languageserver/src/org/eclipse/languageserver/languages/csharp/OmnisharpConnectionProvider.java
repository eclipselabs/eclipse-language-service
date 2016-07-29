package org.eclipse.languageserver.languages.csharp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.languageserver.StreamConnectionProvider;

public class OmnisharpConnectionProvider implements StreamConnectionProvider {
	private Process process;

	@Override
	public void start() throws IOException {
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/node", "/home/mistria/git/omnisharp-node-client/languageserver/server.js")
				.directory(new File("/home/mistria/git/omnisharp-node-client/languageserver"));
		builder.environment().put("LD_LIBRARY_PATH", "/home/mistria/apps/OmniSharp.NET/icu54:" + builder.environment().get("$LD_LIBRARY_PATH"));
		this.process = builder.start();
	}
	

	public InputStream getInputStream() {
		return this.process.getInputStream();
	}

	public OutputStream getOutputStream() {
		return this.process.getOutputStream();
	}

	@Override
	public void stop() {
		this.process.destroy();
	}
}
