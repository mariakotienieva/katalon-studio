package com.kms.katalon.composer.execution.handlers;

import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;

import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.execution.launcher.manager.LauncherManager;

public class CleanAllTerminatedHandler {
	@Inject
	private IEventBroker eventBroker;

	@SuppressWarnings("restriction")
	@Execute
	public void execute() {
		try {
			LauncherManager.getInstance().removeAllTerminated();
			eventBroker.post(EventConstants.JOB_REFRESH, null);
		} catch (CoreException e) {
			LoggerSingleton.getInstance().getLogger().error(e);
		}
	}
}