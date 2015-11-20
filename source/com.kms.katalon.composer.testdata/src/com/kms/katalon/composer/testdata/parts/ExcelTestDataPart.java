package com.kms.katalon.composer.testdata.parts;

import java.text.MessageFormat;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.composer.components.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.impl.control.ImageButton;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.testdata.constants.ImageConstants;
import com.kms.katalon.composer.testdata.constants.StringConstants;
import com.kms.katalon.composer.testdata.job.LoadExcelFileJob;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.controller.TestDataController;
import com.kms.katalon.core.testdata.ExcelData;
import com.kms.katalon.core.util.PathUtils;
import com.kms.katalon.entity.dal.exception.DuplicatedFileNameException;
import com.kms.katalon.entity.testdata.DataFileEntity;
import com.kms.katalon.entity.testdata.DataFileEntity.DataFileDriverType;
import com.kms.katalon.entity.testdata.DataFilePropertyInputEntity;

public class ExcelTestDataPart extends TestDataMainPart {
    private static final String[] FILTER_NAMES = { "Microsoft Excel Spreadsheet Files (*.xls, *.xlsx)" };

    private static final String[] FILTER_EXTS = { "*.xlsx; *.xls" };

    @Inject
    private EPartService partService;

    @Inject
    private UISynchronize sync;

    private Text txtFileName;
    private Combo cbbSheets;
    private TableViewer tableViewer;
    private Label lblSheetName;
    private Button ckcbUseRelativePath;
    private Button btnBrowse;
    private ImageButton btnExpandFileInfo;
    private Composite compositeFileInfoDetails;
    private Composite compositeFileInfoHeader;
    private Composite compositeTable;
    private Composite compositeFileInfo;

    // Control status
    private boolean isFileInfoExpanded;
    private boolean ableToReload;

    // Field
    private String fCurrentPath;
    private String fCurrentSheetName;
    private String[][] fData;

    private LoadExcelFileJob loadFileJob;

    private Listener layoutFileInfoCompositeListener = new Listener() {

        @Override
        public void handleEvent(org.eclipse.swt.widgets.Event event) {
            layoutFileInfoComposite();
        }
    };

    private Label lblFileInfo;

    @PostConstruct
    public void createControls(Composite parent, MPart mpart) {
        ableToReload = true;
        super.createControls(parent, mpart);
    }

