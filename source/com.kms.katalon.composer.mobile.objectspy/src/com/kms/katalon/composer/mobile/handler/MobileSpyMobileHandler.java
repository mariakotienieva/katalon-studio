package com.kms.katalon.composer.mobile.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.composer.components.impl.tree.FolderTreeEntity;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.mobile.constants.StringConstants;
import com.kms.katalon.composer.mobile.objectspy.dialog.MobileDeviceDialog;
import com.kms.katalon.composer.mobile.objectspy.dialog.MobileObjectSpyDialog;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.folder.FolderEntity;

public class MobileSpyMobileHandler implements EventHandler {
    private static final int DIALOG_MARGIN_OFFSET = 5;

    @Inject
    private IEventBroker eventBroker;

    @Inject
    private ESelectionService selectionService;

    private MobileObjectSpyDialog objectSpyDialog;

    private MobileDeviceDialog deviceView;

    private Shell activeShell;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
        try {
            if (this.activeShell == null) {
                this.activeShell = activeShell;
            }
            eventBroker.subscribe(EventConstants.OBJECT_SPY_REFRESH_SELECTED_TARGET, this);
            eventBroker.subscribe(EventConstants.OBJECT_SPY_ENSURE_DEVICE_VIEW_DIALOG, this);
            if (!isObjectSpyDialogRunning()) {
                objectSpyDialog = new MobileObjectSpyDialog(activeShell, calculateInitPositionForObjectSpyDialog(),
                        LoggerSingleton.getInstance().getLogger(), eventBroker, selectionService);
                objectSpyDialog.open();
            }
            if (!isDeviceViewRunning()) {
                deviceView = new MobileDeviceDialog(activeShell,
                        calculateInitPositionForDeviceViewDialog(objectSpyDialog), LoggerSingleton.getInstance()
                                .getLogger(), eventBroker);
                deviceView.open();
            }
        } catch (Exception e) {
            if (isObjectSpyDialogRunning()) {
                objectSpyDialog.dispose();
                objectSpyDialog.close();
            }
            if (isDeviceViewRunning()) {
                deviceView.dispose();
                deviceView.close();
            }
            LoggerSingleton.logError(e);
            MessageDialog.openError(activeShell, StringConstants.ERROR_TITLE, e.getMessage());
        }
    }

    public Point calculateInitPositionForDeviceViewDialog(MobileObjectSpyDialog objectSpyViewDialog) {
        Rectangle displayBounds = Display.getCurrent().getPrimaryMonitor().getBounds();
        Point dialogSize = MobileDeviceDialog.DIALOG_SIZE;
        Rectangle objectSpyViewBounds = objectSpyViewDialog.getShell().getBounds();
        int startX = getDeviceViewStartXIfPlaceRight(objectSpyViewBounds);
        if (isOutOfBound(displayBounds, dialogSize, startX)) {
            startX = getDeviceViewStartXIfPlaceLeft(objectSpyViewBounds, dialogSize);
            if (isOutOfBound(displayBounds, dialogSize, startX)) {
                startX = getDefaultDeviceViewDialogStartX(displayBounds, dialogSize);
            }
        }
        return new Point(startX, objectSpyViewBounds.y);
    }

    public boolean isOutOfBound(Rectangle displayBounds, Point dialogSize, int startX) {
        return startX < 0 || startX + dialogSize.x > displayBounds.width;
    }

    public int getDeviceViewStartXIfPlaceRight(Rectangle objectSpyViewBounds) {
        return objectSpyViewBounds.x + objectSpyViewBounds.width + DIALOG_MARGIN_OFFSET;
    }
    
    public int getDeviceViewStartXIfPlaceLeft(Rectangle objectSpyViewBounds, Point dialogSize) {
        return objectSpyViewBounds.x - dialogSize.x - DIALOG_MARGIN_OFFSET;
    }

    public int getDefaultDeviceViewDialogStartX(Rectangle displayBounds, Point dialogSize) {
        return displayBounds.width - dialogSize.x;
    }

    public Point calculateInitPositionForObjectSpyDialog() {
        Rectangle displayBounds = Display.getCurrent().getPrimaryMonitor().getBounds();
        Point dialogSize = MobileObjectSpyDialog.DIALOG_SIZE;
        return new Point(calculateObjectSpyDialogStartX(displayBounds, dialogSize), calculateObjectSpyDialogStartY(displayBounds, dialogSize));
    }

    public int calculateObjectSpyDialogStartX(Rectangle displayBounds, Point dialogSize) {
        int dialogsWidth = dialogSize.x + MobileDeviceDialog.DIALOG_SIZE.x;
        int startX = (displayBounds.width - dialogsWidth) / 2;
        if (startX < 0) {
            return 0;
        }
        return startX;
    }

    public int calculateObjectSpyDialogStartY(Rectangle displayBounds, Point dialogSize) {
        if (displayBounds.height < dialogSize.y) {
            return 0;
        }
        return (displayBounds.height - dialogSize.y) / 2;
    }

    @CanExecute
    private boolean canExecute() throws Exception {
        return ProjectController.getInstance().getCurrentProject() != null;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EventConstants.OBJECT_SPY_REFRESH_SELECTED_TARGET)) {
            if (!isObjectSpyDialogRunning()) {
                return;
            }
            refreshSelectedTarget();
        } else if (event.getTopic().equals(EventConstants.OBJECT_SPY_ENSURE_DEVICE_VIEW_DIALOG)) {
            ensureDeviceViewOpen();
        }
    }

    public void refreshSelectedTarget() {
        try {
            FolderEntity parentFolder = objectSpyDialog.getParentFolder();
            eventBroker.send(EventConstants.EXPLORER_REFRESH_SELECTED_ITEM, new FolderTreeEntity(parentFolder,
                    null));
        } catch (Exception e) {
            LoggerSingleton.logError(e);
        }
    }

    public void ensureDeviceViewOpen() {
        if (isDeviceViewRunning() || activeShell == null) {
            return;
        }
        deviceView = new MobileDeviceDialog(activeShell,
                calculateInitPositionForDeviceViewDialog(objectSpyDialog), LoggerSingleton.getInstance()
                        .getLogger(), eventBroker);
        deviceView.open();
    }

    public boolean isObjectSpyDialogRunning() {
        return objectSpyDialog != null && !objectSpyDialog.isDisposed();
    }

    public boolean isDeviceViewRunning() {
        return deviceView != null && !deviceView.isDisposed();
    }
}
