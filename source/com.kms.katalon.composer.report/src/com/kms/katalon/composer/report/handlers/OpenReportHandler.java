package com.kms.katalon.composer.report.handlers;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.report.constants.ImageConstants;
import com.kms.katalon.composer.report.constants.StringConstants;
import com.kms.katalon.composer.report.parts.ReportPart;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.core.reporting.ReportUtil;
import com.kms.katalon.entity.report.ReportEntity;
import com.kms.katalon.execution.launcher.AbstractLauncher;
import com.kms.katalon.execution.launcher.IDELauncher;
import com.kms.katalon.execution.launcher.manager.LauncherManager;
import com.kms.katalon.execution.launcher.model.LauncherStatus;

public class OpenReportHandler {
	private static final String REPORT_PART_URI = "bundleclass://com.kms.katalon.composer.report/"
			+ ReportPart.class.getName();

	@Inject
	MApplication application;

	@Inject
	EPartService partService;

	@Inject
	EModelService modelService;

	@Inject
	private IEclipseContext context;

	// @Inject
	@PostConstruct
	public void registerEventHandler(IEventBroker eventBroker) {
		eventBroker.subscribe(EventConstants.EXPLORER_OPEN_SELECTED_ITEM, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				Object object = event.getProperty(EventConstants.EVENT_DATA_PROPERTY_NAME);
				if (object != null && object instanceof ReportEntity) {
					excute((ReportEntity) object);
				}
			}
		});
	}

	@Inject
	@Optional
	private void getNotifications(@UIEventTopic(EventConstants.REPORT_OPEN) ReportEntity entity) {
		excute(entity);
	}

	public void excute(ReportEntity report) {
		try {
			if (report != null && report.getId() != null) {
				// Generate HTML file if it is not created
				File htmlFile = new File(report.getHtmlFile());
				if (!htmlFile.exists()) {
					String reportFolder = new File(report.getLocation()).getCanonicalPath();
					// To check if this report is still being written by a
					// current execution
					boolean isRunning = false;
					for (AbstractLauncher launcher : LauncherManager.getInstance().getIDELaunchers()) {
						if (launcher instanceof IDELauncher) {
							IDELauncher qLauncher = (IDELauncher) launcher;
							if (qLauncher.getStatus() != LauncherStatus.DONE
									&& qLauncher.getStatus() != LauncherStatus.TERMINATED) {
								if (qLauncher.getCurrentLogFile() != null) {
									String currentRunningLogFolder = qLauncher.getCurrentLogFile().getParentFile()
											.getCanonicalPath();
									isRunning = reportFolder.equals(currentRunningLogFolder);
									if (isRunning) {
										break;
									}
								}
							}
						}
					}
					if (!isRunning) {
						ReportUtil.writeLogRecordToFiles(reportFolder);
					}
				}
				
				String partId = IdConstants.REPORT_CONTENT_PART_ID_PREFIX + "(" + report.getId() + ")";
				MPartStack stack = (MPartStack) modelService.find(IdConstants.COMPOSER_CONTENT_PARTSTACK_ID,
						application);
				MPart mPart = (MPart) modelService.find(partId, application);
				if (mPart == null) {
					mPart = modelService.createModelElement(MPart.class);
					mPart.setElementId(partId);
					mPart.setLabel(report.getName());
					mPart.setContributionURI(REPORT_PART_URI);
					mPart.setCloseable(true);
					mPart.setIconURI(ImageConstants.URL_16_REPORT);
					stack.getChildren().add(mPart);
				}
				context.set(ReportEntity.class, report);
				partService.showPart(mPart, PartState.ACTIVATE);
				stack.setSelectedElement(mPart);
			}
		} catch (Exception e) {
			LoggerSingleton.logError(e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error",
					StringConstants.HAND_ERROR_MSG_UNABLE_TO_OPEN_REPORT + " (" + e.getMessage() + ")");
		}
	}
}