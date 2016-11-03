/*******************************************************************************
 * Copyright (c) 2016 Rogue Wave Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.languageserver;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import io.typefox.lsapi.CompletionItemKind;
import io.typefox.lsapi.SymbolKind;

public class LSPImages {

	private static ImageRegistry imageRegistry;
	private static String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$
	private final static String OBJECT = ICONS_PATH + "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$

	public static final String IMG_MODULE = "IMG_MODULE"; //$NON-NLS-1$
	public static final String IMG_NAMESPACE = "IMG_NAMESPACE"; //$NON-NLS-1$
	public static final String IMG_PACKAGE = "IMG_PACKAGE"; //$NON-NLS-1$
	public static final String IMG_CLASS = "IMG_CLASS"; //$NON-NLS-1$
	public static final String IMG_METHOD = "IMG_METOHD"; //$NON-NLS-1$
	public static final String IMG_PROPERTY = "IMG_PROPERTY"; //$NON-NLS-1$
	public static final String IMG_FIELD = "IMG_FIELD"; //$NON-NLS-1$
	public static final String IMG_CONSTRUCTOR = "IMG_CONSTRUCTOR"; //$NON-NLS-1$
	public static final String IMG_ENUM = "IMG_ENUM"; //$NON-NLS-1$
	public static final String IMG_INTERACE = "IMG_INTERFACE"; //$NON-NLS-1$
	public static final String IMG_FUNCTION = "IMG_FUNCTION"; //$NON-NLS-1$
	public static final String IMG_VARIABLE = "IMG_VARIABLE"; //$NON-NLS-1$
	public static final String IMG_CONSTANT = "IMG_CONSTANT"; //$NON-NLS-1$
	public static final String IMG_STRING = "IMG_STRING"; //$NON-NLS-1$
	public static final String IMG_NUMBER = "IMG_NUMBER"; //$NON-NLS-1$
	public static final String IMG_BOOLEAN = "IMG_BOOLEAN"; //$NON-NLS-1$
	public static final String IMG_ARRAY = "IMG_ARRAY"; //$NON-NLS-1$

	public static final String IMG_TEXT = "IMG_TEXT"; //$NON-NLS-1$
	public static final String IMG_UNIT = "IMG_UNIT"; //$NON-NLS-1$
	public static final String IMG_VALUE = "IMG_VALUE"; //$NON-NLS-1$
	public static final String IMG_KEYWORD = "IMG_KEYWORD"; //$NON-NLS-1$
	public static final String IMG_SNIPPET = "IMG_SNIPPET"; //$NON-NLS-1$
	public static final String IMG_COLOR = "IMG_COLOR"; //$NON-NLS-1$
	public static final String IMG_REFERENCE = "IMG_REFERENCE"; //$NON-NLS-1$

	public static void initalize(ImageRegistry registry) {
		imageRegistry = registry;

		declareRegistryImage(IMG_MODULE, OBJECT + "module.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_NAMESPACE, OBJECT + "namespace.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_PACKAGE, OBJECT + "package.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_CLASS, OBJECT + "class.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_METHOD, OBJECT + "method.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_PROPERTY, OBJECT + "property.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_FIELD, OBJECT + "field.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_CONSTRUCTOR, OBJECT + "constructor.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_ENUM, OBJECT + "enum.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_INTERACE, OBJECT + "interface.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_FUNCTION, OBJECT + "function.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_VARIABLE, OBJECT + "variable.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_CONSTANT, OBJECT + "constant.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_STRING, OBJECT + "string.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_NUMBER, OBJECT + "number.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_BOOLEAN, OBJECT + "boolean.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_ARRAY, OBJECT + "array.png"); //$NON-NLS-1$

		declareRegistryImage(IMG_TEXT, OBJECT + "text.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_UNIT, OBJECT + "unit.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_VALUE, OBJECT + "value.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_KEYWORD, OBJECT + "keyword.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_SNIPPET, OBJECT + "snippet.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_COLOR, OBJECT + "color.png"); //$NON-NLS-1$
		declareRegistryImage(IMG_REFERENCE, OBJECT + "reference.png"); //$NON-NLS-1$
	}

	private final static void declareRegistryImage(String key, String path) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		Bundle bundle = Platform.getBundle(LanguageServerPluginActivator.PLUGIN_ID);
		URL url = null;
		if (bundle != null) {
			url = FileLocator.find(bundle, new Path(path), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		imageRegistry.put(key, desc);
	}

	/**
	 * Returns the <code>Image</code> identified by the given key, or
	 * <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor</code> identified by the given key, or
	 * <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getImageRegistry().getDescriptor(key);
	}

	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = LanguageServerPluginActivator.getDefault().getImageRegistry();
		}
		return imageRegistry;
	}

	public static Image imageFromSymbolKind(SymbolKind kind) {
		switch (kind) {
		case Array:
			return getImage(IMG_ARRAY);
		case Boolean:
			return getImage(IMG_BOOLEAN);
		case Class:
			return getImage(IMG_CLASS);
		case Constant:
			return getImage(IMG_CONSTANT);
		case Constructor:
			return getImage(IMG_CONSTRUCTOR);
		case Enum:
			return getImage(IMG_ENUM);
		case Field:
			return getImage(IMG_FIELD);
		case File:
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		case Function:
			return getImage(IMG_FUNCTION);
		case Interface:
			return getImage(IMG_INTERACE);
		case Method:
			return getImage(IMG_METHOD);
		case Module:
			return getImage(IMG_MODULE);
		case Namespace:
			return getImage(IMG_NAMESPACE);
		case Number:
			return getImage(IMG_NUMBER);
		case Package:
			return getImage(IMG_PACKAGE);
		case Property:
			return getImage(IMG_PROPERTY);
		case String:
			return getImage(IMG_STRING);
		case Variable:
			return getImage(IMG_VARIABLE);
		}
		return null;
	}

	public static Image imageFromCompletionKind(CompletionItemKind kind) {
		switch (kind) {
		case Text:
			return getImage(IMG_TEXT);
		case Method:
			return getImage(IMG_METHOD);
		case Function:
			return getImage(IMG_FUNCTION);
		case Constructor:
			return getImage(IMG_CONSTRUCTOR);
		case Field:
			return getImage(IMG_FIELD);
		case Variable:
			return getImage(IMG_VARIABLE);
		case Class:
			return getImage(IMG_CLASS);
		case Interface:
			return getImage(IMG_INTERACE);
		case Module:
			return getImage(IMG_MODULE);
		case Property:
			return getImage(IMG_PROPERTY);
		case Unit:
			return getImage(IMG_UNIT);
		case Value:
			return getImage(IMG_VALUE);
		case Enum:
			return getImage(IMG_ENUM);
		case Keyword:
			return getImage(IMG_KEYWORD);
		case Snippet:
			return getImage(IMG_SNIPPET);
		case Color:
			return getImage(IMG_COLOR); //Color TODO use Gef Palette icon or generate color image
		case File:
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		case Reference:
			return getImage(IMG_REFERENCE);
		}
		return null;
	}

}
