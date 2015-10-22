package com.kms.katalon.composer.integration.qtest.preference;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.integration.qtest.QTestIntegrationUtil;
import com.kms.katalon.composer.integration.qtest.constant.StringConstants;
import com.kms.katalon.composer.integration.qtest.dialog.TestSuiteRepoDialog;
import com.kms.katalon.composer.integration.qtest.model.TestSuiteRepo;
import com.kms.katalon.composer.integration.qtest.preference.provider.TestSuiteRepoTableLabelProvider;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.integration.IntegratedEntity;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.integration.qtest.QTestIntegrationProjectManager;
import com.kms.katalon.integration.qtest.entity.QTestProject;

public class TestSuiteRepoPreferencePage extends AbstractQTestIntegrationPage {

    private Composite container;
    private TableViewer tableViewer;
    private Button btnAdd;
    private Button btnEdit;
    private Button btnRemove;
    private List<QTestProject> qTestProjects;
    private List<TestSuiteRepo> testSuiteRepositories;

    @Override
    protected Control createContents(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        Composite compositeTable = new Composite(container, SWT.NONE);
        compositeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = tableViewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        TableViewerColumn tableViewerColumnQTestProject = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tblclmnQTestProject = tableViewerColumnQTestProject.getColumn();
        tblclmnQTestProject.setText(StringConstants.DIA_TITLE_QTEST_PROJECT);

        TableViewerColumn tableViewerColumnKatalonFolder = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tblclmnKatalonFolder = tableViewerColumnKatalonFolder.getColumn();
        tblclmnKatalonFolder.setText(StringConstants.DIA_TITLE_KATALON_FOLDER);

        tableViewer.setLabelProvider(new TestSuiteRepoTableLabelProvider());
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        TableColumnLayout tableLayout = new TableColumnLayout();
        tableLayout.setColumnData(tblclmnQTestProject, new ColumnWeightData(0, 100));
        tableLayout.setColumnData(tblclmnKatalonFolder, new ColumnWeightData(90, 100));
        compositeTable.setLayout(tableLayout);

        Composite compositeButton = new Composite(container, SWT.NONE);
        compositeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, true, 1, 1));
        GridLayout glCompositeButton = new GridLayout(1, false);
        glCompositeButton.marginHeight = 0;
        compositeButton.setLayout(glCompositeButton);

        btnAdd = new Button(compositeButton, SWT.NONE);
        btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAdd.setText(StringConstants.ADD);

        btnEdit = new Button(compositeButton, SWT.NONE);
        btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnEdit.setText(StringConstants.EDIT);
        btnEdit.setEnabled(false);

        btnRemove = new Button(compositeButton, SWT.NONE);
        btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnRemove.setText(StringConstants.REMOVE);
        btnRemove.setEnabled(false);

        addButtonSelectionListeners();
        initialize();

        return container;
    }

    @Override
    protected void initialize() {
        if (container == null || container.isDisposed()) {
            return;
        }
        
        ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();
        IntegratedEntity integratedProjectEntity = QTestIntegrationUtil.getIntegratedEntity(projectEntity);

        try {
            if (integratedProjectEntity != null) {
                qTestProjects = QTestIntegrationProjectManager
                        .getQTestProjectsByIntegratedEntity(integratedProjectEntity);
            } else {
                qTestProjects = new ArrayList<QTestProject>();
            }
        } catch (Exception ex) {
            LoggerSingleton.logError(ex);
            MessageDialog.openWarning(null, StringConstants.DIA_MSG_UNABLE_GET_PROJECT_INFO, ex.getMessage());
            return;
        }

        testSuiteRepositories = QTestIntegrationUtil.getTestSuiteRepositories(projectEntity, qTestProjects);

        tableViewer.setInput(testSuiteRepositories);
    }

    private void addButtonSelectionListeners() {
        btnAdd.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewTestSuiteRepo();
            }
        });

        btnEdit.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                editTestSuiteRepo();
            }
        });

        btnRemove.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                removeTestSuiteRepo();
            }
        });

        tableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                if (selection == null || selection.isEmpty()) {
                    btnEdit.setEnabled(false);
                    btnRemove.setEnabled(false);
                } else {
                    btnEdit.setEnabled(true);
                    btnRemove.setEnabled(true);
                }

            }
        });
    }

    @Focus
    public void focus() {
        initialize();
    }

    protected void removeTestSuiteRepo() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection == null || selection.isEmpty()) return;

        final TestSuiteRepo repo = (TestSuiteRepo) selection.getFirstElement();
        testSuiteRepositories.remove(repo);

        tableViewer.refresh();
    }

    private void insertNewRepoToTable(int index, TestSuiteRepo newRepo) {
        testSuiteRepositories.remove(index);
        if (index >= testSuiteRepositories.size()) {
            testSuiteRepositories.add(newRepo);
        } else {
            testSuiteRepositories.add(index, newRepo);
        }
        tableViewer.refresh();
    }

    protected void editTestSuiteRepo() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection == null || selection.isEmpty()) return;

        final TestSuiteRepo repo = (TestSuiteRepo) selection.getFirstElement();
        List<String> currentFolderIds = getRegisteredFolderIds();
        currentFolderIds.remove(repo.getFolderId());
        TestSuiteRepoDialog dialog = new TestSuiteRepoDialog(btnAdd.getDisplay().getActiveShell(), qTestProjects,
                currentFolderIds, repo);
        if (dialog.open() == Dialog.OK) {
            TestSuiteRepo newRepo = dialog.getTestSuiteRepo();
            final int index = testSuiteRepositories.indexOf(repo);
            if (!repo.equals(newRepo)) {
                insertNewRepoToTable(index, newRepo);
            }
        }
    }

    private List<String> getRegisteredFolderIds() {
        List<String> currentFolderIds = new ArrayList<String>();
        for (TestSuiteRepo testSuiteRepo : testSuiteRepositories) {
            currentFolderIds.add(testSuiteRepo.getFolderId());
        }

        return currentFolderIds;
    }

    protected void addNewTestSuiteRepo() {
        List<String> currentFolderIds = getRegisteredFolderIds();

        TestSuiteRepoDialog dialog = new TestSuiteRepoDialog(btnAdd.getDisplay().getActiveShell(), qTestProjects,
                currentFolderIds, null);
        if (dialog.open() == Dialog.OK) {
            TestSuiteRepo repo = dialog.getTestSuiteRepo();
            testSuiteRepositories.add(repo);

            qTestProjects.clear();
            for (QTestProject qTestProject : dialog.getQTestProjectsMap().values()) {
                if (qTestProject.equals(repo.getQTestProject())) {
                    qTestProject.getTestSuiteFolderIds().add(repo.getFolderId());
                }

                qTestProjects.add(qTestProject);
            }
            tableViewer.refresh();
        }
    }

    @Override
    public boolean performOk() {
        // if it never be opened, just returns to the parent class
        if (container == null) return true;

        ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();
        
        // Sync with the current project
        Set<QTestProject> currentProjects = new LinkedHashSet<QTestProject>();
        IntegratedEntity projectIntegratedEntity = QTestIntegrationUtil.getIntegratedEntity(projectEntity);
        
        if (projectIntegratedEntity != null) {
            currentProjects.addAll(QTestIntegrationProjectManager
                    .getQTestProjectsByIntegratedEntity(projectIntegratedEntity));
        }
        currentProjects.addAll(qTestProjects);

        for (QTestProject qTestProject : currentProjects) {
            qTestProject.getTestSuiteFolderIds().clear();
        }

        for (TestSuiteRepo repo : testSuiteRepositories) {
            for (QTestProject qTestProject : currentProjects) {
                if (repo.getQTestProject().equals(qTestProject)) {
                    qTestProject.getTestSuiteFolderIds().add(repo.getFolderId());
                }
            }
        }

        qTestProjects.clear();
        qTestProjects.addAll(currentProjects);

        saveProject(projectEntity);
        return true;
    }

    private void saveProject(ProjectEntity projectEntity) {
        IntegratedEntity projectNewIntegratedEntity = QTestIntegrationProjectManager
                .getIntegratedEntityByQTestProjects(qTestProjects);

        ProjectEntity currentProject = (ProjectEntity) QTestIntegrationUtil.updateFileIntegratedEntity(projectEntity,
                projectNewIntegratedEntity);

        try {
            ProjectController.getInstance().updateProject(currentProject);
        } catch (Exception e) {
            LoggerSingleton.logError(e);
        }
    }

    @Override
    protected void performDefaults() {
        initialize();
    }
}
