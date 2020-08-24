package com.katalon.plugin.smart_xpath.part;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.katalon.plugin.smart_xpath.constant.SmartXPathConstants;
import com.katalon.plugin.smart_xpath.constant.SmartXPathMessageConstants;
import com.katalon.plugin.smart_xpath.controller.AutoHealingController;
import com.katalon.plugin.smart_xpath.entity.BrokenTestObject;
import com.katalon.plugin.smart_xpath.entity.BrokenTestObjects;
import com.katalon.plugin.smart_xpath.helpers.FileWatcher;
import com.katalon.plugin.smart_xpath.part.composites.BrokenTestObjectsTableComposite;
import com.katalon.plugin.smart_xpath.part.composites.SelfHealingToolbarComposite;
import com.kms.katalon.composer.components.application.ApplicationSingleton;
import com.kms.katalon.composer.components.services.ModelServiceSingleton;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.tracking.service.Trackings;

public class SelfHealingInsightsPart implements EventHandler {

    @Inject
    private UISynchronize sync;

    @Inject
    private IEventBroker eventBroker;

    protected BrokenTestObjectsTableComposite brokenTestObjectsTableComposite;

    protected SelfHealingToolbarComposite toolbarComposite;

    private FileWatcher dataWatcher;

    private static SelfHealingInsightsPart prevInstance;

    // This property is used to workaround bug #549654
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=549654
    private static long prevFocusTime = 0;

    @PostConstruct
    public void init(Composite parent) {
        createContents(parent);
        registerEventListeners();
        initialize();

        if (prevInstance != null) {
            prevInstance.preDestroy();
        }
        prevInstance = this;
    }

    @PreDestroy
    public void preDestroy() {
        eventBroker.unsubscribe(this);
        if (dataWatcher != null) {
            dataWatcher.stop();
            dataWatcher = null;
        }
        if (!brokenTestObjectsTableComposite.isDisposed()) {
            brokenTestObjectsTableComposite.dispose();
            brokenTestObjectsTableComposite = null;
        }
        if (!toolbarComposite.isDisposed()) {
            toolbarComposite.dispose();
            toolbarComposite = null;
        }
    }

    protected Control createContents(Composite parent) {
        Composite container = createContainer(parent);

        createBrokenTestObjectsTable(container);
        createToolbar(container);

        return parent;
    }

    protected Composite createContainer(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        return container;
    }

    private Composite createBrokenTestObjectsTable(Composite parent) {
        brokenTestObjectsTableComposite = new BrokenTestObjectsTableComposite(parent, SWT.NONE);
        return brokenTestObjectsTableComposite;
    }

