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
package org.eclipse.languageserver.operations.codeactions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.languageserver.LanguageServiceAccessor;
import org.eclipse.languageserver.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.languageserver.ui.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import io.typefox.lsapi.CodeActionContext;
import io.typefox.lsapi.CodeActionParams;
import io.typefox.lsapi.Command;
import io.typefox.lsapi.Diagnostic;
import io.typefox.lsapi.builders.CodeActionContextBuilder;
import io.typefox.lsapi.builders.CodeActionParamsBuilder;
import io.typefox.lsapi.services.transport.client.LanguageClientEndpoint;

public class LSPCodeActionMarkerResolution extends WorkbenchMarkerResolution implements IMarkerResolutionGenerator2 {

	private static final String LSP_REMEDIATION = "lspCodeActions";

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		Object att;
		try {
			att = marker.getAttribute(LSP_REMEDIATION);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
		if (att == null) {
			return null;
		}
		List<? extends Command> commands = (List<? extends Command>)att;
		List<IMarkerResolution> res = new ArrayList<>(commands.size());
		for (Command command : commands) {
			res.add(new CodeActionMarkerResolution(command));
		}
		return res.toArray(new IMarkerResolution[res.size()]);
	}

	@Override
	public String getDescription() {
		return Messages.codeActions_description;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		return Messages.codeActions_label;
	}

	@Override
	public void run(IMarker marker) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		List<? extends Command> resolutions = null;
		try {
			if (marker.getAttribute(LSP_REMEDIATION) != null) {
				resolutions = (List<? extends Command>)marker.getAttribute(LSP_REMEDIATION);
			} else if (marker.getResource().getType() == IResource.FILE) {
				IDocument document = ITextFileBufferManager.DEFAULT.getTextFileBuffer(marker.getResource().getFullPath(), LocationKind.IFILE).getDocument();
				LanguageClientEndpoint lsp = LanguageServiceAccessor.getLanguageServer((IFile)marker.getResource(), document);
				if (lsp != null) {
					Diagnostic diagnostic = (Diagnostic)marker.getAttribute(LSPDiagnosticsToMarkers.LSP_DIAGNOSTIC);
					CodeActionContext context = new CodeActionContextBuilder()
							.diagnostic(diagnostic)
							.build();
					CodeActionParams params = new CodeActionParamsBuilder()
							.context(context)
							.textDocument(marker.getResource().getLocation().toFile().toURI().toString())
							.range(diagnostic.getRange())
							.build();
					CompletableFuture<List<? extends Command>> codeAction = lsp.getTextDocumentService().codeAction(params);
					resolutions = codeAction.get();
					marker.setAttribute(LSP_REMEDIATION, resolutions);
				}
			}
		} catch (CoreException | InterruptedException | ExecutionException | IOException ex) {
			ex.printStackTrace(); //TODO
		}
		return resolutions != null && !resolutions.isEmpty();
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		return null;
	}
}
