package com.kms.katalon.composer.webui.setting;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.kms.katalon.composer.components.dialogs.PreferencePageWithHelp;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.webui.constants.ComposerWebuiMessageConstants;
import com.kms.katalon.composer.webui.constants.ImageConstants;
import com.kms.katalon.composer.webui.constants.StringConstants;
import com.kms.katalon.constants.DocumentationMessageConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.execution.webui.setting.WebUiExecutionSettingStore;
import com.kms.katalon.util.collections.Pair;

public class WebLocatorsPerferencePage extends PreferencePageWithHelp {

    private static final String MSG_PROPERTY_NAME_IS_EXISTED = ComposerWebuiMessageConstants.MSG_PROPERTY_NAME_IS_EXISTED;

    private static final String GRP_LBL_DEFAULT_SELECTED_PROPERTIES_FOR_CAPTURED_TEST_OBJECT = ComposerWebuiMessageConstants.GRP_LBL_DEFAULT_SELECTED_PROPERTIES_FOR_CAPTURED_TEST_OBJECT;

    private static final String COL_LBL_DETECT_OBJECT_BY = ComposerWebuiMessageConstants.COL_LBL_DETECT_OBJECT_BY;

    private WebUiExecutionSettingStore store;

    private Composite container;

    ToolItem tiAdd, tiDelete, tiClear;

    private Table tProperty;

    private TableViewer tvProperty;

    private TableViewerColumn cvName, cvSelected;

    private TableColumn cName, cSelected;

    private List<Pair<String, Boolean>> defaultSelectingCapturedObjectProperties;

    public WebLocatorsPerferencePage() {
        store = new WebUiExecutionSettingStore(ProjectController.getInstance().getCurrentProject());
        defaultSelectingCapturedObjectProperties = Collections.emptyList();
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 10;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        container.setLayout(layout);

        createTestObjectLocatorSettings(container);

        try {
            initialize();
        } catch (IOException e) {
            LoggerSingleton.logError(e);
        }
        registerListeners();

        return container;
    }

    private void createTestObjectLocatorSettings(Composite container) {
        Group locatorGroup = new Group(container, SWT.NONE);
        locatorGroup.setText(GRP_LBL_DEFAULT_SELECTED_PROPERTIES_FOR_CAPTURED_TEST_OBJECT);
        locatorGroup.setLayout(new GridLayout());
        locatorGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Composite locatorContainer = new Composite(locatorGroup, SWT.NONE);
        locatorContainer.setLayout(new GridLayout());
        locatorContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ToolBar tb = new ToolBar(locatorContainer, SWT.FLAT | SWT.RIGHT);
        tiAdd = new ToolItem(tb, SWT.PUSH);
        tiAdd.setText(StringConstants.ADD);
        tiAdd.setImage(ImageConstants.IMG_16_ADD);

        tiDelete = new ToolItem(tb, SWT.PUSH);
        tiDelete.setText(StringConstants.DELETE);
        tiDelete.setImage(ImageConstants.IMG_16_DELETE);
        tiDelete.setEnabled(false);

        tiClear = new ToolItem(tb, SWT.PUSH);
        tiClear.setText(StringConstants.CLEAR);
        tiClear.setImage(ImageConstants.IMG_16_CLEAR);

        createPropertyTable(locatorContainer);
    }

    @SuppressWarnings("unchecked")
    private void createPropertyTable(Composite parent) {
        Composite tableComposite = new Composite(parent, SWT.NONE);
        GridData ldTableComposite = new GridData(SWT.FILL, SWT.FILL, true, true);
        ldTableComposite.minimumHeight = 70;
        ldTableComposite.heightHint = 380;
        tableComposite.setLayoutData(ldTableComposite);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);

