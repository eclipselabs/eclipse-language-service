package org.eclipse.languageserver.languages.csharp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.languageserver.StreamConnectionProvider;

public class OmnisharpConnectionProvider implements StreamConnectionProvider {
	
	private static String LAUNCH_NAME = "OmniSharp";
	private StreamProxyInputStream inputStream;
	private OutputStream outputStream;
	private ILaunch launch;
	private IProcess process;
	
	private static class StreamProxyInputStream extends InputStream implements IStreamListener {
		
		private ConcurrentLinkedQueue<Byte> queue = new ConcurrentLinkedQueue<>();

		@Override
		public void streamAppended(String text, IStreamMonitor monitor) {
			System.err.println(text);
			byte[] bytes = text.getBytes(Charset.defaultCharset());
			List<Byte> bytesAsList = new ArrayList<>(bytes.length);
			for (byte b : bytes) {
				bytesAsList.add(b);
			}
			queue.addAll(bytesAsList);
		}

		@Override
		public int read() throws IOException {
			while (queue.isEmpty()) {
				try {
					Thread.sleep(5, 0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return queue.poll();
		}

	}

	@Override
	public void start() throws IOException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
		ILaunchConfiguration omniSharpLauch = null;
		try {
			for (ILaunchConfiguration launch : manager.getLaunchConfigurations(type)) {
				if (launch.getName().equals(LAUNCH_NAME)) {
					omniSharpLauch = launch;
				}
			}
			if (omniSharpLauch == null) {
				ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, LAUNCH_NAME);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, "/usr/bin/node");
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_BUILDER_ENABLED, false);
				workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "/home/mistria/git/omnisharp-node-client/languageserver/server.js");
				workingCopy.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, false);
				workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
				workingCopy.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, Charset.defaultCharset().name());
				workingCopy.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
				Map<String, String> environment = new HashMap<>(1);
				environment.put("LD_LIBRARY_PATH", "/home/mistria/apps/OmniSharp.NET/icu54:" + System.getenv("LD_LIBRARY_PATH"));
				workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, environment);
				omniSharpLauch = workingCopy.doSave();
			}
			launch = omniSharpLauch.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public InputStream getInputStream() {
		if (this.inputStream == null) {
			process = this.launch.getProcesses()[0];
			this.inputStream = new StreamProxyInputStream();
			process.getStreamsProxy().getOutputStreamMonitor().addListener(this.inputStream);
		}
		return this.inputStream;
	}

	public OutputStream getOutputStream() {
		if (this.outputStream == null) {
			try {
				Method systemProcessGetter = RuntimeProcess.class.getDeclaredMethod("getSystemProcess");
				systemProcessGetter.setAccessible(true);
				Process systemProcess = (Process)systemProcessGetter.invoke(process);
				this.outputStream = systemProcess.getOutputStream();
			} catch (ReflectiveOperationException ex) {
				ex.printStackTrace();
			}
		}
		return this.outputStream;
	}

	@Override
	public void stop() {
		for (IProcess p : this.launch.getProcesses()) {
			try {
				p.terminate();
			} catch (DebugException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
