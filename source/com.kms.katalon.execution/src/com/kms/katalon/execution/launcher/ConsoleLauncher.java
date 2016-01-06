package com.kms.katalon.execution.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.swt.widgets.Display;

import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.core.logging.LogLevel;
import com.kms.katalon.core.logging.XmlLogRecord;
import com.kms.katalon.core.logging.XmlLogRecordException;
import com.kms.katalon.core.logging.model.ILogRecord;
import com.kms.katalon.core.logging.model.TestCaseLogRecord;
import com.kms.katalon.core.logging.model.TestStatus.TestStatusValue;
import com.kms.katalon.core.logging.model.TestSuiteLogRecord;
import com.kms.katalon.core.reporting.ReportUtil;
import com.kms.katalon.core.util.PathUtil;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.entity.testcase.TestCaseEntity;
import com.kms.katalon.entity.testsuite.TestSuiteEntity;
import com.kms.katalon.execution.configuration.AbstractRunConfiguration;
import com.kms.katalon.execution.configuration.IRunConfiguration;
import com.kms.katalon.execution.constants.StringConstants;
import com.kms.katalon.execution.entity.TestSuiteExecutedEntity;
import com.kms.katalon.execution.launcher.manager.ConsoleMain;
import com.kms.katalon.execution.launcher.manager.LauncherManager;
import com.kms.katalon.execution.launcher.model.LaunchMode;
import com.kms.katalon.execution.logging.ConsoleLogFileWatcher;
import com.kms.katalon.execution.logging.LogExceptionFilter;
import com.kms.katalon.execution.util.ExecutionUtil;
import com.kms.katalon.groovy.util.GroovyUtil;

public class ConsoleLauncher extends AbstractLauncher {
    public ConsoleLauncher(IRunConfiguration runConfig) {
        super(runConfig);
    }

    public void launch(TestSuiteEntity testSuite, TestSuiteExecutedEntity testSuiteExecutedEntity, int reRunTime,
            List<String> passedTestCaseIds) throws Exception {
        if (testSuite != null) {
            executedEntity = testSuite;
            this.testSuiteExecutedEntity = testSuiteExecutedEntity;
            ExecutionUtil.writeRunConfigToFile(getRunConfiguration());
            scriptFile = generateTempTestSuiteScript(testSuite, runConfig, testSuiteExecutedEntity);
            this.reRunTime = reRunTime;
            this.passedTestCaseIds = passedTestCaseIds;
            LauncherManager.getInstance().addLauncher(this);
        }
    }

    private CustomGroovyScriptLaunchShortcut executeScript(ProjectEntity project, IFile testSuiteScript)
            throws Exception {
        System.out.println(MessageFormat.format(StringConstants.LAU_PRT_LAUNCHING_X, getDisplayID()));
        CustomGroovyScriptLaunchShortcut launchShortcut = getLauncher();
        launchShortcut.launch(scriptFile, project, LaunchMode.RUN);
        String name = FilenameUtils.getBaseName(scriptFile.getName());

        while (launch == null) {
            for (ILaunch launch : CustomGroovyScriptLaunchShortcut.getLaunchManager().getLaunches()) {
                if (launch.getLaunchConfiguration() != null) {
                    if (launch.getLaunchConfiguration().getName().equals(name)) {
                        this.launch = launch;
                    }
                }
            }
        }
        return launchShortcut;
    }

    public String getDisplayID() throws Exception {
        return ((TestSuiteEntity) executedEntity).getIdForDisplay() + " - " + runConfig.getName();
    }

    private void terminateProcess() throws DebugException {
        if (launch.getProcesses() != null && launch.getProcesses().length > 0) {
            RuntimeProcess process = (RuntimeProcess) launch.getProcesses()[0];
            if (process.canTerminate()) {
                process.terminate();
            }
        }
    }