    protected Composite createToolbar(Composite parent) {
        toolbarComposite = new SelfHealingToolbarComposite(parent, SWT.NONE);

        toolbarComposite.addApproveListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Set<BrokenTestObject> selectedBrokenTestObjects = brokenTestObjectsTableComposite
                        .getSelectedTestObjects();
                int numSelectedTestObjects = selectedBrokenTestObjects.size();
                AutoHealingController.autoHealBrokenTestObjects(Display.getCurrent().getActiveShell(),
                        selectedBrokenTestObjects);

                ProjectEntity currentProject = ProjectController.getInstance().getCurrentProject();
                Set<BrokenTestObject> deselectedBrokenTestObjects = brokenTestObjectsTableComposite
                        .getDeselectedTestObjects();
                BrokenTestObjects brokenTestObjects = new BrokenTestObjects();
                brokenTestObjects.setBrokenTestObjects(deselectedBrokenTestObjects);
                AutoHealingController.writeBrokenTestObjects(brokenTestObjects, currentProject);

                refresh();
                toolbarComposite.notifyRecoverSucceeded(numSelectedTestObjects);

                String approvedProposals = selectedBrokenTestObjects.stream()
                        .map(brokenTestObject -> MessageFormat.format("{0}:{1}",
                                brokenTestObject.getBrokenLocatorMethod(), brokenTestObject.getRecoveryMethod()))
                        .collect(Collectors.joining(","));
                Trackings.trackApproveSelfHealingTestObjects(approvedProposals);
            }
        });

        toolbarComposite.addDiscardListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Set<BrokenTestObject> selectedBrokenTestObjects = brokenTestObjectsTableComposite
                        .getSelectedTestObjects();
                int numSelectedTestObjects = selectedBrokenTestObjects.size();
                if (numSelectedTestObjects > 0) {
                    toolbarComposite.clearStatusMessage();
                    boolean isOK = MessageDialog.openQuestion(parent.getShell(),
                            SmartXPathConstants.SELF_HEALING_INSIGHTS_PART_LABEL,
                            MessageFormat.format(SmartXPathMessageConstants.MSG_DISCARD_BROKEN_TEST_OBJECT_DIALOG,
                                    numSelectedTestObjects));
                    if (isOK) {
                        ProjectEntity currentProject = ProjectController.getInstance().getCurrentProject();
                        Set<BrokenTestObject> deselectedBrokenTestObjects = brokenTestObjectsTableComposite
                                .getDeselectedTestObjects();
                        BrokenTestObjects brokenTestObjects = new BrokenTestObjects();
                        brokenTestObjects.setBrokenTestObjects(deselectedBrokenTestObjects);
                        AutoHealingController.writeBrokenTestObjects(brokenTestObjects, currentProject);

                        refresh();
                        toolbarComposite.notifyDiscardSucceeded(numSelectedTestObjects);

                        String discardedProposals = selectedBrokenTestObjects.stream()
                                .map(brokenTestObject -> MessageFormat.format("{0}:{1}",
                                        brokenTestObject.getBrokenLocatorMethod(), brokenTestObject.getRecoveryMethod()))
                                .collect(Collectors.joining(","));
                        Trackings.trackDiscardSelfHealingTestObjects(discardedProposals);
                    }
                }
            }
        });

        return toolbarComposite;
    }

    private void registerEventListeners() {
        eventBroker.subscribe(EventConstants.SEFL_HEALING_INSIGHTS_REFRESH, this);
        eventBroker.subscribe(EventConstants.JOB_COMPLETED, this);
        eventBroker.subscribe(EventConstants.PROJECT_OPENED, this);
        eventBroker.subscribe(EventConstants.PROJECT_CLOSED, this);
        trackBrokenTestObjectsFile();
    }

    private void trackBrokenTestObjectsFile() {
        ProjectEntity currentProject = ProjectController.getInstance().getCurrentProject();
        if (currentProject == null) {
            if (dataWatcher != null) {
                dataWatcher.stop();
                dataWatcher = null;
            }
            return;
        }
        String dataFilePath = AutoHealingController.getDataFilePath(currentProject);

        if (dataWatcher == null) {
            dataWatcher = FileWatcher.watch(dataFilePath);
            dataWatcher.addEventListener((kind, file) -> {
                sync.syncExec(() -> {
                    refresh();
                });
            });
        } else {
            dataWatcher.updateTrackedFile(dataFilePath);
        }
    }

    protected void initialize() {
        refresh();
    }

    @Focus
    public void onFocus() {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=549654
        if (System.currentTimeMillis() - prevFocusTime < 1000) {
            return;
        }
        prevFocusTime = System.currentTimeMillis();

        refresh();
        Trackings.trackOpenSelfHealingInsights();
    }

    @Override
    public void handleEvent(Event event) {
        refresh();
        toolbarComposite.clearStatusMessage();

        if (StringUtils.equals(EventConstants.PROJECT_OPENED, event.getTopic())
                || StringUtils.equals(EventConstants.PROJECT_CLOSED, event.getTopic())) {
            trackBrokenTestObjectsFile();
        }
    }

    private void refresh() {
        loadBrokenTestObjects();
        addNotificationNumber();
    }

    public void loadBrokenTestObjects() {
        ProjectEntity currentProject = ProjectController.getInstance().getCurrentProject();
        brokenTestObjectsTableComposite.setProject(currentProject);
        Set<BrokenTestObject> brokenTestObjects = AutoHealingController.readUnapprovedBrokenTestObjects(currentProject);
        brokenTestObjectsTableComposite.setInput(brokenTestObjects);
    }

    private void addNotificationNumber() {
        EModelService modelService = ModelServiceSingleton.getInstance().getModelService();
        MApplication application = ApplicationSingleton.getInstance().getApplication();

        List<MPerspectiveStack> psList = modelService.findElements(application, null, MPerspectiveStack.class, null);
        MPartStack consolePartStack = (MPartStack) modelService.find(IdConstants.CONSOLE_PART_STACK_ID,
                psList.get(0).getSelectedElement());
        MPart selfHealingInsightsPart = (MPart) modelService.find(SmartXPathConstants.SELF_HEALING_INSIGHTS_PART_ID,
                consolePartStack);

        int numBrokenTestObjects = 0;
        Set<BrokenTestObject> brokenTestObjects = brokenTestObjectsTableComposite.getInput();
        if (brokenTestObjects != null && !brokenTestObjects.isEmpty()) {
            numBrokenTestObjects = brokenTestObjects.size();
        }

        String selfHealingInsightsLabel = SmartXPathConstants.SELF_HEALING_INSIGHTS_PART_LABEL;

        if (numBrokenTestObjects > 0) {
            selfHealingInsightsPart.getTransientData().put(SmartXPathConstants.ICON_URI_FOR_PART, SmartXPathConstants.NOTIFICATION_SELF_HEALING_ICON);
            selfHealingInsightsPart.setIconURI(SmartXPathConstants.NOTIFICATION_SELF_HEALING_ICON);
            selfHealingInsightsPart.setLabel(MessageFormat.format("{0} ({1})", selfHealingInsightsLabel, numBrokenTestObjects));
        } else {
            selfHealingInsightsPart.getTransientData().put(SmartXPathConstants.ICON_URI_FOR_PART, SmartXPathConstants.SELF_HEALING_ICON);
            selfHealingInsightsPart.setIconURI(SmartXPathConstants.SELF_HEALING_ICON);
            selfHealingInsightsPart.setLabel(selfHealingInsightsLabel);
        }
    }
}