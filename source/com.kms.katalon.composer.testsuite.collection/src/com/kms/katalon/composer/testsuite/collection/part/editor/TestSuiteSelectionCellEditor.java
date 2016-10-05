package com.kms.katalon.composer.testsuite.collection.part.editor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.kms.katalon.composer.components.impl.dialogs.TreeEntitySelectionDialog;
import com.kms.katalon.composer.components.impl.editors.CustomDialogCellEditor;
import com.kms.katalon.composer.components.impl.tree.TestSuiteTreeEntity;
import com.kms.katalon.composer.components.impl.util.TreeEntityUtil;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.explorer.providers.EntityLabelProvider;
import com.kms.katalon.composer.explorer.providers.EntityProvider;
import com.kms.katalon.composer.testsuite.collection.constant.StringConstants;
import com.kms.katalon.composer.testsuite.collection.part.provider.TestSuiteViewerFilter;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.entity.testsuite.TestSuiteEntity;

public class TestSuiteSelectionCellEditor extends CustomDialogCellEditor {

    private ProjectEntity currentProject;

    private TestSuiteEntity testSuiteEntity;

    public TestSuiteSelectionCellEditor(Composite parent) {
        super(parent);
        currentProject = ProjectController.getInstance().getCurrentProject();

    }

    private TestSuiteTreeEntity getSelectedObjectForDialog() throws Exception {
        if (testSuiteEntity == null) {
            return null;
        }
        return new TestSuiteTreeEntity(testSuiteEntity, TreeEntityUtil.createSelectedTreeEntityHierachy(
                testSuiteEntity.getParentFolder(), FolderController.getInstance().getTestSuiteRoot(currentProject)));
    }

    private Object[] getInputForTreeDialog() throws Exception {
        return TreeEntityUtil.getChildren(null, FolderController.getInstance().getTestSuiteRoot(currentProject));
    }

    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        EntityProvider entityProvider = new EntityProvider();
        TreeEntitySelectionDialog dialog = new TreeEntitySelectionDialog(getControl().getShell(),
                new EntityLabelProvider(), entityProvider, new TestSuiteViewerFilter(entityProvider));

        dialog.setAllowMultiple(false);
        dialog.setTitle(StringConstants.DIA_TITLE_TEST_SUITE_BROWSER);
        try {
            dialog.setInput(getInputForTreeDialog());
            dialog.setInitialSelection(getSelectedObjectForDialog());
        } catch (Exception e) {
            LoggerSingleton.logError(e);
            return null;
        }
        if (dialog.open() != Dialog.OK) {
            return null;
        }

        Object result = dialog.getFirstResult();
        if (!(result instanceof TestSuiteTreeEntity)) {
            return null;
        }

        try {
            return (TestSuiteEntity) ((TestSuiteTreeEntity) result).getObject();
        } catch (Exception e) {
            LoggerSingleton.logError(e);
            return null;
        }
    }
    
    protected void updateContents(Object value) {
       if (value instanceof TestSuiteEntity) {
           testSuiteEntity = (TestSuiteEntity) value;
           defaultLabel.setText(testSuiteEntity != null ? testSuiteEntity.getIdForDisplay() : StringUtils.EMPTY);
       }
    }

}