    private void handleExecutionEvents(final File logFile, final TestSuiteEntity testSuite, final IFile scriptFile)
            throws FileNotFoundException {
        logRecords = new ArrayList<XmlLogRecord>();

        final Thread threadWatcher = new Thread(new ConsoleLogFileWatcher(logFile, 1, this));
        threadWatcher.start();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean terminated = false;
                while (true) {
                    if (stopSignal && !terminated) {
                        try {
                            terminateProcess();
                            terminated = true;
                        } catch (DebugException e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    if (launch.getProcesses() != null && launch.getProcesses().length > 0
                            && launch.getProcesses()[0].isTerminated()) {
                        terminated = true;
                    }

                    if (terminated) {
                        try {
                            long currentTime = System.currentTimeMillis();
                            while (System.currentTimeMillis() - currentTime < 30000) {
                                try {
                                    updateLastRun(testSuite, logFile);
                                    break;
                                } catch (Exception e) {
                                    // Concurrency modifier exception
                                }
                            }

                            System.out.println(MessageFormat.format(StringConstants.LAU_PRT_X_DONE, getDisplayID(),
                                    launcherResult.toString()));

                            // For report summary
                            prepareReport(testSuite, logFile);

                            retryIfNecessary(testSuite, logFile);

                            stopAndSchedule();

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            }

        });
        thread.start();
    }

    private static void collectPassedTestCaseIds(TestSuiteLogRecord testSuiteRecord, List<String> passedTestCaseIds) {
        if (passedTestCaseIds == null) {
            return;
        }
        for (ILogRecord childLogRecord : testSuiteRecord.getChildRecords()) {
            if (childLogRecord instanceof TestCaseLogRecord
                    && childLogRecord.getStatus().getStatusValue() == TestStatusValue.PASSED) {
                passedTestCaseIds.add(childLogRecord.getName());
            }
        }
    }

    private void retryIfNecessary(final TestSuiteEntity testSuite, File logFile) throws Exception {
        File testSuiteReportSourceFolder = logFile.getParentFile();
        if (testSuiteReportSourceFolder == null) {
            return;
        }
        TestSuiteLogRecord suiteLog = ReportUtil.generate(testSuiteReportSourceFolder.getAbsolutePath());
        if (suiteLog != null && suiteLog.getStatus().getStatusValue() != TestStatusValue.PASSED
                && reRunTime < testSuite.getNumberOfRerun() && runConfig instanceof AbstractRunConfiguration) {
            System.out.println("Re-run test suite #" + (reRunTime + 1));
            final AbstractRunConfiguration abstractRunConfiguration = (AbstractRunConfiguration) runConfig;
            abstractRunConfiguration.generateLogFolder(testSuite);
            abstractRunConfiguration.generateLogFilePath(testSuite);
            if (testSuite.isRerunFailedTestCasesOnly()) {
                collectPassedTestCaseIds(suiteLog, passedTestCaseIds);
            }
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    try {
                        ConsoleMain.launchTestSuite(testSuite, abstractRunConfiguration,
                                testSuiteExecutedEntity.getReportFolderPath(), reRunTime + 1, passedTestCaseIds);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private boolean prepareReport(TestSuiteEntity testSuite, File logFile) {
        if (testSuite != null) {
            try {
                File testSuiteReportSourceFolder = logFile.getParentFile();
                File htmlFile = new File(testSuiteReportSourceFolder,
                        FilenameUtils.getBaseName(testSuiteReportSourceFolder.getName()) + ".html");
                TestSuiteLogRecord suiteLog = ReportUtil.generate(testSuiteReportSourceFolder.getAbsolutePath());
                // Generate HTML file if it does not exist.
                if (!htmlFile.exists()) {
                    ReportUtil.writeLogRecordToFiles(suiteLog, testSuiteReportSourceFolder);
                }

                System.out.println(StringConstants.LAU_PRT_SENDING_RPT_TO_INTEGRATING_PRODUCTS);
                uploadReportToIntegratingProduct(suiteLog);
                System.out.println(StringConstants.LAU_PRT_REPORT_SENT);

                // report folder that is set by user.
                File userReportFolder = getUserReportFolder(testSuite);

                if (userReportFolder != null && htmlFile.exists()) {
                    System.out.println(StringConstants.LAU_PRT_COPYING_RPT_TO_USR_RPT_FOLDER);
                    System.out.println(MessageFormat.format(StringConstants.LAU_PRT_USR_REPORT_FOLDER_X,
                            userReportFolder.getAbsolutePath()));
                    System.out.println(StringConstants.LAU_PRT_CLEANING_USR_RPT_FOLDER);

                    cleanUserReportFolder(testSuite);

                    for (File reportChildSourceFile : testSuiteReportSourceFolder.listFiles()) {
                        String fileName = FilenameUtils.getBaseName(reportChildSourceFile.getName());
                        String fileExtension = FilenameUtils.getExtension(reportChildSourceFile.getName());

                        // ignore LOCK file
                        if (fileExtension.equalsIgnoreCase("lck")) continue;

                        // Rename .csv, .log and .html file to user's format
                        if ((ConsoleMain.getReportFileName() != null)
                                && (fileExtension.equals("csv") || fileExtension.equals("log") || fileExtension
                                        .equals("html"))) {
                            fileName = ConsoleMain.getReportFileName();
                        }

                        // Copy child file to user's report folder
                        FileUtils.copyFile(reportChildSourceFile, new File(userReportFolder, fileName + "."
                                + fileExtension));
                    }
                    System.out.println(StringConstants.LAU_PRT_REPORT_COPIED);
                }
            } catch (Exception ex) {
                System.out.println(MessageFormat.format(StringConstants.LAU_PRT_CANNOT_CREATE_REPORT_FOLDER,
                        ex.getMessage()));
            }

            try {
                File testSuiteReportSourceFolder = logFile.getParentFile();

                File csvFile = new File(testSuiteReportSourceFolder,
                        FilenameUtils.getBaseName(testSuiteReportSourceFolder.getName()) + ".csv");

                List<String> csvReports = new ArrayList<String>();

                csvReports.add(csvFile.getAbsolutePath());

                List<Object[]> suitesSummaryForEmail = collectSummaryData(csvReports);

                sendReportEmail(testSuite, null, logFile, suitesSummaryForEmail);

                return true;
            } catch (Exception e) {
                System.out.println(MessageFormat.format(StringConstants.LAU_PRT_CANNOT_SEND_EMAIL, e.getMessage()));
            }
        }
        return false;
    }

    private void cleanUserReportFolder(TestSuiteEntity testSuite) throws IOException {
        FileUtils.cleanDirectory(getUserReportFolder(testSuite));
    }

    private File getUserReportFolder(TestSuiteEntity testSuite) {
        if (testSuiteExecutedEntity.getReportFolderPath() == null) return null;
        try {
            File reportFolder = new File(PathUtil.relativeToAbsolutePath(testSuiteExecutedEntity.getReportFolderPath(),
                    testSuite.getProject().getFolderLocation()));

            if (reportFolder != null && !reportFolder.exists()) {
                reportFolder.mkdirs();
            }
            return reportFolder;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void execute() {
        try {
            executeScript(ProjectController.getInstance().getCurrentProject(), scriptFile);
            handleExecutionEvents(getCurrentLogFile(), (TestSuiteEntity) executedEntity, scriptFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(StringConstants.LAU_PRT_CANNOT_EXECUTE_TEST_SUITE);
        }
    }

    @Override
    public List<XmlLogRecord> getAllRecords() {
        return logRecords;
    }

    /**
     * Only print line number of the failed step. Users don't want to see the test case that the line number belongs to.
     * 
     * @author Tuan Nguyen Manh
     * @param record
     * @throws Exception
     */
    private synchronized void printErrorLineLogs(XmlLogRecord record) throws Exception {

        if ((record.getLevel() == LogLevel.FAILED || record.getLevel() == LogLevel.ERROR)
                && record.getExceptions() != null) {
            for (XmlLogRecordException logRecordException : record.getExceptions()) {
                if (!LogExceptionFilter.isTraceableException(logRecordException)) continue;
                if (LogExceptionFilter.isTestCaseScript(logRecordException.getClassName())) {
                    TestCaseEntity testCase = LogExceptionFilter.getTestCaseByLogException(logRecordException);
                    if (testCase != null) {
                        System.out.println(record);
                        System.err.println(MessageFormat.format(StringConstants.LAU_PRT_X_FAILED_AT_LINE_Y,
                                testCase.getIdForDisplay(), logRecordException.getLineNumber()));
                        continue;
                    }
                }
                System.out.println(record);
                System.err.println(MessageFormat.format(StringConstants.LAU_PRT_FAILED_AT_LINE_X,
                        logRecordException.toString()));
                break;
            }
        } else {
            System.out.println(record);
        }
    }

    @Override
    public void addRecords(List<XmlLogRecord> records) {
        synchronized (this) {
            try {
                for (XmlLogRecord record : records) {
                    if (logDepth == 2) {
                        logRecords.add(record);
                    }
                    printErrorLineLogs(record);

                    if (record.getLevel().equals(LogLevel.END)) {
                        logDepth--;
                        if (record.getSourceMethodName().equals(
                                com.kms.katalon.core.constants.StringConstants.LOG_END_TEST_METHOD)) {
                            if (logDepth == 0 || logDepth == 1) {
                                XmlLogRecord resultRecord = logRecords.get(logRecords.size() - 2);
                                if (resultRecord.getLevel() == LogLevel.PASSED) {
                                    launcherResult.increasePasses();
                                } else {
                                    if (resultRecord.getLevel() == LogLevel.FAILED) {
                                        launcherResult.increaseFailures();
                                    } else if (resultRecord.getLevel() == LogLevel.ERROR) {
                                        launcherResult.increaseErrors();
                                    }
                                }

                                logRecords.clear();
                            }
                        }

                        if (logDepth == 0) {
                            stopSignal = true;
                        }

                    } else if (record.getLevel().equals(LogLevel.START)) {
                        logDepth++;
                    } else if (record.getLevel().equals(LogLevel.ERROR)) {
                        if (logDepth == 1) {
                            launcherResult.increaseErrors();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    protected void deleteScriptFile() {
        try {
            scriptFile.delete(true, null);
            IFolder libFolder = GroovyUtil.getCustomKeywordLibFolder(ProjectController.getInstance()
                    .getCurrentProject());
            libFolder.refreshLocal(IResource.DEPTH_ONE, null);
        } catch (Exception e) {
            System.out.println(StringConstants.LAU_PRT_CANNOT_CLEAN_TEMP_FILES);
        }
    }
}
