package com.kms.katalon.execution.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.katalon.platform.api.event.ExecutionEvent;
import com.katalon.platform.api.execution.TestSuiteExecutionContext;
import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.controller.ReportController;
import com.kms.katalon.core.logging.model.TestStatus.TestStatusValue;
import com.kms.katalon.dal.exception.DALException;
import com.kms.katalon.entity.report.ReportCollectionEntity;
import com.kms.katalon.entity.report.ReportItemDescription;
import com.kms.katalon.entity.testsuite.TestSuiteCollectionEntity.ExecutionMode;
import com.kms.katalon.execution.entity.TestSuiteCollectionExecutedEntity;
import com.kms.katalon.execution.entity.TestSuiteCollectionExecutionContextImpl;
import com.kms.katalon.execution.launcher.listener.LauncherEvent;
import com.kms.katalon.execution.launcher.listener.LauncherListener;
import com.kms.katalon.execution.launcher.listener.LauncherNotifiedObject;
import com.kms.katalon.execution.launcher.manager.LauncherManager;
import com.kms.katalon.execution.launcher.result.ExecutionEntityResult;
import com.kms.katalon.execution.launcher.result.ILauncherResult;
import com.kms.katalon.execution.launcher.result.LauncherResult;
import com.kms.katalon.execution.launcher.result.LauncherStatus;
import com.kms.katalon.execution.platform.TestSuiteCollectionExecutionEvent;
import com.kms.katalon.logging.LogUtil;
import com.kms.katalon.tracking.service.Trackings;

public class TestSuiteCollectionLauncher extends BasicLauncher implements LauncherListener {

    public static final int MAX_NUMBER_INSTANCES_IN_PARALLEL_MODE = 8;

    protected List<ReportableLauncher> subLaunchers;

    private LauncherResult result;

    protected TestSuiteCollectionLauncherManager subLauncherManager;

    private LauncherManager parentManager;

    private Thread watchDog;

    private TestSuiteCollectionExecutedEntity executedEntity;

    private ExecutionMode executionMode;

    private ReportCollectionEntity reportCollection;
    
    private ReportableLauncher flagToUploadReport;

    private Date startTime;

    private Date endTime;

    public TestSuiteCollectionLauncher(TestSuiteCollectionExecutedEntity executedEntity, LauncherManager parentManager,
            List<ReportableLauncher> subLaunchers, ExecutionMode executionMode,
            ReportCollectionEntity reportCollection,
            String execytionUUID) {
        super.setExecutionUUID(execytionUUID);
        this.subLauncherManager = new TestSuiteCollectionLauncherManager();
        this.subLaunchers = subLaunchers;
        for (ReportableLauncher subLauncher : subLaunchers) {
            subLauncher.setExecutionUUID(super.getExecutionUUID());
        }
        this.result = new LauncherResult(executedEntity.getTotalTestCases());
        this.parentManager = parentManager;
        this.executedEntity = executedEntity;
        this.executionMode = executionMode;
        this.reportCollection = reportCollection;
        addListenerForChildren(subLaunchers);
    }

    private void addListenerForChildren(List<? extends ReportableLauncher> subLaunchers) {
        for (ReportableLauncher childLauncher : subLaunchers) {
            childLauncher.addListener(this);
            childLauncher.setParentLauncher(this);
        }
        flagToUploadReport = subLaunchers.get(0);
    }

    @Override
    public void start() throws IOException {
        setStatus(LauncherStatus.RUNNING);

        preStarting();

        scheduleSubLaunchers();

        startWatchDog();

        startTime = new Date();
        fireTestSuiteExecutionEvent(ExecutionEvent.TEST_SUITE_COLLECTION_STARTED_EVENT);
    }

    private void scheduleSubLaunchers() {
        for (ReportableLauncher launcher : subLaunchers) {
            subLauncherManager.addLauncher(launcher);
            launcher.setManager(subLauncherManager);
        }
    }

    protected void preStarting() {
        // Children may override this
    }

