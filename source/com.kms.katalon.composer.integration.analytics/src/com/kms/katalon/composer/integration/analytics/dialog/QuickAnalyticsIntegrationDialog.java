package com.kms.katalon.composer.integration.analytics.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.components.controls.HelpComposite;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.integration.analytics.constants.ComposerIntegrationAnalyticsMessageConstants;
import com.kms.katalon.integration.analytics.entity.AnalyticsProject;
import com.kms.katalon.integration.analytics.entity.AnalyticsTeam;
import com.kms.katalon.integration.analytics.entity.AnalyticsTokenInfo;
import com.kms.katalon.integration.analytics.handler.AnalyticsAuthorizationHandler;

public class QuickAnalyticsIntegrationDialog extends Dialog{
	public static final int OK_ID = 2;
	
	private Composite container;
	
	private Button btnOk;
	
	private Button cbxAutoSubmit;
	
	private Button cbxAttachScreenshot;
	
    private Combo cbbProjects;

    private Combo cbbTeams;

    private List<AnalyticsProject> projects = new ArrayList<>();

    private List<AnalyticsTeam> teams = new ArrayList<>();
	
	public QuickAnalyticsIntegrationDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		
		shell.setText(ComposerIntegrationAnalyticsMessageConstants.TITLE_DLG_QUICK_ANALYTICS_INTEGRATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        
        Label lblNote = new Label(container, SWT.NONE);
        lblNote.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_QUICK_TITLE_ANALYTICS_INTEGRATION);
        
        Composite recommendComposite = new Composite(container, SWT.NONE);
        recommendComposite.setLayout(new GridLayout(2, false));
        recommendComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        
        Label lblRecommend = new Label(recommendComposite, SWT.NONE);
        lblRecommend.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_QUICK_ANALYTICS_INTEGRATION_RECOMMEND);
        
        new HelpComposite(recommendComposite, "");
        
        Group grpSelect = new Group(container, SWT.NONE);
        grpSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout glGrpSelect = new GridLayout(4, false);
        grpSelect.setLayout(glGrpSelect);
        grpSelect.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_SELECT_GROUP);
        
        Label lblTeam = new Label(grpSelect, SWT.NONE);
        lblTeam.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_TEAM);

        cbbTeams = new Combo(grpSelect, SWT.READ_ONLY);
        cbbTeams.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblProject = new Label(grpSelect, SWT.NONE);
        lblProject.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_PROJECT);

        cbbProjects = new Combo(grpSelect, SWT.READ_ONLY);
        cbbProjects.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        Group grpTestResult = new Group(container, SWT.NONE);
        grpTestResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout glGrpTestResult = new GridLayout(2, false);
        glGrpTestResult.horizontalSpacing = 15;
        grpTestResult.setLayout(glGrpTestResult);
        grpTestResult.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_TEST_RESULT_GROUP);

        cbxAutoSubmit = new Button(grpTestResult, SWT.CHECK);
        cbxAutoSubmit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cbxAutoSubmit.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_QUICK_ANALYTICS_INTEGRATION_AUTO_SUBMIT);
        new HelpComposite(grpTestResult, "");

        Composite attachComposite = new Composite(grpTestResult, SWT.NONE);
        GridLayout glGrpAttach = new GridLayout(1, false);
        glGrpAttach.marginLeft = 15;
        attachComposite.setLayout(glGrpAttach);
        attachComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        cbxAttachScreenshot = new Button(attachComposite, SWT.CHECK);
        cbxAttachScreenshot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cbxAttachScreenshot.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_TEST_RESULT_ATTACH_SCREENSHOT);
       
        Label lblSuggest = new Label(container, SWT.NONE);
        lblSuggest.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_QUICK_ANALYTICS_INTEGRATION_UPLOAD);
        
        Label lblDir = new Label(container, SWT.NONE);
        lblDir.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_QUICK_ANALYTICS_INTEGRATION_TO_CONFIG);
        ControlUtils.setFontStyle(lblDir, SWT.BOLD | SWT.ITALIC, -1);
        
        initialize();
        
        return container;
	}
	
    protected void initialize() {
    	cbxAutoSubmit.setSelection(true);
    	cbxAttachScreenshot.setSelection(true);
    }
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		btnOk = createButton(parent, OK_ID, "OK", true);
		addControlListeners();
	}
	
	private void addControlListeners() {
		btnOk.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
//                handleUpload();
            	okPressed();
            }
        });
		
		cbxAutoSubmit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cbxAttachScreenshot.setSelection(cbxAutoSubmit.getSelection());
			}
		});
		
		cbbTeams.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
//                AnalyticsTeam getSelectTeam = teams.get(cbbTeams.getSelectionIndex());
//				
//				String serverUrl = txtServerUrl.getText();
//				String email = txtEmail.getText();
//				String password = txtPassword.getText();
//				AnalyticsTokenInfo tokenInfo = AnalyticsAuthorizationHandler.getToken(serverUrl, email, password, analyticsSettingStore);
//				projects = AnalyticsAuthorizationHandler.getProjects(serverUrl, email, password,
//						getSelectTeam, tokenInfo, new ProgressMonitorDialog(getShell()));
//				
//				setProjectsBasedOnTeam(getSelectTeam, projects);
//				changeEnabled();
            }
        });
	}
	
    @Override
    protected Point getInitialSize() {
//        return getShell().computeSize(450, 370, true);
        return getShell().computeSize(550, 370, true);
    }
}
