package com.kms.katalon.composer.integration.qtest.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.composer.components.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.integration.qtest.QTestIntegrationUtil;
import com.kms.katalon.composer.integration.qtest.dialog.TestCaseRepoDialog;
import com.kms.katalon.composer.integration.qtest.jobs.DisintegrateTestCaseJob;
import com.kms.katalon.composer.integration.qtest.jobs.listeners.DisintegrateJobListener;
import com.kms.katalon.composer.integration.qtest.model.TestCaseRepo;
import com.kms.katalon.composer.integration.qtest.preferences.providers.TestCaseRepoTableLabelProvider;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.file.IntegratedFileEntity;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.integration.IntegratedEntity;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.integration.qtest.QTestIntegrationFolderManager;
import com.kms.katalon.integration.qtest.QTestIntegrationProjectManager;
import com.kms.katalon.integration.qtest.constants.QTestStringConstants;
import com.kms.katalon.integration.qtest.entity.QTestModule;
import com.kms.katalon.integration.qtest.entity.QTestProject;

public class TestCaseRepoPreferencePage extends PreferencePage implements EventHandler {

	private IEventBroker eventBroker;

	@Inject
	UISynchronize sync;

	private Composite container;
	private Table table;
	private Button btnAdd, btnEdit, btnRemove;
	private TableViewer tableViewer;
	private List<QTestProject> qTestProjects;
	private List<TestCaseRepo> testCaseRepositories;

	private static boolean needToInitialize;

	public TestCaseRepoPreferencePage() {
		eventBroker = EventBrokerSingleton.getInstance().getEventBroker();
		needToInitialize = true;
		registerEventListeners();
	}

	private void registerEventListeners() {
		eventBroker.subscribe(EventConstants.PROJECT_UPDATED, this);
	}

	@Override
	protected Control createContents(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Composite compositeTable = new Composite(container, SWT.NONE);
		compositeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION);
		table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableViewerColumn tableViewerColumnQTestProject = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnQTestProject = tableViewerColumnQTestProject.getColumn();
		tblclmnQTestProject.setText("qTest Project");

		TableViewerColumn tableViewerColumnQTestModule = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnQTestModule = tableViewerColumnQTestModule.getColumn();
		tblclmnQTestModule.setText("qTest Module");

		TableViewerColumn tableViewerColumnKatalonFolder = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnKatalonFolder = tableViewerColumnKatalonFolder.getColumn();
		tblclmnKatalonFolder.setText("Katalon Folder");

		tableViewer.setLabelProvider(new TestCaseRepoTableLabelProvider());
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(tblclmnQTestProject, new ColumnWeightData(0, 100));
		tableLayout.setColumnData(tblclmnQTestModule, new ColumnWeightData(40, 100));
		tableLayout.setColumnData(tblclmnKatalonFolder, new ColumnWeightData(40, 100));
		compositeTable.setLayout(tableLayout);

