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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationViewer;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;

public class NewContentTypeLSPLaunchDialog extends Dialog {
	
	////
	// copied from ContentTypesPreferencePage
	
	private class ContentTypesLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			IContentType contentType = (IContentType) element;
			return contentType.getName();
		}
	}

	private class ContentTypesContentProvider implements ITreeContentProvider {

		private IContentTypeManager manager;

		@Override
		public Object[] getChildren(Object parentElement) {
			List<IContentType> elements = new ArrayList<>();
			IContentType baseType = (IContentType) parentElement;
			IContentType[] contentTypes = manager.getAllContentTypes();
			for (int i = 0; i < contentTypes.length; i++) {
				IContentType type = contentTypes[i];
				if (Util.equals(type.getBaseType(), baseType)) {
					elements.add(type);
				}
			}
			return elements.toArray();
		}

		@Override
		public Object getParent(Object element) {
			IContentType contentType = (IContentType) element;
			return contentType.getBaseType();
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(null);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			manager = (IContentTypeManager) newInput;
		}
	}

	protected IContentType contentType;
	protected ILaunchConfiguration launchConfig;
	
	//
	////

	protected NewContentTypeLSPLaunchDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite res = (Composite)super.createDialogArea(parent);
		res.setLayout(new GridLayout(2, false));
		new Label(res, SWT.NONE).setText(Messages.NewContentTypeLSPLaunchDialog_associateContentType);
		new Label(res, SWT.NONE).setText(Messages.NewContentTypeLSPLaunchDialog_withLSPLaunch);
		// copied from ContentTypesPreferencePage
		TreeViewer contentTypesViewer = new TreeViewer(res);
		contentTypesViewer.setContentProvider(new ContentTypesContentProvider());
		contentTypesViewer.setLabelProvider(new ContentTypesLabelProvider());
		contentTypesViewer.setComparator(new ViewerComparator());
		contentTypesViewer.setInput(Platform.getContentTypeManager());
		contentTypesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IContentType newContentType = null;
				if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					if (sel.size() == 1 && sel.getFirstElement() instanceof IContentType) {
						newContentType = (IContentType)sel.getFirstElement();
					}
				}
				contentType = newContentType;
				updateButtons();
			}
		});
		// copied from LaunchConfigurationDialog : todo use LaunchConfigurationFilteredTree
		TreeViewer launchConfigViewer = new TreeViewer(res);
		launchConfigViewer.setLabelProvider(new DecoratingLabelProvider(DebugUITools.newDebugModelPresentation(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		launchConfigViewer.setContentProvider(new LaunchConfigurationTreeContentProvider(null, getShell()));
		launchConfigViewer.setInput(DebugPlugin.getDefault().getLaunchManager());
		launchConfigViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ILaunchConfiguration newLaunchConfig = null;
				if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					if (sel.size() == 1 && sel.getFirstElement() instanceof ILaunchConfiguration) {
						newLaunchConfig = (ILaunchConfiguration)sel.getFirstElement();
					}
				}
				launchConfig = newLaunchConfig;
				updateButtons();
			}
		});
		return res;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control res = super.createContents(parent);
		updateButtons();
		return res;
	}

	public IContentType getContentType() {
		return this.contentType;
	}

	public ILaunchConfiguration getLaunchConfiguration() {
		return this.launchConfig;
	}
	
	private void updateButtons() {
		getButton(OK).setEnabled(contentType != null && launchConfig != null);
	}

}
