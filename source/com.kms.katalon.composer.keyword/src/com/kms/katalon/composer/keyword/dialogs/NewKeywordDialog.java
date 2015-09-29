package com.kms.katalon.composer.keyword.dialogs;

import javax.naming.InvalidNameException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.kms.katalon.composer.components.impl.dialogs.AbstractEntityDialog;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.keyword.constants.StringConstants;
import com.kms.katalon.groovy.constant.GroovyConstants;

@SuppressWarnings("restriction")
public class NewKeywordDialog extends AbstractEntityDialog {

    private IPackageFragment parentPackage;

    private IPackageFragmentRoot rootPackage;

    private Text txtPackage;

    private Button btnBrowse;

    public NewKeywordDialog(Shell parentShell, IPackageFragmentRoot rootPackage, IPackageFragment parentPackage) {
        super(parentShell, null);
        setDialogTitle(StringConstants.DIA_TITLE_KEYWORD);
        setDialogMsg(StringConstants.DIA_MSG_CREATE_KEYWORD);
        this.rootPackage = rootPackage;
        this.parentPackage = parentPackage;
    }

    @Override
    public Control createDialogBodyArea(Composite parent) {
        if (container == null) {
            container = new Composite(parent, SWT.NONE);
        }
        createPackageNameControl(container, 3);
        return super.createDialogBodyArea(parent);
    }

    private Control createPackageNameControl(Composite parent, int column) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        parent.setLayout(new GridLayout(column, false));
        Label labelName = new Label(parent, SWT.NONE);
        labelName.setText(StringConstants.PACKAGE);

        txtPackage = new Text(parent, SWT.BORDER);
        txtPackage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtPackage.setText(getParentPackage().getElementName());
        txtPackage.selectAll();
        txtPackage.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateStatus();
            }
        });

        btnBrowse = new Button(parent, SWT.PUSH);
        btnBrowse.setText(StringConstants.BROWSE);
        btnBrowse.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                parentPackage = choosePackage();
                txtPackage.setText(getParentPackage().getElementName());
            }

            public void widgetSelected(SelectionEvent e) {
                parentPackage = choosePackage();
                txtPackage.setText(getParentPackage().getElementName());
            }
        });
        return parent;
    }

    @Override
    public void validateEntityName(String entityName) throws Exception {
        // validate package name
        validatePackageName();

        // validate Keyword class name
        entityName = entityName + GroovyConstants.GROOVY_FILE_EXTENSION;

        if (parentPackage.exists()) {
            ICompilationUnit[] cuList = parentPackage.getCompilationUnits();
            for (ICompilationUnit cu : cuList) {
                if (StringUtils.equalsIgnoreCase(entityName, cu.getElementName())) {
                    throw new InvalidNameException(
                            com.kms.katalon.composer.components.impl.constants.StringConstants.DIA_NAME_EXISTED);
                }
            }
        }

        GroovyCompilationUnit cu = (GroovyCompilationUnit) parentPackage.getCompilationUnit(entityName);
        IStatus status = JavaConventionsUtil.validateCompilationUnitName(entityName, cu);
        if (status.isOK()) {
            if (StringUtils.isAllLowerCase(entityName.substring(0, 1))) {
                setMessage(StringConstants.DIA_WARN_KEYWORD_START_WITH_LOWERCASE, IMessageProvider.WARNING);
            }
        } else {
            throw new InvalidNameException(status.getMessage().replaceAll(" Java", "")
                    .replaceAll("(?i)type", StringConstants.KEYWORD));
        }
    }

    private void validatePackageName() throws InvalidNameException {
        String packageName = txtPackage.getText();
        if (packageName.isEmpty()) {
            setMessage(StringConstants.DIA_WARN_DEFAULT_PACKAGE, IMessageProvider.WARNING);
        } else {
            setMessage(getDialogMsg(), getMsgType());
            IPackageFragment pkg = rootPackage.getPackageFragment(packageName);
            IStatus status = JavaConventionsUtil.validatePackageName(packageName, pkg);
            if (status.isOK()) {
                parentPackage = pkg;
            } else {
                throw new InvalidNameException(status.getMessage().replaceAll(" Java", ""));
            }
        }
    }

    protected IPackageFragment choosePackage() {
        IJavaElement[] packages = null;
        try {
            if (rootPackage != null && rootPackage.exists()) {
                packages = rootPackage.getChildren();
            }
        } catch (JavaModelException e) {
            LoggerSingleton.logError(e);
        }
        if (packages == null) {
            packages = new IJavaElement[0];
        }

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT));
        dialog.setIgnoreCase(false);
        dialog.setTitle(StringConstants.DIA_TITLE_PACKAGE_SELECTION);
        dialog.setMessage(StringConstants.DIA_MSG_CHOOSE_A_PACKAGE);
        dialog.setEmptyListMessage(StringConstants.DIA_MSG_NO_PACKAGE);
        dialog.setElements(packages);
        dialog.setHelpAvailable(false);

        if (parentPackage != null) {
            dialog.setInitialSelections(new Object[] { parentPackage });
        }

        if (dialog.open() == Window.OK) {
            return (IPackageFragment) dialog.getFirstResult();
        }
        return parentPackage;
    }

    public IPackageFragment getParentPackage() {
        return parentPackage;
    }

}