    protected void layoutFileInfoComposite() {
        Display.getDefault().timerExec(10, new Runnable() {

            @Override
            public void run() {
                isFileInfoExpanded = !isFileInfoExpanded;
                compositeFileInfoDetails.setVisible(isFileInfoExpanded);
                if (!isFileInfoExpanded) {
                    ((GridData) compositeFileInfoDetails.getLayoutData()).exclude = true;
                    compositeFileInfo.setSize(compositeFileInfo.getSize().x, compositeFileInfo.getSize().y
                            - compositeTable.getSize().y);
                } else {
                    ((GridData) compositeFileInfoDetails.getLayoutData()).exclude = false;
                }
                compositeFileInfo.layout(true, true);
                compositeFileInfo.getParent().layout();
                redrawBtnExpandFileInfo();
            }
        });
    }

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public Composite createFileInfoPart(Composite parent) {
        // File info part
        compositeFileInfo = new Composite(parent, SWT.NONE);
        compositeFileInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        GridLayout glFileComposite = new GridLayout(1, true);
        glFileComposite.marginWidth = 0;
        glFileComposite.marginHeight = 0;
        compositeFileInfo.setLayout(glFileComposite);
        compositeFileInfo.setBackground(ColorUtil.getCompositeBackgroundColor());

        compositeFileInfoHeader = new Composite(compositeFileInfo, SWT.NONE);
        compositeFileInfoHeader.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        GridLayout glFileCompositeHeader = new GridLayout(3, false);
        glFileCompositeHeader.marginWidth = 0;
        glFileCompositeHeader.marginHeight = 0;
        compositeFileInfoHeader.setLayout(glFileCompositeHeader);
        compositeFileInfoHeader.setCursor(compositeFileInfoHeader.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        btnExpandFileInfo = new ImageButton(compositeFileInfoHeader, SWT.NONE);

        lblFileInfo = new Label(compositeFileInfoHeader, SWT.NONE);
        lblFileInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        lblFileInfo.setText(StringConstants.PA_LBL_FILE_INFO);
        ControlUtils.setFontToBeBold(lblFileInfo);

        lblFileInfoStatus = new Label(compositeFileInfoHeader, SWT.NONE);
        lblFileInfoStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        lblFileInfoStatus.setForeground(ColorUtil.getWarningForegroudColor());
        ControlUtils.setFontToBeBold(lblFileInfoStatus);

        compositeFileInfoDetails = new Composite(compositeFileInfo, SWT.NONE);
        compositeFileInfoDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout glFileCompositeDetails = new GridLayout(2, true);
        glFileCompositeDetails.marginHeight = 0;
        glFileCompositeDetails.horizontalSpacing = 30;
        glFileCompositeDetails.marginRight = 40;
        glFileCompositeDetails.marginLeft = 40;
        compositeFileInfoDetails.setLayout(glFileCompositeDetails);

        Composite compositeFileName = new Composite(compositeFileInfoDetails, SWT.NONE);
        compositeFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout glCompositeFileName = new GridLayout(3, false);
        glCompositeFileName.marginRight = 5;
        glCompositeFileName.marginWidth = 0;
        compositeFileName.setLayout(glCompositeFileName);

        Label lblFileName = new Label(compositeFileName, SWT.NONE);
        GridData gdLblFileName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblFileName.widthHint = TestDataMainPart.MAX_LABEL_WIDTH;
        lblFileName.setLayoutData(gdLblFileName);
        lblFileName.setText(StringConstants.PA_LBL_FILE_NAME);
        txtFileName = new Text(compositeFileName, SWT.BORDER);
        GridData gdTxtFileName = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdTxtFileName.heightHint = 20;
        txtFileName.setLayoutData(gdTxtFileName);
        txtFileName.setEditable(false);
        btnBrowse = new Button(compositeFileName, SWT.FLAT);
        btnBrowse.setText(StringConstants.PA_BTN_BROWSE);

        Composite compositeSheetName = new Composite(compositeFileInfoDetails, SWT.NONE);
        compositeSheetName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        compositeSheetName.setLayout(new GridLayout(2, false));

        lblSheetName = new Label(compositeSheetName, SWT.NONE);
        lblSheetName.setText(StringConstants.PA_LBL_SHEET_NAME);

        cbbSheets = new Combo(compositeSheetName, SWT.READ_ONLY);
        GridData gdCbbSheets = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gdCbbSheets.heightHint = 20;
        cbbSheets.setLayoutData(gdCbbSheets);

        Composite compositeCheckBoxes = new Composite(compositeFileInfoDetails, SWT.NONE);
        compositeCheckBoxes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        GridLayout glCompositeCheckBoxes = new GridLayout(2, false);
        glCompositeCheckBoxes.horizontalSpacing = 15;
        glCompositeCheckBoxes.marginWidth = 0;
        compositeCheckBoxes.setLayout(glCompositeCheckBoxes);

        ckcbUseRelativePath = new Button(compositeCheckBoxes, SWT.CHECK);
        ckcbUseRelativePath.setText(StringConstants.PA_CHKBOX_USE_RELATIVE_PATH);
        new Label(compositeCheckBoxes, SWT.NONE);

        isFileInfoExpanded = true;
        redrawBtnExpandFileInfo();

        addControlListeners();
        return compositeFileInfo;
    }

    private String getSourceUrlAbsolutePath() {
        String sourceUrl = txtFileName.getText();
        if (ckcbUseRelativePath.getSelection()) {
            sourceUrl = PathUtils.relativeToAbsolutePath(sourceUrl, getProjectFolderLocation());
        }
        return sourceUrl;
    }

    @Override
    public Composite createDataTablePart(Composite parent) {
        parent.setLayout(new GridLayout(1, true));
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        compositeTable = new Composite(parent, SWT.BORDER);
        GridLayout glTableViewerComposite = new GridLayout(1, true);
        glTableViewerComposite.marginWidth = 0;
        glTableViewerComposite.marginHeight = 0;
        compositeTable.setLayout(glTableViewerComposite);
        compositeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tableViewer = new TableViewer(compositeTable, SWT.VIRTUAL | SWT.FULL_SELECTION);
        tableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        TableViewerColumn tbviewerClmnNo = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tbclmnNo = tbviewerClmnNo.getColumn();
        tbclmnNo.setText(StringConstants.NO_);
        tbclmnNo.setWidth(40);
        tbviewerClmnNo.setLabelProvider(new CellLabelProvider() {

            @Override
            public void update(ViewerCell cell) {
                int order = tableViewer.getTable().indexOf((TableItem) cell.getItem()) + 1;
                cell.setText(Integer.toString(order));
            }
        });

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        return compositeTable;
    }

    private void loadInput(final DataFileEntity dataFile) {
        fCurrentPath = dataFile.getDataSourceUrl();

        txtFileName.setText(fCurrentPath);

        ckcbUseRelativePath.setSelection(dataFile.getIsInternalPath());

        fCurrentSheetName = dataFile.getSheetName();

        readExcelFile();
    }

    private void readExcelFile() {
        if (ableToReload) {
            if (loadFileJob != null && loadFileJob.getState() == Job.RUNNING) {
                loadFileJob.cancel();
                loadFileJob.removeJobChangeListener(readExcelJobListener);
            }

            loadFileJob = new LoadExcelFileJob(getSourceUrlAbsolutePath());
            loadFileJob.setUser(true);
            loadFileJob.schedule();

            loadFileJob.addJobChangeListener(readExcelJobListener);
        }
    }

    private IJobChangeListener readExcelJobListener = new JobChangeAdapter() {

        @Override
        public void done(final IJobChangeEvent event) {
            sync.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (event.getResult() == Status.OK_STATUS) {
                        loadSheetNames(loadFileJob.getSheetNames());
                        loadExcelDataToTable();
                    }
                }
            });
        }
    };
    private Label lblFileInfoStatus;

    private void addControlListeners() {
        btnBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(btnBrowse.getShell());
                dialog.setFilterNames(FILTER_NAMES);
                dialog.setFilterExtensions(FILTER_EXTS);
                dialog.setFilterPath(getProjectFolderLocation());

                String absolutePath = dialog.open();
                if (absolutePath == null || absolutePath.equals(fCurrentPath)) {
                    return;
                }
                
                lblFileInfoStatus.setText("");
                    
                fCurrentPath = absolutePath;
                fCurrentSheetName = "";

                if (ckcbUseRelativePath.getSelection()) {
                    txtFileName.setText(PathUtils.absoluteToRelativePath(fCurrentPath, getProjectFolderLocation()));
                } else {
                    txtFileName.setText(fCurrentPath);
                }

                readExcelFile();

                dirtyable.setDirty(true);
            }
        });

        cbbSheets.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String selectedSheetName = cbbSheets.getText();

                if (fCurrentSheetName.equals(selectedSheetName)) {
                    return;
                }
                lblFileInfoStatus.setText("");
                
                fCurrentSheetName = selectedSheetName;
                loadExcelDataToTable();
                dirtyable.setDirty(true);
            }
        });

        ckcbUseRelativePath.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (txtFileName.getText() != null) {
                    String sourceUrl = txtFileName.getText();
                    if (ckcbUseRelativePath.getSelection()) {
                        txtFileName.setText(PathUtils.absoluteToRelativePath(sourceUrl, getProjectFolderLocation()));
                    } else {
                        txtFileName.setText(PathUtils.relativeToAbsolutePath(sourceUrl, getProjectFolderLocation()));
                    }
                }
                dirtyable.setDirty(true);
            }
        });

        btnExpandFileInfo.addListener(SWT.MouseDown, layoutFileInfoCompositeListener);
        lblFileInfo.addListener(SWT.MouseDown, layoutFileInfoCompositeListener);
        lblFileInfoStatus.addListener(SWT.MouseDown, layoutFileInfoCompositeListener);
    }

    private void redrawBtnExpandFileInfo() {
        btnExpandFileInfo.getParent().setRedraw(false);
        if (isFileInfoExpanded) {
            btnExpandFileInfo.setImage(ImageConstants.IMG_16_ARROW_UP_BLACK);
        } else {
            btnExpandFileInfo.setImage(ImageConstants.IMG_16_ARROW_DOWN_BLACK);
        }
        btnExpandFileInfo.getParent().setRedraw(true);

    }

    private void selectDefaultSheet(String[] sheetNames) {
        if (sheetNames.length > 0) {
            fCurrentSheetName = sheetNames[0];
            cbbSheets.select(0);
        }
    }

    private void loadSheetNames(String[] sheetNames) {
        try {
            if (cbbSheets.isDisposed()) {
                return;
            }

            cbbSheets.setItems(sheetNames);

            if (StringUtils.isBlank(fCurrentSheetName)) {
                selectDefaultSheet(sheetNames);
            } else {
                int currentIdx = cbbSheets.indexOf(fCurrentSheetName);
                if (currentIdx < 0) {
                    MessageDialog.openWarning(null, StringConstants.WARN_TITLE,
                            MessageFormat.format(StringConstants.PA_WARN_MSG_SHEET_NOT_FOUND, fCurrentSheetName));
                    selectDefaultSheet(sheetNames);
                } else {
                    cbbSheets.select(cbbSheets.indexOf(fCurrentSheetName));
                }
            }

        } catch (Exception e) {
            MessageDialog.openWarning(null, StringConstants.WARN_TITLE,
                    StringConstants.PA_WARN_MSG_UNABLE_TO_LOAD_SHEET_NAME);
        }
    }

    private void clearTable() {
        while (tableViewer.getTable().getColumnCount() > 1) {
            tableViewer.getTable().getColumns()[1].dispose();
        }
        tableViewer.getTable().clearAll();
    }

    private void loadExcelDataToTable() {
        try {
            tableViewer.getTable().setRedraw(false);
            clearTable();

            String[] headers = null;
            if (cbbSheets.getSelectionIndex() < 0) {
                return;
            }

            final ExcelData excelData = new ExcelData(cbbSheets.getText(), getSourceUrlAbsolutePath());
            headers = excelData.getColumnNames();
            if (headers.length <= 0) {
                return;
            }

            int rowNumbers = excelData.getRowNumbers();
            int columnNumbers = excelData.getColumnNumbers();

            if (columnNumbers > MAX_COLUMN_COUNT) {
                MessageDialog.openWarning(null, StringConstants.WARN,
                        MessageFormat.format(StringConstants.PA_FILE_TOO_LARGE, MAX_COLUMN_COUNT));
                columnNumbers = MAX_COLUMN_COUNT;
            }

            fData = new String[rowNumbers][columnNumbers];

            tableViewer.getTable().setItemCount(rowNumbers);

            int numEmptyHeader = 0;
            for (int i = 0; i < columnNumbers; i++) {
                final int idx = i;
                if (idx >= tableViewer.getTable().getColumnCount() - 1) {
                    TableViewerColumn columnViewer = new TableViewerColumn(tableViewer, SWT.NONE);
                    String header = headers[i];
                    if (!StringUtils.isBlank(header)) {
                        columnViewer.getColumn().setText(header);
                    } else {
                        columnViewer.getColumn().setImage(ImageConstants.IMG_16_WARN_TABLE_ITEM);
                        columnViewer.getColumn().setToolTipText(StringConstants.PA_TOOLTIP_WARNING_COLUMN_HEADER);
                        columnViewer.getColumn().setText(StringUtils.EMPTY);
                        numEmptyHeader++;
                    }

                    columnViewer.getColumn().setWidth(COLUMN_WIDTH);
                    columnViewer.setLabelProvider(new ColumnLabelProvider() {
                        @Override
                        public void update(final ViewerCell cell) {
                            final int columnIndex = cell.getColumnIndex() - 1;
                            final int rowIndex = tableViewer.getTable().indexOf((TableItem) cell.getItem());
                            String text = "...";
                            cell.setText(text);

                            sync.asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    if (fData == null) {
                                        return;
                                    }
                                    if (fData[rowIndex][columnIndex] == null) {
                                        final String text = excelData.getValue(columnIndex + 1, rowIndex + 1);
                                        if (text == null) {
                                            fData[rowIndex][columnIndex] = "";
                                        } else {
                                            fData[rowIndex][columnIndex] = text;
                                        }
                                        if (!cell.getItem().isDisposed()) {
                                            cell.setText(text);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            }

            if (numEmptyHeader > 0) {
                lblFileInfoStatus.setText(MessageFormat.format(StringConstants.PA_LBL_WARNING_COLUMN_HEADER,
                        numEmptyHeader, columnNumbers));
            }

            tableViewer.setInput(fData);

        } catch (IllegalArgumentException ex) {
            fData = null;
        } catch (Exception e) {
            fData = null;
            LoggerSingleton.logError(e);
        } finally {
            tableViewer.getTable().setRedraw(true);
        }
    }

    @Persist
    public void save() {
        try {
            ableToReload = false;
            String oldPk = originalDataFile.getId();
            String oldName = originalDataFile.getName();
            String oldIdForDisplay = TestDataController.getInstance().getIdForDisplay(originalDataFile);
            originalDataFile = updateDataFileProperty(originalDataFile.getLocation(), txtName.getText(),
                    txtDesc.getText(), DataFileDriverType.ExcelFile, txtFileName.getText(), cbbSheets.getText(),
                    ckcbUseRelativePath.getSelection(), true);
            updateDataFile(originalDataFile);
            dirtyable.setDirty(false);
            eventBroker.post(EventConstants.EXPLORER_REFRESH_TREE_ENTITY, null);
            if (!StringUtils.equalsIgnoreCase(oldName, originalDataFile.getName())) {
                eventBroker.post(EventConstants.EXPLORER_RENAMED_SELECTED_ITEM, new Object[] { oldIdForDisplay,
                        TestDataController.getInstance().getIdForDisplay(originalDataFile) });
            }
            sendTestDataUpdatedEvent(oldPk);
        } catch (DuplicatedFileNameException e) {
            MultiStatusErrorDialog.showErrorDialog(e, StringConstants.PA_ERROR_MSG_UNABLE_TO_SAVE_TEST_DATA,
                    MessageFormat.format(StringConstants.PA_ERROR_REASON_TEST_DATA_EXISTED, txtName.getText()));
        } catch (Exception e) {
            LoggerSingleton.logError(e);
            MultiStatusErrorDialog.showErrorDialog(e, StringConstants.PA_ERROR_MSG_UNABLE_TO_SAVE_TEST_DATA, e
                    .getClass().getSimpleName());
        } finally {
            ableToReload = true;
        }
    }

    public DataFileEntity updateDataFileProperty(String pk, String name, String description,
            DataFileDriverType dataFileDriver, String dataSourceURL, String sheetName, boolean isInternalPath,
            boolean isHeaderEnabled) throws Exception {

        DataFilePropertyInputEntity dataFileInputPro = new DataFilePropertyInputEntity();

        dataFileInputPro.setPk(pk);
        dataFileInputPro.setName(name);
        dataFileInputPro.setDescription(description);
        dataFileInputPro.setDataFileDriver(dataFileDriver.name());
        dataFileInputPro.setdataSourceURL(dataSourceURL);
        dataFileInputPro.setSheetName(sheetName);
        dataFileInputPro.setIsInternalPath(isInternalPath);
        dataFileInputPro.setEnableHeader(isHeaderEnabled);
        DataFileEntity dataFileEntity = TestDataController.getInstance().updateDataFile(dataFileInputPro);
        return dataFileEntity;
    }

    private String getProjectFolderLocation() {
        return ProjectController.getInstance().getCurrentProject().getFolderLocation();
    }

    @Override
    protected void updateChildInfo(DataFileEntity dataFile) {
        loadInput(dataFile);
    }

    @Override
    protected EPartService getPartService() {
        return partService;
    }

    @Override
    protected void preDestroy() {
        fCurrentPath = "";
        fCurrentSheetName = "";
        fData = null;
    }
}
