/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver.languages;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.languageserver.LSPStreamConnectionProviderRegistry;
import org.eclipse.languageserver.LaunchConfigurationStreamProvider;
import org.eclipse.ui.IStartup;

/**
 * Initialize the LaunchConfiguration that will be used to start language servers.
 * TODO: find a better way to contribute that, or maybe use some dedicated launch types.
 * @author mistria
 *
 */
public class InitializeLaunchConfigurations implements IStartup {

	public static final String VSCODE_CSS_NAME = "VSCode-CSS";
	public static final String VSCODE_JSON_NAME = "VSCode-JSON";
	public static final String OMNISHARP_NAME = "OmniSharp";

	@Override
	public void earlyStartup() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType externalType = launchManager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
		LSPStreamConnectionProviderRegistry registry = LSPStreamConnectionProviderRegistry.getInstance();
		// OmniSharp
		try {
			String omniSharpLaunchName = OMNISHARP_NAME;
			ILaunchConfiguration omniSharpLauch = null;
			for (ILaunchConfiguration launch : launchManager.getLaunchConfigurations(externalType)) {
				if (launch.getName().equals(omniSharpLaunchName)) {
					omniSharpLauch = launch;
				}
			}
			if (omniSharpLauch == null) {
				ILaunchConfigurationWorkingCopy workingCopy = externalType.newInstance(null, omniSharpLaunchName);
				// some common config
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILDER_ENABLED, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILD_SCOPE, "${none}");
				workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, "/usr/bin/node");
				workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "/home/mistria/git/omnisharp-node-client/languageserver/server.js");
				workingCopy.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
				Map<String, String> environment = new HashMap<>(1);
				environment.put("LD_LIBRARY_PATH", "/home/mistria/apps/OmniSharp.NET/icu54:" + System.getenv("LD_LIBRARY_PATH"));
				workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, environment);
				omniSharpLauch = workingCopy.doSave();
				registry.registerAssociation(contentTypeManager.getContentType("org.eclipse.languageserver.csharp"), LaunchConfigurationStreamProvider.findLaunchConfiguration(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE, InitializeLaunchConfigurations.OMNISHARP_NAME));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		// VSCode CSS
		try {
			String omniSharpLaunchName = VSCODE_CSS_NAME;
			ILaunchConfiguration omniSharpLauch = null;
			for (ILaunchConfiguration launch : launchManager.getLaunchConfigurations(externalType)) {
				if (launch.getName().equals(omniSharpLaunchName)) {
					omniSharpLauch = launch;
				}
			}
			if (omniSharpLauch == null) {
				ILaunchConfigurationWorkingCopy workingCopy = externalType.newInstance(null, omniSharpLaunchName);
				// some common config
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILDER_ENABLED, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILD_SCOPE, "${none}");
				workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
				// Steps to get a usable Language server (for language supported by VSCode LS implementations)
				// 1. Make VSCode LS [for CSS here] use STDIN/STDOUT. In file extensions/css/server/cssServerMain.ts change connection line to
				//       let connection: IConnection = createConnection(new StreamMessageReader(process.in), new StreamMessageWriter(process.out));
				//    and add imports  StreamMessageReader, StreamMessageWriter (as same level as IPCMessageReader/Writer)
				// 2. Build VSCode as explained in https://github.com/Microsoft/vscode/wiki/How-to-Contribute#build-and-run-from-source
				//     (don't forget the `npm run watch` after the install script)
				// 3. Then set language server location and adapt process builder.
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, "/usr/bin/node");
				// Arguments are: CSS server & --stdio to support stdin/stdout on server side because launch can work only with stdio transport (not with IPC transport)
				workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "/home/mistria/git/vscode/extensions/css/server/out/cssServerMain.js --stdio");
				omniSharpLauch = workingCopy.doSave();
				registry.registerAssociation(contentTypeManager.getContentType("org.eclipse.wst.css.core.csssource"), LaunchConfigurationStreamProvider.findLaunchConfiguration(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE, InitializeLaunchConfigurations.VSCODE_CSS_NAME));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		// VSCode CSS
		try {
			String omniSharpLaunchName = VSCODE_JSON_NAME;
			ILaunchConfiguration omniSharpLauch = null;
			for (ILaunchConfiguration launch : launchManager.getLaunchConfigurations(externalType)) {
				if (launch.getName().equals(omniSharpLaunchName)) {
					omniSharpLauch = launch;
				}
			}
			if (omniSharpLauch == null) {
				ILaunchConfigurationWorkingCopy workingCopy = externalType.newInstance(null, omniSharpLaunchName);
				// some common config
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILDER_ENABLED, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILD_SCOPE, "${none}");
				workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
				// Steps to get a usable Language server (for language supported by VSCode LS implementations)
				// 1. Make VSCode LS [for JS here] use STDIN/STDOUT. In file extensions/json/server/jsonServerMain.ts change connection line to
				//       let connection: IConnection = createConnection(new StreamMessageReader(process.in), new StreamMessageWriter(process.out));
				//    and add imports  StreamMessageReader, StreamMessageWriter (as same level as IPCMessageReader/Writer)
				// 2. Build VSCode as explained in https://github.com/Microsoft/vscode/wiki/How-to-Contribute#build-and-run-from-source
				//     (don't forget the `npm run watch` after the install script)
				// 3. Then set language server location and adapt process builder.
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, "/usr/bin/node");
				// Arguments are: JSON server & --stdio to support stdin/stdout on server side because launch can work only with stdio transport (not with IPC transport)
				workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "/home/mistria/git/vscode/extensions/json/server/out/jsonServerMain.js --stdio");
				omniSharpLauch = workingCopy.doSave();
				registry.registerAssociation(contentTypeManager.getContentType("org.eclipse.wst.jsdt.core.jsonSource"), LaunchConfigurationStreamProvider.findLaunchConfiguration(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE, InitializeLaunchConfigurations.VSCODE_JSON_NAME));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