		Composite compositeButton = new Composite(container, SWT.NONE);
		compositeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, true, 1, 1));
		GridLayout gl_compositeButton = new GridLayout(1, false);
		gl_compositeButton.marginHeight = 0;
		compositeButton.setLayout(gl_compositeButton);

		btnAdd = new Button(compositeButton, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAdd.setText("Add");

		btnEdit = new Button(compositeButton, SWT.NONE);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnEdit.setText("Edit");
		btnEdit.setEnabled(false);

		btnRemove = new Button(compositeButton, SWT.NONE);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnRemove.setText("Remove");
		btnRemove.setEnabled(false);

		addButtonSelectionListeners();
		initilize();

		return container;
	}

	private void initilize() {
		if (!needToInitialize || container == null || container.isDisposed()) return;

		ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();
		IntegratedEntity integratedProjectEntity = projectEntity.getIntegratedEntity(QTestStringConstants.PRODUCT_NAME);

		try {
			if (integratedProjectEntity != null) {
				qTestProjects = QTestIntegrationProjectManager
						.getQTestProjectsByIntegratedEntity(integratedProjectEntity);
			} else {
				qTestProjects = new ArrayList<QTestProject>();
			}
		} catch (Exception ex) {
			LoggerSingleton.logError(ex);
			MessageDialog.openWarning(null, "Unable to get qTest projects's information.", ex.getMessage());
			return;
		}

		testCaseRepositories = QTestIntegrationUtil.getTestCaseRepositories(projectEntity, qTestProjects);

		tableViewer.setInput(testCaseRepositories);
	}

	private void addButtonSelectionListeners() {
		btnAdd.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				addNewTestCaseRepo();
			}
		});

		btnEdit.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				editTestCaseRepo();
			}
		});

		btnRemove.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				removeTestCaseRepo();
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

	private void addNewTestCaseRepo() {
		List<String> currentFolderIds = getRegisteredFolderIds();

		TestCaseRepoDialog dialog = new TestCaseRepoDialog(btnAdd.getDisplay().getActiveShell(), qTestProjects,
				currentFolderIds, null);
		if (dialog.open() == Dialog.OK) {
			TestCaseRepo repo = dialog.getTestCaseRepo();
			testCaseRepositories.add(repo);

			qTestProjects.clear();
			for (QTestProject qTestProject : dialog.getQTestProjectsMap().values()) {
				if (qTestProject.equals(repo.getQTestProject())) {
					qTestProject.getTestCaseFolderIds().add(repo.getFolderId());
				}

				qTestProjects.add(qTestProject);
			}
			tableViewer.refresh();
		}
	}

	private List<String> getRegisteredFolderIds() {
		List<String> currentFolderIds = new ArrayList<String>();
		for (TestCaseRepo testCaseRepo : testCaseRepositories) {
			currentFolderIds.add(testCaseRepo.getFolderId());
		}

		return currentFolderIds;
	}

	private void editTestCaseRepo() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (selection == null || selection.isEmpty()) return;

		final TestCaseRepo repo = (TestCaseRepo) selection.getFirstElement();
		List<String> currentFolderIds = getRegisteredFolderIds();
		currentFolderIds.remove(repo.getFolderId());

		TestCaseRepoDialog dialog = new TestCaseRepoDialog(btnAdd.getDisplay().getActiveShell(), qTestProjects,
				currentFolderIds, repo);
		if (dialog.open() == Dialog.OK) {
			final TestCaseRepo newRepo = dialog.getTestCaseRepo();
			if (!repo.equals(newRepo)) {
				final int index = testCaseRepositories.indexOf(repo);
				String folderId = repo.getFolderId();
				ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();
				try {
					FolderEntity folderEntity = FolderController.getInstance().getFolderByDisplayId(projectEntity,
							folderId);

					if (folderEntity == null) insertNewRepoToTable(index, newRepo);

					IntegratedEntity folderIntegratedEntity = folderEntity
							.getIntegratedEntity(QTestStringConstants.PRODUCT_NAME);

					if (folderIntegratedEntity == null) insertNewRepoToTable(index, newRepo);

					if (confirmRemoveRepo()) {
						performInsertTestCaseRepor(folderEntity, newRepo, index);
					}
				} catch (Exception e) {
					LoggerSingleton.logError(e);
					MultiStatusErrorDialog.showErrorDialog(e, "Unable to modify Test Case Repository.", e.getClass()
							.getSimpleName());
				}
			}
		}
	}

	private void removeRepoFromTable(TestCaseRepo repo) {
		if (testCaseRepositories.contains(repo)) {
			testCaseRepositories.remove(repo);
		}
		tableViewer.refresh();
	}

	private void insertNewRepoToTable(int index, TestCaseRepo newRepo) {
		testCaseRepositories.remove(index);
		if (index >= testCaseRepositories.size()) {
			testCaseRepositories.add(newRepo);
		} else {
			testCaseRepositories.add(index, newRepo);
		}
		tableViewer.refresh();
	}

	private boolean confirmRemoveRepo() {
		if (MessageDialog.openConfirm(null, "Confirmation",
				"Are you sure you want to disintegrate all test cases in this folder with qTest?")) {
			return true;
		} else {
			return false;
		}
	}

	private void removeTestCaseRepo() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (selection == null || selection.isEmpty()) return;

		final TestCaseRepo repo = (TestCaseRepo) selection.getFirstElement();

		String folderId = repo.getFolderId();
		ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();
		try {
			FolderEntity folderEntity = FolderController.getInstance().getFolderByDisplayId(projectEntity, folderId);

			if (folderEntity == null) removeRepoFromTable(repo);

			IntegratedEntity folderIntegratedEntity = folderEntity.getIntegratedEntity(QTestStringConstants.PRODUCT_NAME);

			if (folderIntegratedEntity == null) removeRepoFromTable(repo);

			if (confirmRemoveRepo()) {
				performRemoveTestCaseRepo(folderEntity, repo);
			}

		} catch (Exception e) {
			LoggerSingleton.logError(e);
			MultiStatusErrorDialog.showErrorDialog(e, "Unable to remove Test Case Repository.", e.getClass()
					.getSimpleName());
		}
	}

	private void performRemoveTestCaseRepo(final FolderEntity folderEntity, final TestCaseRepo repo) {
		DisintegrateTestCaseJob job = new DisintegrateTestCaseJob("Disintegrate test cases");
		job.setFileEntities(Arrays.asList((IntegratedFileEntity) folderEntity));
		job.doTask();
		job.addJobChangeListener(new DisintegrateJobListener() {
			@Override
			public void done(IJobChangeEvent event) {
				sync.syncExec(new Runnable() {
					@Override
					public void run() {
						removeRepoFromTable(repo);
						return;
					}
				});
			}
		});
	}
	
	private void performInsertTestCaseRepor(final FolderEntity folderEntity, final TestCaseRepo newRepo, final int index) {
		DisintegrateTestCaseJob job = new DisintegrateTestCaseJob("Disintegrate test cases");
		job.setFileEntities(Arrays.asList((IntegratedFileEntity) folderEntity));
		job.doTask();
		job.addJobChangeListener(new DisintegrateJobListener() {
			@Override
			public void done(IJobChangeEvent event) {
				sync.syncExec(new Runnable() {
					@Override
					public void run() {
						insertNewRepoToTable(index, newRepo);
						return;
					}
				});
			}
		});
	}

	@Override
	public boolean performOk() {
		// if it never be opened, just returns to the parent class
		if (container == null) return true;

		ProjectEntity projectEntity = ProjectController.getInstance().getCurrentProject();

		for (QTestProject qTestProject : qTestProjects) {
			qTestProject.getTestCaseFolderIds().clear();
		}

		for (TestCaseRepo repo : testCaseRepositories) {
			for (QTestProject qTestProject : qTestProjects) {
				if (repo.getQTestProject().equals(qTestProject)) {
					qTestProject.getTestCaseFolderIds().add(repo.getFolderId());
				}
			}

			// update integrated entity of folderEntity
			try {
				FolderEntity folderEntity = FolderController.getInstance().getFolderByDisplayId(projectEntity,
						repo.getFolderId());
				if (folderEntity != null) {
					saveFolder(folderEntity, repo.getQTestModule());
				}
			} catch (Exception e) {
				LoggerSingleton.logError(e);
				continue;
			}
		}

		saveProject(projectEntity);
		return true;
	}

	private void saveFolder(FolderEntity folderEntity, QTestModule qTestModule) {
		IntegratedEntity folderNewIntegratedEntity = QTestIntegrationFolderManager
				.getFolderIntegratedEntityByQTestModule(qTestModule);

		folderEntity = (FolderEntity) updateFileIntegratedEntity(folderEntity, folderNewIntegratedEntity);

		try {
			FolderController.getInstance().saveFolder(folderEntity);
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
	}

	private IntegratedFileEntity updateFileIntegratedEntity(IntegratedFileEntity entity, IntegratedEntity newIntegrated) {
		IntegratedEntity oldIntegrated = entity.getIntegratedEntity(QTestStringConstants.PRODUCT_NAME);

		// Otherwise, add the new one to integrated list
		int index = 0;
		if (oldIntegrated == null) {
			index = entity.getIntegratedEntities().size();
		} else {
			index = entity.getIntegratedEntities().indexOf(oldIntegrated);
			entity.getIntegratedEntities().remove(index);
		}

		if (index >= entity.getIntegratedEntities().size()) {
			entity.getIntegratedEntities().add(newIntegrated);
		} else {
			entity.getIntegratedEntities().add(index, oldIntegrated);
		}

		return entity;
	}

	private void saveProject(ProjectEntity projectEntity) {
		IntegratedEntity projectNewIntegratedEntity = QTestIntegrationProjectManager
				.getIntegratedEntityByQTestProjects(qTestProjects);

		ProjectEntity currentProject = (ProjectEntity) updateFileIntegratedEntity(projectEntity,
				projectNewIntegratedEntity);

		try {
			needToInitialize = false;
			ProjectController.getInstance().updateProject(currentProject);
			eventBroker.send(EventConstants.PROJECT_UPDATED, null);
			needToInitialize = true;
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}

	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTopic().equals(EventConstants.PROJECT_UPDATED)) {
			initilize();
		}
	}

	@Override
	protected void performDefaults() {
		initilize();
	}

}