        tvProperty = new TableViewer(tableComposite,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        tvProperty.setContentProvider(ArrayContentProvider.getInstance());
        tProperty = tvProperty.getTable();
        tProperty.setHeaderVisible(true);
        tProperty.setLinesVisible(true);

        cvName = new TableViewerColumn(tvProperty, SWT.LEFT);
        cName = cvName.getColumn();
        cName.setText(StringConstants.NAME);
        cvName.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                return ((Pair<String, Boolean>) element).getLeft();
            }
        });

        cvName.setEditingSupport(new EditingSupport(cvName.getViewer()) {

            @Override
            protected void setValue(Object element, Object value) {
                String newName = String.valueOf(value);

                if (StringUtils.isBlank(newName)) {
                    defaultSelectingCapturedObjectProperties.remove(element);
                    tvProperty.refresh();
                    return;
                }

                if (StringUtils.equals(((Pair<String, Boolean>) element).getLeft(), newName)) {
                    return;
                }

                boolean isExisted = defaultSelectingCapturedObjectProperties.stream()
                        .filter(i -> i.getLeft().equals(newName))
                        .count() > 0;

                if (isExisted) {
                    MessageDialog.openWarning(getShell(), StringConstants.WARN, MSG_PROPERTY_NAME_IS_EXISTED);
                    tvProperty.refresh();
                    return;
                }
                ((Pair<String, Boolean>) element).setLeft(newName);
                tvProperty.update(element, null);
            }

            @Override
            protected Object getValue(Object element) {
                return ((Pair<String, Boolean>) element).getLeft();
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor(tProperty);
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }
        });

        cvSelected = new TableViewerColumn(tvProperty, SWT.CENTER);
        cSelected = cvSelected.getColumn();
        cSelected.setText(COL_LBL_DETECT_OBJECT_BY);

        cvSelected.setLabelProvider(new CellLabelProvider() {

            @Override
            public void update(ViewerCell cell) {
                Object property = cell.getElement();
                if (!(property instanceof Pair)) {
                    return;
                }
                Boolean isSelected = ((Pair<String, Boolean>) property).getRight();
                FontDescriptor fontDescriptor = FontDescriptor.createFrom(cell.getFont());
                Font font = fontDescriptor.setStyle(SWT.NORMAL).setHeight(13).createFont(tProperty.getDisplay());
                cell.setFont(font);
                cell.setText(getCheckboxSymbol(isSelected));
            }
        });
        cvSelected.setEditingSupport(new EditingSupport(cvSelected.getViewer()) {

            @Override
            protected void setValue(Object element, Object value) {
                ((Pair<String, Boolean>) element).setRight((boolean) value);
                tvProperty.update(element, null);
            }

            @Override
            protected Object getValue(Object element) {
                return ((Pair<String, Boolean>) element).getRight();
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return new CheckboxCellEditor();
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }
        });

        tableColumnLayout.setColumnData(cName, new ColumnWeightData(80, 100));
        tableColumnLayout.setColumnData(cSelected, new ColumnWeightData(20, 100));
    }

    private String getCheckboxSymbol(boolean isChecked) {
        // Unicode symbols
        // Checked box: \u2611
        // Unchecked box: \u2610
        return isChecked ? "\u2611" : "\u2610";
    }

    protected void registerListeners() {
        tiAdd.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Pair<String, Boolean> element = Pair.of(StringConstants.EMPTY, false);
                defaultSelectingCapturedObjectProperties.add(element);
                tvProperty.refresh();
                tvProperty.editElement(element, 0);
            }

        });
        tiDelete.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] selectedPropertyIndices = tProperty.getSelectionIndices();
                if (selectedPropertyIndices.length == 0) {
                    return;
                }

                List<Pair<String, Boolean>> selectedProperties = Arrays.stream(selectedPropertyIndices)
                        .boxed()
                        .map(i -> defaultSelectingCapturedObjectProperties.get(i))
                        .collect(Collectors.toList());
                defaultSelectingCapturedObjectProperties.removeAll(selectedProperties);
                tvProperty.refresh();
            }

        });

        tiClear.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                defaultSelectingCapturedObjectProperties.clear();
                tvProperty.refresh();
            }

        });

        tvProperty.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                StructuredSelection selection = (StructuredSelection) tvProperty.getSelection();
                tiDelete.setEnabled(selection != null && selection.getFirstElement() != null);
            }
        });
    }

    private void initialize() throws IOException {
        setInputForCapturedObjectPropertySetting(store.getCapturedTestObjectLocators());
    }

    private void setInputForCapturedObjectPropertySetting(List<Pair<String, Boolean>> input) {
        defaultSelectingCapturedObjectProperties = input;
        tvProperty.setInput(defaultSelectingCapturedObjectProperties);
    }

    @Override
    protected void performDefaults() {
        if (container == null) {
            return;
        }
        try {
            store.setDefaultCapturedTestObjectLocators();
            setInputForCapturedObjectPropertySetting(store.getCapturedTestObjectLocators());
        } catch (IOException e) {
            LoggerSingleton.logError(e);
        }
    }

    @Override
    public boolean performOk() {
        if (super.performOk() && isValid()) {
            if (tvProperty != null) {
                try {
                    List<Pair<String, Boolean>> emptyItems = defaultSelectingCapturedObjectProperties.stream()
                            .filter(i -> i.getLeft().isEmpty())
                            .collect(Collectors.toList());
                    defaultSelectingCapturedObjectProperties.removeAll(emptyItems);
                    store.setCapturedTestObjectLocators(defaultSelectingCapturedObjectProperties);
                } catch (IOException e) {
                    LoggerSingleton.logError(e);
                }
            }
        }
        return true;
    }

	@Override
    public boolean hasDocumentation() {
		return true;
	}

	@Override
    public String getDocumentationUrl() {
		return DocumentationMessageConstants.SETTINGS_WEBLOCATORS;
	}
}