    private void startWatchDog() {
        watchDog = new Thread(new Runnable() {
            @Override
            public void run() {
                while (subLauncherManager.isAnyLauncherRunning()) {
                    try {
                        Thread.sleep(IWatcher.DF_TIME_OUT_IN_MILLIS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                flagToUploadReport.uploadReportTestSuiteCollection();
                setStatus(LauncherStatus.DONE);
                postExecution();
            }
        });
        watchDog.start();
    }

    protected void postExecution() {
        schedule();

        endTime = new Date();
        
        fireTestSuiteExecutionEvent(ExecutionEvent.TEST_SUITE_COLLECTION_FINISHED_EVENT);
    }

    protected void schedule() {
        try {
            parentManager.stopRunningAndSchedule(this);
        } catch (InterruptedException e) {
            LogUtil.logError(e);
        }
    }

    @Override
    public void setStatus(LauncherStatus status) {
        super.setStatus(status);
        if (LauncherStatus.DONE == status || LauncherStatus.TERMINATED == status) {
            ExecutionEntityResult executionResult = new ExecutionEntityResult();
            executionResult.setEnd(true);
            notifyProccess(status, executedEntity, executionResult);
        }
    }
    
    @Override
    public void stop() {
        setStatus(LauncherStatus.TERMINATED);

        if (watchDog != null && watchDog.isAlive()) {
            watchDog.interrupt();
        }

        subLauncherManager.stopAllLauncher();

        postExecution();
    }

    @Override
    public void clean() {
        for (ReportableLauncher launcher : subLaunchers) {
            launcher.clean();
        }
    }

    @Override
    public String getId() {
        return executedEntity.getId();
    }

    @Override
    public String getName() {
        return executedEntity.getSourceId() + " - " + executedEntity.getId();
    }

    @Override
    public ILauncherResult getResult() {
        return result;
    }

    public class TestSuiteCollectionLauncherManager extends LauncherManager {
        protected boolean isLauncherReadyToRun(ILauncher launcher) {
            if (executionMode == ExecutionMode.PARALLEL) {
                return getRunningLaunchers().size() < executedEntity.getEntity().getMaxConcurrentInstances();
            }
            return getRunningLaunchers().isEmpty();
        }

        @Override
        public String getChildrenLauncherStatus(int consoleWidth) {
            return super.getChildrenLauncherStatus(consoleWidth);
        }

        @Override
        protected void schedule() {
            try {
                Thread.sleep(IWatcher.DF_TIME_OUT_IN_MILLIS);
            } catch (InterruptedException e) {
                LogUtil.logError(e);
            }
            super.schedule();
        }

        @Override
        public void addLauncher(ILauncher subLauncher) {
            addNewLauncher((SubLauncher) subLauncher);
            super.addLauncher(subLauncher);
        }
    }

    private void addNewLauncher(SubLauncher subLauncher) {
        ReportableLauncher subReportableLauncher = (ReportableLauncher) subLauncher;

        if (this.subLaunchers.contains(subReportableLauncher)) {
            return;
        }
        this.subLaunchers.add(subReportableLauncher);
        subReportableLauncher.addListener(this);
        subReportableLauncher.setParentLauncher(this);

        ILauncherResult subLauncherResult = subLauncher.getResult();
        LauncherResult newResult = new LauncherResult(
                result.getTotalTestCases() + subLauncherResult.getTotalTestCases());
        newResult.setNumPasses(result.getNumPasses() + subLauncherResult.getNumPasses());
        newResult.setNumFailures(result.getNumFailures() + subLauncherResult.getNumFailures());
        newResult.setNumIncomplete(result.getNumIncomplete() + subLauncherResult.getNumIncomplete());
        newResult.setNumErrors(result.getNumErrors() + subLauncherResult.getNumErrors());
        result = newResult;

        reportCollection.getReportItemDescriptions()
                .add(ReportItemDescription.from(subReportableLauncher.getReportEntity().getIdForDisplay(),
                        subLauncher.getRunConfigurationDescription()));
        try {
            ReportController.getInstance().updateReportCollection(reportCollection);
        } catch (DALException e) {
            LogUtil.logError(e);
        }
        onNewLauncherAdded();
    }

    protected void onNewLauncherAdded() {
        // Children may override this
    }

    @Override
    public void handleLauncherEvent(LauncherEvent event, LauncherNotifiedObject object) {
        if (event == LauncherEvent.UPDATE_RESULT) {
            TestStatusValue statusValue = (TestStatusValue) object.getObject();
            switch (statusValue) {
                case ERROR:
                    result.increaseErrors();
                    break;
                case FAILED:
                    result.increaseFailures();
                    break;
                case PASSED:
                    result.increasePasses();
                    break;
                default:
                    break;
            }
            onUpdateResult(statusValue);
        }
    }

    protected TestSuiteCollectionExecutionEvent fireTestSuiteExecutionEvent(String eventName) {
        List<TestSuiteExecutionContext> testSuiteContexts = new ArrayList<>();

        for (ReportableLauncher subLauncher : subLaunchers) {
            testSuiteContexts.add(subLauncher.getTestSuiteExecutionContext());
        }

        TestSuiteCollectionExecutionContextImpl executionContext = TestSuiteCollectionExecutionContextImpl.Builder
                .create(getId(), executedEntity.getSourceId())
                .withReportId(reportCollection.getIdForDisplay())
                .withTestSuiteContexts(testSuiteContexts)
                .withProjectLocation(executedEntity.getEntity().getProject().getFolderLocation())
                .withStartTime(startTime != null ? startTime.getTime() : 0L)
                .withEndTime(endTime != null ? endTime.getTime() : 0L)
                .build();
        TestSuiteCollectionExecutionEvent eventObject = new TestSuiteCollectionExecutionEvent(eventName,
                executionContext);
        EventBrokerSingleton.getInstance().getEventBroker().post(eventName, eventObject);

        return eventObject;
    }

    public List<ReportableLauncher> getSubLaunchers() {
        return Collections.unmodifiableList(subLaunchers);
    }
    

    public TestSuiteCollectionExecutedEntity getExecutedEntity() {
        return executedEntity;
    }

    public Date getStartTime() {
        return startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }

}
