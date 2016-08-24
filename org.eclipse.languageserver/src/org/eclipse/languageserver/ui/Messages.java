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
package org.eclipse.languageserver.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String hyperlinkLabel;
	public static String PreferencesPage_LaunchConfiguration;
	public static String PreferencesPage_Add;
	public static String PreferencesPage_Remove;
	public static String PreferencesPage_contentType;
	public static String NewContentTypeLSPLaunchDialog_associateContentType;
	public static String NewContentTypeLSPLaunchDialog_withLSPLaunch;
	public static String PreferencesPage_Intro;

	static {
		NLS.initializeMessages(Messages.class.getPackage().getName() + ".messages", Messages.class);
	}
}
