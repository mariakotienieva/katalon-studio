package com.kms.katalon.composer.webservice.parts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MCompositePart;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.composer.components.controls.HelpToolBarForMPart;
import com.kms.katalon.composer.components.controls.ToolBarForMPart;
import com.kms.katalon.composer.components.impl.control.DropdownToolItemSelectionListener;
import com.kms.katalon.composer.components.impl.tree.FolderTreeEntity;
import com.kms.katalon.composer.components.impl.tree.WebElementTreeEntity;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.components.impl.util.EntityPartUtil;
import com.kms.katalon.composer.components.impl.util.EventUtil;
import com.kms.katalon.composer.components.impl.util.KeyEventUtil;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.part.IComposerPartEvent;
import com.kms.katalon.composer.components.tree.ITreeEntity;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.parts.SavableCompositePart;
import com.kms.katalon.composer.resources.constants.IImageKeys;
import com.kms.katalon.composer.resources.image.ImageManager;

import com.kms.katalon.composer.testcase.constants.ComposerTestcaseMessageConstants;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.composer.testcase.parts.IVariablePart;
import com.kms.katalon.composer.testcase.parts.TestCaseVariableView;
import com.kms.katalon.composer.util.groovy.GroovyEditorUtil;
import com.kms.katalon.composer.webservice.components.MirrorEditor;
import com.kms.katalon.composer.webservice.constants.ComposerWebserviceMessageConstants;
import com.kms.katalon.composer.webservice.constants.StringConstants;
import com.kms.katalon.composer.webservice.support.PropertyNameEditingSupport;
import com.kms.katalon.composer.webservice.support.PropertyValueEditingSupport;
import com.kms.katalon.composer.webservice.view.ParameterTable;
import com.kms.katalon.composer.webservice.view.WSRequestPartUI;
import com.kms.katalon.composer.webservice.view.WebServiceAPIControl;
import com.kms.katalon.constants.DocumentationMessageConstants;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.GlobalMessageConstants;
import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ObjectRepositoryController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.core.logging.model.TestStatus.TestStatusValue;
import com.kms.katalon.core.testobject.ResponseObject;
import com.kms.katalon.core.util.internal.Base64;
import com.kms.katalon.core.webservice.common.PrivateKeyReader;
import com.kms.katalon.core.webservice.common.ScriptSnippet;
import com.kms.katalon.core.webservice.common.VerificationScriptSnippetFactory;
import com.kms.katalon.core.webservice.constants.RequestHeaderConstants;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.repository.WebElementPropertyEntity;
import com.kms.katalon.entity.repository.WebServiceRequestEntity;
import com.kms.katalon.entity.variable.VariableEntity;
import com.kms.katalon.execution.exception.ExecutionException;
import com.kms.katalon.execution.webservice.VariableEvaluator;
import com.kms.katalon.execution.webservice.VerificationScriptExecutor;

public abstract class WebServicePart implements IVariablePart, SavableCompositePart, EventHandler, IComposerPartEvent {
    
    protected static final String WS_BUNDLE_NAME = FrameworkUtil.getBundle(WebServicePart.class).getSymbolicName();

    private static final Font FONT_COURIER_NEW_12 = new Font(Display.getCurrent(), "Courier New", 12, SWT.NORMAL);

    private static final Font FONT_CONSOLAS_10 = new Font(Display.getCurrent(), "Consolas", 10, SWT.NORMAL);
    
    protected static final String TAB_SPACE = "    ";

    private static final char RAW_PASSWORD_CHAR_MASK = '\0';

    private static final char PASSWORD_CHAR_MASK = '\u2022';

    private static final int AUTH_LBL_WIDTH = 100;

    private static final int AUTH_FIELD_WIDTH = 300;

    private static final String BASIC_AUTH_PREFIX_VALUE = ComposerWebserviceMessageConstants.BASIC_AUTH_PREFIX_VALUE;

    private static final String HTTP_HEADER_AUTHORIZATION = RequestHeaderConstants.AUTHORIZATION;

    private static final String AUTH_META_PREFIX = RequestHeaderConstants.AUTH_META_PREFIX;

    private static final String AUTHORIZATION_OAUTH_REALM = RequestHeaderConstants.AUTHORIZATION_OAUTH_REALM;

    private static final String AUTHORIZATION_OAUTH_TOKEN_SECRET = RequestHeaderConstants.AUTHORIZATION_OAUTH_TOKEN_SECRET;

    private static final String AUTHORIZATION_OAUTH_TOKEN = RequestHeaderConstants.AUTHORIZATION_OAUTH_TOKEN;

    private static final String AUTHORIZATION_OAUTH_SIGNATURE_METHOD = RequestHeaderConstants.AUTHORIZATION_OAUTH_SIGNATURE_METHOD;

    private static final String AUTHORIZATION_OAUTH_CONSUMER_SECRET = RequestHeaderConstants.AUTHORIZATION_OAUTH_CONSUMER_SECRET;

    private static final String AUTHORIZATION_OAUTH_CONSUMER_KEY = RequestHeaderConstants.AUTHORIZATION_OAUTH_CONSUMER_KEY;

    protected static final String AUTHORIZATION_TYPE = RequestHeaderConstants.AUTHORIZATION_TYPE;

    private static final String BASIC_AUTH = ComposerWebserviceMessageConstants.BASIC_AUTH;

    private static final String NO_AUTH = ComposerWebserviceMessageConstants.NO_AUTH;

    private static final String LBL_SIGNATURE_METHOD = ComposerWebserviceMessageConstants.PA_LBL_SIGNATURE_METHOD;

    private static final String TOOLTIP_CONSUMER_SECRET = ComposerWebserviceMessageConstants.PA_TOOLTIP_CONSUMER_SECRET;

    private static final String TXT_IMPORT_CONSUMER_SECRET_FROM_FILE = ComposerWebserviceMessageConstants.PA_TXT_IMPORT_CONSUMER_SECRET_FROM_FILE;

    private static final String TOOLTIP_IMPORT_CONSUMER_SECRET_FROM_FILE = ComposerWebserviceMessageConstants.PA_TOOLTIP_IMPORT_CONSUMER_SECRET_FROM_FILE;

    private static final String WARNING_UNSUPORTED_PRIVATE_KEY_FILE = ComposerWebserviceMessageConstants.PA_WARNING_UNSUPORTED_PRIVATE_KEY_FILE;

    private static final String LBL_CONSUMER_KEY = ComposerWebserviceMessageConstants.PA_LBL_CONSUMER_KEY;

    private static final String LBL_CONSUMER_SECRET = ComposerWebserviceMessageConstants.PA_LBL_CONSUMER_SECRET;

    private static final String LBL_TOKEN = ComposerWebserviceMessageConstants.PA_LBL_TOKEN;

    private static final String LBL_TOKEN_SECRET = ComposerWebserviceMessageConstants.PA_LBL_TOKEN_SECRET;

    private static final String LBL_REALM = ComposerWebserviceMessageConstants.PA_LBL_REALM;

    private static final String TXT_MSG_OPTIONAL = ComposerWebserviceMessageConstants.PA_TXT_MSG_OPTIONAL;

    private static final String ICON_URI_FOR_PART = "IconUriForPart";

    private static final String RSA_SHA1 = RequestHeaderConstants.SIGNATURE_METHOD_RSA_SHA1;

    private static final String HMAC_SHA1 = RequestHeaderConstants.SIGNATURE_METHOD_HMAC_SHA1;
    
    private static final String SHOW_SNIPPETS = ComposerWebserviceMessageConstants.SHOW_SNIPPETS;
    
    private static final String HIDE_SNIPPETS = ComposerWebserviceMessageConstants.HIDE_SNIPPETS;

    protected static final String OAUTH_1_0 = RequestHeaderConstants.AUTHORIZATION_TYPE_OAUTH_1_0;
    
    private static final int MIN_PART_WIDTH = 400;
    
    private static final InputValueType[] variableInputValueTypes = { InputValueType.String, InputValueType.Number,
            InputValueType.Boolean, InputValueType.Null, InputValueType.GlobalVariable, InputValueType.TestDataValue,
            InputValueType.List, InputValueType.Map };
    
    @Inject
    protected MApplication application;

    @Inject
    protected EModelService modelService;

    @Inject
    protected IEventBroker eventBroker;

    @Inject
    protected IStylingEngine styleEngine;
    
    @Inject
    protected EPartService partService;

    protected MCompositePart mPart;
    
    protected WebServiceRequestEntity originalWsObject;

    protected ScrolledComposite sComposite;

    protected SashForm mainComposite;

    protected Composite userComposite;

    protected Composite oauthComposite;

    protected Composite updateHeaderComposite;

    protected ParameterTable tblParams;

    protected ParameterTable tblHeaders;

    protected List<WebElementPropertyEntity> params = new ArrayList<WebElementPropertyEntity>();

    protected List<WebElementPropertyEntity> httpHeaders = new ArrayList<WebElementPropertyEntity>();

    protected List<WebElementPropertyEntity> tempPropList = new ArrayList<WebElementPropertyEntity>();

    protected WebServiceAPIControl wsApiControl;

    protected SourceViewer requestBody;

    protected MirrorEditor mirrorEditor;

    protected CTabItem tabAuthorization;

    protected CTabItem tabHeaders;

    protected CTabItem tabBody;
    
    protected CTabItem tabVerification;

    protected Composite responseComposite;

    protected CCombo ccbAuthType;

    protected Text txtUsername;

    protected Text txtPassword;

    protected Text txtConsumerKey;

    protected Text txtConsumerSecret;

    protected Text txtToken;

    protected Text txtTokenSecret;

    protected Text txtSignatureMethod;

    protected Text txtRealm;

    protected CCombo ccbOAuth1SignatureMethod;

    protected List<WebElementPropertyEntity> oauth1Headers = new ArrayList<WebElementPropertyEntity>();
    
    protected List<ScriptSnippet> verificationScriptSnippets = new ArrayList<>();
    
    protected ScriptSnippet verificationScriptImport;
    
    @Inject
    protected MDirtyable dirtyable;

    private Label lblStatusCodeDetails, lblReponseTimeDetails, lblReponseLengthDetails;

    protected Composite responseBodyComposite;
    
    protected GroovyEditor verificationScriptEditor;
    
    protected Composite parent;
    
    protected Text txtVerificationLog;

    private MPart scriptEditorPart;
    
    private WSRequestPartUI ui;
    
    protected TestCaseVariableView variableView;
    
    public WebServiceRequestEntity getOriginalWsObject() {
        return originalWsObject;
    }

    public void setOriginalWsObject(WebServiceRequestEntity originalWsObject) {
        this.originalWsObject = originalWsObject;
    }
    
    private Composite responseMessageComposite;

    protected Label lblVerificationResultStatus;

    private Composite verificationResultComposite;

    @PostConstruct
    public void createComposite(Composite parent, MCompositePart part) {
        this.mPart = part;
        new HelpToolBarForMPart(part, DocumentationMessageConstants.TEST_OBJECT_WEB_SERVICES);
        this.originalWsObject = (WebServiceRequestEntity) part.getObject();
        this.parent = parent;
        
        verificationScriptSnippets = VerificationScriptSnippetFactory.getSnippets();
        verificationScriptImport = VerificationScriptSnippetFactory.getSnippetImport();
    }
    
    public Composite getComposite() {
        return parent;
    }
    
    public void initComponents(WSRequestPartUI wsRequestPartUI) {
        this.ui = wsRequestPartUI;
        
        new ToolBarForVerificationPart(ui.getVerificationPart());
        
        scriptEditorPart = ui.getScriptEditorPart();
        verificationScriptEditor = (GroovyEditor)GroovyEditorUtil.getEditor(scriptEditorPart);
        if (StringUtils.isBlank(originalWsObject.getVerificationScript())) {
            insertImportsForVerificationScript();
        }
       
        Composite apiControlsPartComposite = ui.getApiControlsPartComposite();
        Composite apiControlsPartInnerComposite = new Composite(apiControlsPartComposite, SWT.NONE);
        apiControlsPartInnerComposite.setLayout(new GridLayout());
        
        createAPIControls(apiControlsPartInnerComposite);
        
        createParamsComposite(apiControlsPartInnerComposite);
        
        createTabsComposite();
        
        createSnippetComposite();
        
        createVariableComposite();
        
        Composite responsePartComposite = ui.getResponsePartComposite();
        Composite responsePartInnerComposite = new Composite(responsePartComposite, SWT.NONE);

        GridLayout glResponse = new GridLayout(2, false);
        glResponse.marginWidth = glResponse.marginHeight = 0;
        responsePartInnerComposite.setLayout(glResponse);
        
        Label separator = new Label(responsePartInnerComposite, SWT.SEPARATOR | SWT.SHADOW_IN | SWT.VERTICAL);
        separator.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        
        createResponseComposite(responsePartInnerComposite);
        
        responseComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        populateDataToUI();
        registerListeners();
    }

    private void insertImportsForVerificationScript() {
        StringBuilder importBuilder = new StringBuilder()
                .append(verificationScriptImport.getScript())
                .append("\n");

        insertVerificationScript(0, importBuilder.toString());
    }
    
    private void insertVerificationScript(int offset, String script) {
        try {
            GroovyEditorUtil.insertScript(verificationScriptEditor, offset, script);
        } catch (MalformedTreeException e) {
            LoggerSingleton.logError(e);
        } catch (BadLocationException e) {
            LoggerSingleton.logError(e);
        }
    }
    
    protected String getVerificationScript() {
        IEditorInput input = verificationScriptEditor.getEditorInput();
        IDocument document = verificationScriptEditor.getDocumentProvider().getDocument(input);
        if (document != null) {
            String script = document.get();
            return script;
        } else {
            return StringUtils.EMPTY;
        }
    }
    
    protected void executeVerificationScript(ResponseObject responseObject) throws Exception {
        String verificationScript = getVerificationScript();
        VerificationScriptExecutor executor = new VerificationScriptExecutor();
        executor.execute(originalWsObject.getIdForDisplay(), verificationScript, responseObject);
    }


    protected void createAPIControls(Composite parent) {
        String endPoint = isSOAP() ? originalWsObject.getWsdlAddress() : originalWsObject.getRestUrl();
        wsApiControl = new WebServiceAPIControl(parent, isSOAP(), endPoint);
        wsApiControl.addRequestMethodSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (requestBody != null && !isSOAP()) {
                    tabBody.getControl().setEnabled(isBodySupported());
                }
                setDirty();
            }
        });

        wsApiControl.addRequestMethodModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                if (!isSOAP()) {
                    tabBody.getControl().setEnabled(isBodySupported());
                }
                setDirty();
            }
        });

        wsApiControl.addRequestURLModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setDirty();
            }
        });
        
        wsApiControl.addSendSelectionListener(new DropdownToolItemSelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (event.detail == SWT.ARROW) {
                    showDropdown(event);
                } else {
                    sendRequest(false);
                }
            }

            @Override
            protected Menu getMenu() {
                return wsApiControl.getSendMenu();
            }
            
        });
        
        wsApiControl.addSendAndVerifySelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                sendRequest(true);
            }
        });
    }

    protected abstract void sendRequest(boolean runVerificationScript);
    
    protected Map<String, String> evaluateRequestVariables() throws Exception {

        WebServiceRequestEntity requestEntity = getWSRequestObject();
        List<VariableEntity> variables = requestEntity.getVariables();
        Map<String, String> variableMap = variables.stream()
                .collect(Collectors.toMap(VariableEntity::getName, VariableEntity::getDefaultValue));

        VariableEvaluator evaluator = new VariableEvaluator();
        Map<String, String> evaluatedVariables = evaluator.evaluate(originalWsObject.getId(), variableMap);

        return evaluatedVariables;
    }


    protected abstract void createParamsComposite(Composite parent);

    protected ToolBar createAddRemoveToolBar(Composite parent, SelectionListener addSelectionListener,
            SelectionListener removeSelectionListener) {
        ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        ToolItem tiAdd = new ToolItem(toolbar, SWT.RIGHT);
        tiAdd.setText(StringConstants.ADD);
        tiAdd.setImage(ImageManager.getImage(IImageKeys.ADD_16));
        tiAdd.addSelectionListener(addSelectionListener);

        ToolItem tiRemove = new ToolItem(toolbar, SWT.RIGHT);
        tiRemove.setText(StringConstants.REMOVE);
        tiRemove.setImage(ImageManager.getImage(IImageKeys.DELETE_16));
        tiRemove.setDisabledImage(ImageManager.getImage(IImageKeys.DELETE_DISABLED_16));
        tiRemove.setEnabled(false);
        tiRemove.addSelectionListener(removeSelectionListener);
        return toolbar;
    }

    protected void createTabsComposite() {
        CTabFolder tabFolder = ui.getTabFolder();
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        styleEngine.setId(tabFolder.getParent(), "DefaultCTabFolder");
//        styleEngine.setId(parent, "DefaultCTabFolder");

        addTabAuthorization(tabFolder);
        addTabHeaders(tabFolder);
        addTabBody(tabFolder);
//        addTabVerification(tabFolder);

        tabFolder.setSelection(0);
    }
    
    private void createVariableComposite() {
        Composite variablePartComposite = ui.getVariablePartComposite();
        
        variableView = new TestCaseVariableView(this);
        variableView.setInputValueTypes(variableInputValueTypes);
        variableView.createComponents(variablePartComposite);
        
        // hide "Masked" column
        TableColumn[] tableColumns = variableView.getTableViewer().getTable().getColumns();
        for (TableColumn tableColumn : tableColumns) {
            if (ComposerTestcaseMessageConstants.PA_COL_MASKED.equals(tableColumn.getText())) {
                tableColumn.setWidth(0);
                tableColumn.setResizable(false);
            }
        }
        
        List<VariableEntity> variables = originalWsObject.getVariables();
        variableView.addVariable(variables.toArray(new VariableEntity[variables.size()]));
    }
    
    @Override
    public void setDirty(boolean isDirty) {
        this.setDirty();
    }
    
    @Override
    public void addVariables(VariableEntity[] variables) {
        variableView.addVariable(variables);
    }
    
    @Override
    public VariableEntity[] getVariables() {
        return variableView.getVariables();
    }
    
    @Override
    public void deleteVariables(List<VariableEntity> variableList) {
        
    }

    private void createSnippetComposite() {
  
//        FontData[] fontData = parent.getFont().getFontData();
//        fontData[0].setHeight(8);
//        parent.setFont(new Font(Display.getCurrent(), fontData));
//        parent.setLayout(new GridLayout(2, false));
        Composite snippetPartComposite = ui.getSnippetPartComposite();
        
        Composite snippetPartInnerComposite = new Composite(snippetPartComposite, SWT.NONE);
        snippetPartInnerComposite.setLayout(new GridLayout(2, false));
        
        int fontSize = Platform.getOS().equals(Platform.OS_MACOSX) ? 11 : 9;
        
        Label separator = new Label(snippetPartInnerComposite, SWT.SEPARATOR | SWT.SHADOW_IN | SWT.VERTICAL);
        separator.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        
        Composite snippetComposite = new Composite(snippetPartInnerComposite, SWT.NONE);
        snippetComposite.setLayout(new GridLayout());
        snippetComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Label lblInstruction = new Label(snippetComposite, SWT.WRAP);
        lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        ControlUtils.setFontSize(lblInstruction, fontSize);
        lblInstruction.setText(ComposerWebserviceMessageConstants.LBL_VERIFICATION_INSTRUCTION);
        
        Composite headingComposite = new Composite(snippetComposite, SWT.NONE);
        headingComposite.setLayout(new GridLayout(2, false));
        
       
        CLabel lblSnippetHeading = new CLabel(headingComposite, SWT.NONE);
        lblSnippetHeading.setTopMargin(10);
        lblSnippetHeading.setRightMargin(0);
        ControlUtils.setFontSize(lblSnippetHeading, fontSize);
        ControlUtils.setFontToBeBold(lblSnippetHeading);
        lblSnippetHeading.setText(ComposerWebserviceMessageConstants.LBL_SNIPPET_HEADING);
        
        CLabel lblHelp = new CLabel(headingComposite, SWT.NONE);
        lblHelp.setImage(ImageManager.getImage(IImageKeys.HELP_16));
        lblHelp.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
        lblHelp.setTopMargin(10);
        lblHelp.setLeftMargin(0);
        lblHelp.addListener(SWT.MouseDown, e -> {
            Program.launch("https://docs.katalon.com/x/EwjR");
        });
        
        ScrolledComposite scrolledComposite = new ScrolledComposite(snippetComposite, SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledComposite.setBackground(parent.getBackground());
        
        Composite composite = new Composite(scrolledComposite, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        composite.setBackground(parent.getBackground());
        
        Color snippetLblColor = new Color(composite.getDisplay(), 0, 0, 255);
        for (ScriptSnippet snippet : verificationScriptSnippets) {
            CLabel lblSnippet = new CLabel(composite, SWT.WRAP);
            ControlUtils.setFontSize(lblSnippet, fontSize);
            lblSnippet.setForeground(snippetLblColor);
            lblSnippet.setText(snippet.getName());
            lblSnippet.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
            lblSnippet.setBackground(parent.getBackground());
            lblSnippet.setBottomMargin(5);
            lblSnippet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            lblSnippet.addListener(SWT.MouseDown, e -> {
                IEditorInput editorInput = verificationScriptEditor.getEditorInput();
                IDocument document = verificationScriptEditor.getDocumentProvider().getDocument(editorInput);
                
                StringBuilder scriptBuilder = new StringBuilder("\n").append(snippet.getScript()).append("\n");
                
                insertVerificationScript(document.getLength(), scriptBuilder.toString());
            });
        }
        snippetLblColor.dispose();
        
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolledComposite.setContent(composite);
    }

    protected CTabItem createTab(CTabFolder parent, CTabItem tab, String title) {
        tab = new CTabItem(parent, SWT.NONE);
        tab.setText(title);

        Composite tabComposite = new Composite(parent, SWT.NONE);
        tabComposite.setLayout(new GridLayout());
        tab.setControl(tabComposite);

        return tab;
    }

    protected void addTabAuthorization(CTabFolder parent) {
        tabAuthorization = ui.getAuthorizationTab();

        Composite authorizationPartComposite = ui.getAuthorizationPartComposite();
        Composite formComposite = new Composite(authorizationPartComposite, SWT.NONE);
        formComposite.setLayout(new GridLayout(2, false));
        GridData gdForm = new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1);
        gdForm.widthHint = 400;
        formComposite.setLayoutData(gdForm);

        Label lblAuthType = new Label(formComposite, SWT.NONE);
        lblAuthType.setText(StringConstants.TYPE);
        GridData gdLblAuthType = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblAuthType.widthHint = AUTH_LBL_WIDTH;
        lblAuthType.setLayoutData(gdLblAuthType);

        ccbAuthType = new CCombo(formComposite, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);
        GridData gdCcbAuthType = new GridData(SWT.FILL, SWT.FILL, true, false);
        gdCcbAuthType.widthHint = AUTH_FIELD_WIDTH;
        gdCcbAuthType.heightHint = 20;
        ccbAuthType.setLayoutData(gdCcbAuthType);
        ccbAuthType.add(NO_AUTH);
        ccbAuthType.add(BASIC_AUTH);
        ccbAuthType.add(OAUTH_1_0);

        userComposite = new Composite(formComposite, SWT.NONE);
        oauthComposite = new Composite(formComposite, SWT.NONE);
        updateHeaderComposite = new Composite(formComposite, SWT.NONE);

        createBasicAuthInput(userComposite);
        createOAuth1Input(oauthComposite);
        createUpdateHeaderButton(updateHeaderComposite);

        ccbAuthType.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                renderAuthenticationUI(ccbAuthType.getText());
            }
        });
        
        ccbAuthType.select(0);
        renderAuthenticationUI(ccbAuthType.getText());
    }

    /**
     * @param composite Composite with GridData layout
     * @param isVisible
     */
    protected void setCompositeVisible(Composite composite, boolean isVisible) {
        composite.setVisible(isVisible);
        GridData gridData = (GridData) composite.getLayoutData();
        gridData.exclude = !isVisible;
        Composite parent = composite.getParent();
        parent.layout(true, true);
        parent.pack();
    }

    private void createBasicAuthInput(Composite parent) {
        GridLayout glUserComposite = new GridLayout(2, false);
        glUserComposite.marginWidth = 0;
        glUserComposite.marginHeight = 0;
        parent.setLayout(glUserComposite);
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Label lblUsername = new Label(parent, SWT.NONE);
        lblUsername.setText(ComposerWebserviceMessageConstants.LBL_USERNAME);
        GridData gdLblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblUsername.widthHint = AUTH_LBL_WIDTH;
        lblUsername.setLayoutData(gdLblUsername);

        txtUsername = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData gdTxtUsername = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdTxtUsername.widthHint = AUTH_FIELD_WIDTH;
        txtUsername.setLayoutData(gdTxtUsername);

        Label lblPassword = new Label(parent, SWT.NONE);
        lblPassword.setText(ComposerWebserviceMessageConstants.LBL_PASSWORD);
        GridData gdLblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblPassword.widthHint = AUTH_LBL_WIDTH;
        lblPassword.setLayoutData(gdLblPassword);

        txtPassword = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData gdTxtPassword = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdTxtPassword.widthHint = AUTH_FIELD_WIDTH;
        txtPassword.setLayoutData(gdTxtPassword);
        txtPassword.setEchoChar(PASSWORD_CHAR_MASK);

        new Label(parent, SWT.NONE);
        final Button chkShowPassword = new Button(parent, SWT.CHECK);
        chkShowPassword.setText(ComposerWebserviceMessageConstants.CHK_SHOW_PASSWORD);
        chkShowPassword.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                txtPassword.setEchoChar(PASSWORD_CHAR_MASK); // show as dot
                if (chkShowPassword.getSelection()) {
                    txtPassword.setEchoChar(RAW_PASSWORD_CHAR_MASK); // show the text
                }
            }
        });
    }

    private void createOAuth1Input(Composite parent) {
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        parent.setLayout(gl);
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        txtConsumerKey = addAuthInput(LBL_CONSUMER_KEY, txtConsumerKey, parent, null);

        Label lblConsumerSecret = new Label(parent, SWT.NONE);
        lblConsumerSecret.setText(LBL_CONSUMER_SECRET);
        GridData gdLblConsumerSecret = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2);
        gdLblConsumerSecret.widthHint = AUTH_LBL_WIDTH;
        lblConsumerSecret.setLayoutData(gdLblConsumerSecret);

        txtConsumerSecret = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gdTxtConsumerSecret = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdTxtConsumerSecret.widthHint = AUTH_FIELD_WIDTH;
        gdTxtConsumerSecret.heightHint = 80;
        txtConsumerSecret.setLayoutData(gdTxtConsumerSecret);
        txtConsumerSecret.setToolTipText(TOOLTIP_CONSUMER_SECRET);

        Button btnLoadSecretFromFile = new Button(parent, SWT.FLAT);
        btnLoadSecretFromFile.setText(TXT_IMPORT_CONSUMER_SECRET_FROM_FILE);
        btnLoadSecretFromFile.setToolTipText(TOOLTIP_IMPORT_CONSUMER_SECRET_FROM_FILE);
        btnLoadSecretFromFile.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Shell activeShell = Display.getCurrent().getActiveShell();
                FileDialog dialog = new FileDialog(activeShell);
                dialog.setFilterPath(ProjectController.getInstance().getCurrentProject().getFolderLocation());
                String filePath = dialog.open();
                if (StringUtils.isEmpty(filePath)) {
                    return;
                }
                try {
                    String fileContent = FileUtils.readFileToString(new File(filePath));
                    if (txtConsumerSecret == null || fileContent == null) {
                        return;
                    }
                    if (RSA_SHA1.equals(ccbOAuth1SignatureMethod.getText())
                            && !(StringUtils.contains(fileContent, PrivateKeyReader.P1_BEGIN_MARKER)
                                    || StringUtils.contains(fileContent, PrivateKeyReader.P8_BEGIN_MARKER))) {
                        MessageDialog.openWarning(activeShell, StringConstants.WARN,
                                WARNING_UNSUPORTED_PRIVATE_KEY_FILE);
                        return;
                    }
                    txtConsumerSecret.setText(fileContent);
                } catch (IOException ex) {
                    LoggerSingleton.logError(ex);
                }
            }
        });

        Label lblSignatureMethod = new Label(parent, SWT.NONE);
        lblSignatureMethod.setText(LBL_SIGNATURE_METHOD);
        GridData gdLblSignatureMethod = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblSignatureMethod.widthHint = AUTH_LBL_WIDTH;
        lblSignatureMethod.setLayoutData(gdLblSignatureMethod);

        ccbOAuth1SignatureMethod = new CCombo(parent, SWT.FLAT | SWT.READ_ONLY | SWT.BORDER);
        GridData gdCcbSignatureMethod = new GridData(SWT.FILL, SWT.FILL, true, false);
        gdCcbSignatureMethod.widthHint = AUTH_FIELD_WIDTH;
        gdCcbSignatureMethod.heightHint = 20;
        ccbOAuth1SignatureMethod.setLayoutData(gdCcbSignatureMethod);
        ccbOAuth1SignatureMethod.add(HMAC_SHA1);
        ccbOAuth1SignatureMethod.add(RSA_SHA1);
        ccbOAuth1SignatureMethod.select(0);

        txtToken = addAuthInput(LBL_TOKEN, txtToken, parent, null);
        txtTokenSecret = addAuthInput(LBL_TOKEN_SECRET, txtTokenSecret, parent, null);
        txtRealm = addAuthInput(LBL_REALM, txtRealm, parent, TXT_MSG_OPTIONAL);
    }

    private Text addAuthInput(String label, Text txtField, Composite parent, String placeholder) {
        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText(label);
        GridData gdLbl = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLbl.widthHint = AUTH_LBL_WIDTH;
        lbl.setLayoutData(gdLbl);

        txtField = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData gdTxt = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdTxt.widthHint = AUTH_FIELD_WIDTH;
        txtField.setLayoutData(gdTxt);
        if (placeholder != null) {
            txtField.setMessage(placeholder);
            txtField.setToolTipText(placeholder);
        }
        return txtField;
    }

    private void createUpdateHeaderButton(Composite parent) {
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        parent.setLayout(gl);
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Label lbl = new Label(parent, SWT.NONE);
        GridData gdLbl = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLbl.widthHint = AUTH_LBL_WIDTH;
        lbl.setLayoutData(gdLbl);

        Button btnUpdateHeader = new Button(parent, SWT.FLAT);
        btnUpdateHeader.setText(ComposerWebserviceMessageConstants.BTN_UPDATE_TO_HEADERS);
        btnUpdateHeader.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // Update authorization to header
                String authType = ccbAuthType.getText();
                if (tblHeaders.deleteRowByColumnValue(0, HTTP_HEADER_AUTHORIZATION)) {
                    tblHeaders.refresh();
                    setDirty();
                }

                if (BASIC_AUTH.equals(authType)) {
                    removeOAuth1Headers();
                    tblHeaders.addRow(createBasicAuthHeaderElement());
                    return;
                }

                if (OAUTH_1_0.equals(authType)) {
                    createOAuth1Headers(txtConsumerKey.getText(), txtConsumerSecret.getText(), txtToken.getText(),
                            txtTokenSecret.getText(), ccbOAuth1SignatureMethod.getText(), txtRealm.getText());
                    return;
                }

                // No authorization
                removeOAuth1Headers();
                tblHeaders.refresh();
            }
        });
    }

    protected void addTabHeaders(CTabFolder parent) {
        tabHeaders = ui.getHeadersTab();
        Composite headersPartComposite = ui.getHeadersPartComposite();
        Composite headersPartInnerComposite = new Composite(headersPartComposite, SWT.NONE);
        headersPartInnerComposite.setLayout(new GridLayout());
        ToolBar toolbar = createAddRemoveToolBar(headersPartInnerComposite, new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                tblHeaders.addRow();
            }
        }, new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                tblHeaders.deleteSelections();
            }
        });

        tblHeaders = createKeyValueTable(headersPartInnerComposite, true);
        tblHeaders.setInput(httpHeaders);
        tblHeaders.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                toolbar.getItem(1).setEnabled(tblHeaders.getTable().getSelectionCount() > 0);
            }
        });
    }

    protected void addTabBody(CTabFolder parent) {
        tabBody = ui.getBodyTab();
    }
    
    protected void addTabVerification(CTabFolder parent) {
        tabVerification = ui.getVerificationTab();
    }

    protected void createResponseComposite(Composite parent) {
        
        responseComposite = new Composite(parent, SWT.NONE);
        responseComposite.setLayout(new GridLayout());
        responseComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label lblResponse = new Label(responseComposite, SWT.NONE);
        lblResponse.setText(ComposerWebserviceMessageConstants.TAB_RESPONSE);
        ControlUtils.setFontToBeBold(lblResponse);

        createResponseStatusComposite();

        createResponseDetailsTabs();
        
        responseMessageComposite = new Composite(parent, SWT.NONE);
        GridLayout glMessageComposite = new GridLayout();
        glMessageComposite.marginTop = 20;
        responseMessageComposite.setLayout(glMessageComposite);
        responseMessageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Label lblSendingRequest = new Label(responseMessageComposite, SWT.NONE);
        lblSendingRequest.setText(ComposerWebserviceMessageConstants.LBL_SENDING_REQUEST);
        ControlUtils.setFontToBeBold(lblSendingRequest);
        
        displayResponseContentBasedOnSendingState(false);
    }

    private void createResponseDetailsTabs() {
        CTabFolder reponseDetailsTabFolder = new CTabFolder(responseComposite, SWT.NONE);
        reponseDetailsTabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        styleEngine.setId(responseComposite, "DefaultCTabFolder");

        createResponseBody(reponseDetailsTabFolder);

        createResponseHeader(reponseDetailsTabFolder);
        
        createResponseVerificationResult(reponseDetailsTabFolder);

//        CTabItem responseVerificationLogTab = new CTabItem(reponseDetailsTabFolder, SWT.NONE);
//        responseVerificationLogTab.setText(ComposerWebserviceMessageConstants.TAB_VERIFICATION_LOG);
//
        reponseDetailsTabFolder.setSelection(0);
    }

    private void createResponseHeader(CTabFolder reponseDetailsTabFolder) {
        CTabItem responseHeaderTab = new CTabItem(reponseDetailsTabFolder, SWT.NONE);
        responseHeaderTab.setText(ComposerWebserviceMessageConstants.LBL_RESPONSE_HEADER);

        Composite responseHeaderComposite = new Composite(reponseDetailsTabFolder, SWT.NONE);
        responseHeaderTab.setControl(responseHeaderComposite);
        GridLayout glHeader = new GridLayout();
        glHeader.marginWidth = glHeader.marginHeight = 0;
        responseHeaderComposite.setLayout(glHeader);
        mirrorEditor = new MirrorEditor(responseHeaderComposite, SWT.NONE);
        mirrorEditor.setEditable(false);
    }

    private void createResponseBody(CTabFolder reponseDetailsTabFolder) {
        CTabItem responseBodyTab = new CTabItem(reponseDetailsTabFolder, SWT.NONE);
        responseBodyTab.setText(ComposerWebserviceMessageConstants.LBL_RESPONSE_BODY);

        responseBodyComposite = new Composite(reponseDetailsTabFolder, SWT.NONE);
        responseBodyTab.setControl(responseBodyComposite);
        GridLayout glBody = new GridLayout();
        glBody.marginWidth = glBody.marginHeight = 0;
        responseBodyComposite.setLayout(glBody);
    }
    
    private void createResponseVerificationResult(CTabFolder responseDetailsTabFolder) {
        CTabItem verificationResultTab = new CTabItem(responseDetailsTabFolder, SWT.NONE);
        verificationResultTab.setText(ComposerWebserviceMessageConstants.LBL_RESPONSE_VERIFICATION_LOG);
        
        verificationResultComposite = new Composite(responseDetailsTabFolder, SWT.NONE);
        verificationResultTab.setControl(verificationResultComposite);
        
        GridLayout glVerificationResult = new GridLayout();
        glVerificationResult.marginWidth = glVerificationResult.marginHeight = 0;
        verificationResultComposite.setLayout(glVerificationResult);
        
        Composite resultStatusComposite = new Composite(verificationResultComposite, SWT.NONE);
        GridLayout glResultStatus = new GridLayout(2, false);
        resultStatusComposite.setLayout(glResultStatus);
        
        Label lblVerificationResult = new Label(resultStatusComposite, SWT.NONE);
        lblVerificationResult.setText(ComposerWebserviceMessageConstants.LBL_RESPONSE_VERIFICATION_RESULT);
        
        lblVerificationResultStatus = new Label(resultStatusComposite, SWT.NONE);
        lblVerificationResultStatus.setForeground(ColorUtil.getTextWhiteColor());
        
        txtVerificationLog = new Text(verificationResultComposite, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        txtVerificationLog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        txtVerificationLog.setEditable(false);
        txtVerificationLog.setBackground(ColorUtil.getWhiteBackgroundColor());
        txtVerificationLog.setFont(FONT_CONSOLAS_10);
    }

    private void createResponseStatusComposite() {
        Composite responseStatusComposite = new Composite(responseComposite, SWT.NONE);
        responseStatusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout glResponseStatus = new GridLayout(3, false);
        glResponseStatus.marginWidth = 0;
        responseStatusComposite.setLayout(glResponseStatus);

        Composite statusCodeComposite = new Composite(responseStatusComposite, SWT.NONE);
        statusCodeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout glStatusCode = new GridLayout(2, false);
        glStatusCode.marginWidth = glStatusCode.marginHeight = 0;
        glStatusCode.horizontalSpacing = 2;
        statusCodeComposite.setLayout(glStatusCode);

        Label lblStatusCode = new Label(statusCodeComposite, SWT.NONE);
        lblStatusCode.setText(StringConstants.STATUS + StringConstants.CR_COLON);

        lblStatusCodeDetails = new Label(statusCodeComposite, SWT.NONE);
        lblStatusCodeDetails.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        lblStatusCodeDetails.setForeground(ColorUtil.getTextWhiteColor());
        Cursor cursor = new Cursor(lblStatusCodeDetails.getDisplay(), SWT.CURSOR_HAND);
        lblStatusCodeDetails.setCursor(cursor);
        lblStatusCodeDetails.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                Program.launch(StringConstants.PA_URL_W3_HTTP_STATUS);
            }
        });

        Composite reponseTimeComposite = new Composite(responseStatusComposite, SWT.NONE);
        reponseTimeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout glResponseTime = new GridLayout(2, false);
        glResponseTime.marginWidth = glResponseTime.marginHeight = 0;
        glResponseTime.horizontalSpacing = 2;
        reponseTimeComposite.setLayout(glResponseTime);

        Label lblReponseTime = new Label(reponseTimeComposite, SWT.NONE);
        lblReponseTime.setText(StringConstants.ELAPSED + StringConstants.CR_COLON);

        lblReponseTimeDetails = new Label(reponseTimeComposite, SWT.NONE);
        lblReponseTimeDetails.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        lblReponseTimeDetails.setForeground(ColorUtil.getTextLinkColor());

        Composite reponseLengthComposite = new Composite(responseStatusComposite, SWT.NONE);
        reponseLengthComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout glResponseLength = new GridLayout(2, false);
        glResponseLength.marginWidth = glResponseLength.marginHeight = 0;
        glResponseLength.horizontalSpacing = 2;
        reponseLengthComposite.setLayout(glResponseLength);

        Label lblReponseLength = new Label(reponseLengthComposite, SWT.NONE);
        lblReponseLength.setText(StringConstants.SIZE + StringConstants.CR_COLON);

        lblReponseLengthDetails = new Label(reponseLengthComposite, SWT.NONE);
        lblReponseLengthDetails.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        lblReponseLengthDetails.setForeground(ColorUtil.getTextLinkColor());
    }

    protected void displayResponseContentBasedOnSendingState(boolean isSendingRequest) {
        GridData gdResponseMessageComposite = (GridData) responseMessageComposite.getLayoutData();
        GridData gdResponseComposite = (GridData) responseComposite.getLayoutData();
        
        if (isSendingRequest) {
            gdResponseMessageComposite.exclude = false;
            responseMessageComposite.setVisible(true);
            gdResponseComposite.exclude = true;
            responseComposite.setVisible(false);
        } else {
            gdResponseMessageComposite.exclude = true;
            responseMessageComposite.setVisible(false);
            gdResponseComposite.exclude = false;
            responseComposite.setVisible(true);
        }
        
        responseComposite.getParent().requestLayout();
    }
    
    protected void setResponseStatus(ResponseObject responseObject) {
        int statusCode = responseObject.getStatusCode();
        lblStatusCodeDetails.setBackground(getBackgroundColorForStatusCode(statusCode));
        lblStatusCodeDetails.setText(String.format("%d %s", statusCode,
                EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH)));
        lblReponseLengthDetails.setText(FileUtils.byteCountToDisplaySize(responseObject.getResponseSize()));
        lblReponseTimeDetails.setText(Long.toString(responseObject.getElapsedTime()) + " ms");
        responseComposite.layout(true, true);
    }

    private Color getBackgroundColorForStatusCode(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return ColorUtil.getPassedLogBackgroundColor();
        }
        if (statusCode >= 300 && statusCode < 400) {
            return ColorUtil.getWarningLogBackgroundColor();
        }
        return ColorUtil.getErrorLogBackgroundColor();
    }
    
    protected void clearPreviousResponse() {
        mirrorEditor.setText("");
        
        txtVerificationLog.setText("");
        
        lblVerificationResultStatus.setText("");
    }

    protected ParameterTable createKeyValueTable(Composite containerComposite, boolean isHttpHeader) {
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        Composite compositeTableDetails = new Composite(containerComposite, SWT.NONE);
        GridData gdData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdData.heightHint = 150;
        compositeTableDetails.setLayoutData(gdData);
        compositeTableDetails.setLayout(tableColumnLayout);

        final ParameterTable tblNameValue = new ParameterTable(compositeTableDetails,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.NO_SCROLL | SWT.V_SCROLL, dirtyable) {
            @Override
            public void deleteSelections() {
                if (!isHttpHeader) {
                    deleteSelectedParams();
                } else {
                    super.deleteSelections();
                }
            }
        };
        tblNameValue.createTableEditor();

        Table table = tblNameValue.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        // Double click to add new property
        table.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(org.eclipse.swt.widgets.Event event) {
                tblNameValue.addRow();
            }
        });

        TableViewerColumn tvcName = new TableViewerColumn(tblNameValue, SWT.NONE);
        tvcName.getColumn().setText(ParameterTable.columnNames[0]);
        tvcName.getColumn().setWidth(400);
        tvcName.setEditingSupport(new PropertyNameEditingSupport(tblNameValue, dirtyable, isHttpHeader) {
            @Override
            protected void setValue(Object element, Object value) {
                if (!isHttpHeader) {
                    handleRequestParamNameChanged(element, value);
                } else {
                    super.setValue(element, value);
                }
            }
        });
        tvcName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((WebElementPropertyEntity) element).getName();
            }
        });
        tableColumnLayout.setColumnData(tvcName.getColumn(), new ColumnWeightData(30));

        TableViewerColumn tvcValue = new TableViewerColumn(tblNameValue, SWT.NONE);
        tvcValue.getColumn().setText(ParameterTable.columnNames[1]);
        tvcValue.getColumn().setWidth(500);
        tvcValue.setEditingSupport(new PropertyValueEditingSupport(tblNameValue, dirtyable, isHttpHeader) {
            @Override
            protected void setValue(Object element, Object value) {
                if (!isHttpHeader) {
                    handleRequestParamValueChanged(element, value);
                } else {
                    super.setValue(element, value);
                }
            }
        });
        tvcValue.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((WebElementPropertyEntity) element).getValue();
            }
        });
        tableColumnLayout.setColumnData(tvcValue.getColumn(), new ColumnWeightData(60));

        tblNameValue.setContentProvider(ArrayContentProvider.getInstance());

        // Set tooltip for table
        DefaultToolTip toolTip = new DefaultToolTip(tblNameValue.getControl(), ToolTip.RECREATE, false);
        toolTip.setText(StringConstants.PA_TOOLTIP_DOUBLE_CLICK_FOR_QUICK_INSERT);
        toolTip.setPopupDelay(0);
        toolTip.setShift(new Point(15, 0));

        return tblNameValue;
    }

    protected void handleRequestParamNameChanged(Object element, Object value) {
    };

    protected void handleRequestParamValueChanged(Object element, Object value) {
    };

    protected void deleteSelectedParams() {
    };

    protected SourceViewer createSourceViewer(Composite parent, GridData layoutData) {
        CompositeRuler ruler = new CompositeRuler();
        LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
        lineNumberRulerColumn.setBackground(ColorUtil.getDefaultBackgroundColor());
        ruler.addDecorator(0, lineNumberRulerColumn);

        SourceViewer sv = new SourceViewer(parent, ruler, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
        if (layoutData != null) {
            layoutData.heightHint = 200;
            sv.getControl().setLayoutData(layoutData);
        }
        sv.canDoOperation(SourceViewer.UNDO);
        sv.canDoOperation(SourceViewer.REDO);
        sv.canDoOperation(SourceViewer.CUT);
        sv.canDoOperation(SourceViewer.COPY);
        sv.canDoOperation(SourceViewer.PASTE);
        sv.canDoOperation(SourceViewer.DELETE);
        sv.canDoOperation(SourceViewer.SELECT_ALL);
        sv.showAnnotations(true);
        sv.showAnnotationsOverview(true);
        StyledText textWidget = sv.getTextWidget();
        textWidget.setFont(FONT_COURIER_NEW_12);
        Menu contextMenu = new Menu(textWidget);
        MenuItem miCut = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(StringConstants.CUT, new String[] { IKeyLookup.M1_NAME, "X" }), sv,
                SourceViewer.CUT);
        MenuItem miCopy = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(StringConstants.COPY, new String[] { IKeyLookup.M1_NAME, "C" }), sv,
                SourceViewer.COPY);
        MenuItem miPaste = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(StringConstants.PASTE, new String[] { IKeyLookup.M1_NAME, "V" }), sv,
                SourceViewer.PASTE);
        MenuItem miDelete = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(StringConstants.DELETE, new String[] { IKeyLookup.DELETE_NAME }), sv,
                SourceViewer.DELETE);
        new MenuItem(contextMenu, SWT.SEPARATOR);
        MenuItem miUndo = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(GlobalMessageConstants.UNDO, new String[] { IKeyLookup.M1_NAME, "Z" }), sv,
                SourceViewer.UNDO);
        MenuItem miRedo = createContextMenuItem(contextMenu, getLabelWithHotKeys(GlobalMessageConstants.REDO,
                new String[] { IKeyLookup.M1_NAME, IKeyLookup.SHIFT_NAME, "Z" }), sv, SourceViewer.REDO);
        new MenuItem(contextMenu, SWT.SEPARATOR);
        MenuItem miSelectAll = createContextMenuItem(contextMenu,
                getLabelWithHotKeys(GlobalMessageConstants.SELECT_ALL, new String[] { IKeyLookup.M1_NAME, "A" }), sv,
                SourceViewer.SELECT_ALL);

        textWidget.setMenu(contextMenu);
        textWidget.addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent e) {
                boolean hasTextSelected = textWidget.isTextSelected();
                boolean isEditable = textWidget.getEditable();
                boolean isCutDeleteAllowed = hasTextSelected && isEditable;
                miCut.setEnabled(isCutDeleteAllowed);
                miCopy.setEnabled(hasTextSelected);
                miPaste.setEnabled(isEditable);
                miDelete.setEnabled(isCutDeleteAllowed);
                miUndo.setEnabled(isEditable);
                miRedo.setEnabled(isEditable);
                miSelectAll.setEnabled(!textWidget.getText().isEmpty());
            }
        });

        textWidget.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (!sv.isEditable() || !textWidget.isFocusControl()) {
                    return;
                }

                if (KeyEventUtil.isKeysPressed(e, new String[] { IKeyLookup.M1_NAME, "Z" })) {
                    sv.doOperation(SourceViewer.UNDO);
                    return;
                }

                if (KeyEventUtil.isKeysPressed(e, new String[] { IKeyLookup.M1_NAME, IKeyLookup.SHIFT_NAME, "Z" })) {
                    sv.doOperation(SourceViewer.REDO);
                }
            }
        });
        sv.setTabsToSpacesConverter(new IAutoEditStrategy() {

            @Override
            public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
                if (command.text.equals("\t")) {
                    command.text = TAB_SPACE;
                }
            }
        });
        return sv;
    }

    protected MenuItem createContextMenuItem(Menu parent, String label, SourceViewer sv, int operation) {
        MenuItem menuItem = new MenuItem(parent, SWT.PUSH);
        menuItem.setText(label);
        menuItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                sv.doOperation(operation);
            }
        });
        return menuItem;
    }

    protected String getLabelWithHotKeys(String label, String[] keys) {
        return label + "\t" + KeyEventUtil.geNativeKeyLabel(keys);
    }

    protected boolean warningIfBodyNotEmpty() {
        // if (StringUtils.isNotEmpty(requestBody.getDocument().get())) {
        // return MessageDialog.openConfirm(null, StringConstants.WARN,
        // ComposerWebserviceMessageConstants.PART_WARNING_MSG_BODY_CONTENT_WILL_BE_OVERWRITTEN);
        // }
        return true;
    }

    protected void registerListeners() {
        eventBroker.subscribe(EventConstants.TEST_OBJECT_UPDATED, this);
        eventBroker.subscribe(EventConstants.EXPLORER_REFRESH_SELECTED_ITEM, this);
        eventBroker.subscribe(EventConstants.WS_VERIFICATION_LOG_UPDATED, this);
        eventBroker.subscribe(EventConstants.WS_VERIFICATION_EXECUTION_FINISHED, this);
        
        IFileEditorInput editorInput = (IFileEditorInput) verificationScriptEditor.getEditorInput();
        verificationScriptEditor.getDocumentProvider()
            .getDocument(editorInput)
            .addDocumentListener(new IDocumentListener() {
                @Override
                public void documentChanged(DocumentEvent event) {
                   GroovyEditorUtil.showProblems(verificationScriptEditor);
                   WebServicePart.this.dirtyable.setDirty(true);
                }
    
                @Override
                public void documentAboutToBeChanged(DocumentEvent event) {
                    // TODO Auto-generated method stub
                }
            });
        
//        verificationScriptEditor.addPropertyListener(new IPropertyListener() {
//            @Override
//            public void propertyChanged(Object source, int propId) {
//                if (source instanceof GroovyEditor && propId == ISaveablePart.PROP_DIRTY) {
//                    if (verificationScriptEditor.isDirty()) {
//                        WebServicePart.this.dirtyable.setDirty(true);
//                        tabVerification.setText(StringConstants.PA_LBL_VERIFICATION);
//                    } 
//                }
//            }
//        });
    }

    @Override
    public void handleEvent(Event event) {
        Object eventData = EventUtil.getData(event);
        if (EventConstants.TEST_OBJECT_UPDATED.equals(event.getTopic())) {
            if (!(eventData instanceof Object[])) {
                return;
            }

            Object[] data = (Object[]) eventData;
            String elementId = EntityPartUtil.getTestObjectPartId((String) data[0]);
            if (!StringUtils.equalsIgnoreCase(elementId, mPart.getElementId())) {
                return;
            }

            WebServiceRequestEntity webElement = (WebServiceRequestEntity) data[1];
            mPart.setLabel(webElement.getName());
            mPart.setElementId(EntityPartUtil.getTestObjectPartId(webElement.getId()));
            populateDataToUI();
            return;
        }

        if (EventConstants.EXPLORER_REFRESH_SELECTED_ITEM.equals(event.getTopic())) {
            try {
                if (!(eventData instanceof ITreeEntity)) {
                    return;
                }

                ObjectRepositoryController toController = ObjectRepositoryController.getInstance();
                if (eventData instanceof WebElementTreeEntity) {
                    WebElementTreeEntity testObjectTreeEntity = (WebElementTreeEntity) eventData;
                    WebServiceRequestEntity wsObject = (WebServiceRequestEntity) testObjectTreeEntity.getObject();
                    if (wsObject != null && wsObject.getId().equals(originalWsObject.getId())) {
                        if (toController.getWebElement(wsObject.getId()) == null) {
                            dispose();
                            return;
                        }

                        if (!dirtyable.isDirty()) {
                            originalWsObject = wsObject;
                            populateDataToUI();
                        }
                        return;
                    }

                    if (toController.getWebElement(originalWsObject.getId()) == null) {
                        dispose();
                    }
                    return;
                }

                if (eventData instanceof FolderTreeEntity) {
                    FolderEntity folder = (FolderEntity) ((ITreeEntity) eventData).getObject();
                    if (folder == null
                            || !FolderController.getInstance().isFolderAncestorOfEntity(folder, originalWsObject)) {
                        return;
                    }

                    if (toController.getWebElement(originalWsObject.getId()) == null) {
                        dispose();
                    }
                }
            } catch (Exception e) {
                LoggerSingleton.logError(e);
            }
        }
        
        if (EventConstants.WS_VERIFICATION_LOG_UPDATED.equals(event.getTopic())) {
            Object[] data = (Object[]) eventData;
            String testObjectId = (String) data[0];
            String logLine = (String) data[1];
            if (originalWsObject.getIdForDisplay().equals(testObjectId)) {
                txtVerificationLog.append(logLine + "\n");
            }
        }
        
        if (EventConstants.WS_VERIFICATION_EXECUTION_FINISHED.equals(event.getTopic())) {
            Object[] data = (Object[]) eventData;
            String testObjectId = (String) data[0];
            TestStatusValue testStatusValue = (TestStatusValue) data[1];
            if (originalWsObject.getIdForDisplay().equals(testObjectId)) {
                setVerificationResultStatus(testStatusValue);
            }
        }
    }
    
    private void setVerificationResultStatus(TestStatusValue value) {
        lblVerificationResultStatus.setText(value.toString());
        lblVerificationResultStatus.setBackground(getBackgroundColorForVerificationResultStatus(value));
        verificationResultComposite.layout(true, true);
    }

    private Color getBackgroundColorForVerificationResultStatus(TestStatusValue value) {
        if (TestStatusValue.PASSED.equals(value)) {
            return ColorUtil.getPassedLogBackgroundColor();
        } else if (TestStatusValue.WARNING.equals(value)) {
            return ColorUtil.getWarningLogBackgroundColor();
        } else {
            return ColorUtil.getErrorLogBackgroundColor();
        }
    }

    private void dispose() {
        eventBroker.unsubscribe(this);
        MPartStack mStackPart = (MPartStack) modelService.find(IdConstants.COMPOSER_CONTENT_PARTSTACK_ID, application);
        mStackPart.getChildren().remove(mPart);
    }

    /**
     * Prepare entity before saving
     */
    protected abstract void preSaving();

    protected abstract void populateDataToUI();

    @Persist
    public void save() {
        try {
            saveVariables();
            saveVerificationScript();
            preSaving();

            ObjectRepositoryController.getInstance().updateTestObject(originalWsObject);
            eventBroker.post(EventConstants.TEST_OBJECT_UPDATED,
                    new Object[] { originalWsObject.getId(), originalWsObject });
            eventBroker.post(EventConstants.EXPLORER_REFRESH, null);
            dirtyable.setDirty(false);
        } catch (Exception e) {
            LoggerSingleton.logError(e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), StringConstants.ERROR_TITLE, e.getMessage());
        }
    }
    
    private void saveVariables() {
        VariableEntity[] variables = variableView.getVariables();
        originalWsObject.setVariables(Arrays.asList(variables));
    }
    
    private void saveVerificationScript() {        
        IEditorInput input = verificationScriptEditor.getEditorInput();
        IDocument document = verificationScriptEditor.getDocumentProvider().getDocument(input);
        if (document != null) {
            String script = document.get();
            originalWsObject.setVerificationScript(script);
        }
        GroovyEditorUtil.saveEditor(scriptEditorPart);
    }

    public WebServiceRequestEntity getWSRequestObject() {
        return originalWsObject;
    }

    @Override
    public String getEntityId() {
        return getWSRequestObject().getIdForDisplay();
    }

    @Override
    @Inject
    @Optional
    public void onSelect(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
        MPart part = EventUtil.getPart(event);
        if (part == null || !StringUtils.equals(part.getElementId(), mPart.getElementId())) {
            return;
        }

        EventUtil.post(EventConstants.PROPERTIES_ENTITY, originalWsObject);
    }

    @Override
    @Inject
    @Optional
    public void onChangeEntityProperties(@UIEventTopic(EventConstants.PROPERTIES_ENTITY_UPDATED) Event event) {
        Object eventData = EventUtil.getData(event);
        if (!(eventData instanceof WebServiceRequestEntity)) {
            return;
        }

        WebServiceRequestEntity updatedEntity = (WebServiceRequestEntity) eventData;
        if (!StringUtils.equals(updatedEntity.getIdForDisplay(), getEntityId())) {
            return;
        }
        originalWsObject.setTag(updatedEntity.getTag());
        originalWsObject.setDescription(updatedEntity.getDescription());
    }

    @Override
    @PreDestroy
    public void onClose() {
        deleteTempScriptFile();
        try {
            GroovyEditorUtil.clearEditorProblems(verificationScriptEditor);
        } catch (CoreException e) {
            LoggerSingleton.logError(e);
        }
        EventUtil.post(EventConstants.PROPERTIES_ENTITY, null);
    }
    
    private void deleteTempScriptFile() {
        IFileEditorInput input = (IFileEditorInput) verificationScriptEditor.getEditorInput();
        IFile tempScriptFile = input.getFile();
        tempScriptFile.getRawLocation().toFile().delete();
        
        try {
            tempScriptFile.delete(true, null);
        } catch (CoreException e) {
            LoggerSingleton.logError(e);
        }
    }

    private WebElementPropertyEntity createBasicAuthHeaderElement() {
        return new WebElementPropertyEntity(HTTP_HEADER_AUTHORIZATION,
                BASIC_AUTH_PREFIX_VALUE + Base64.basicEncode(txtUsername.getText(), txtPassword.getText()));
    }

    protected void populateOAuth1FromHeader() {
        oauth1Headers.clear();
        oauth1Headers.addAll(tblHeaders.getInput()
                .stream()
                .filter(header -> header.getName().startsWith(AUTH_META_PREFIX))
                .collect(Collectors.toList()));
        if (oauth1Headers.isEmpty()) {
            return;
        }
        java.util.Optional<WebElementPropertyEntity> authType = oauth1Headers.stream()
                .filter(header -> AUTHORIZATION_TYPE.equals(header.getName()) && OAUTH_1_0.equals(header.getValue()))
                .findFirst();
        if (!authType.isPresent()) {
            // Not an OAuth 1.0 authorization
            return;
        }
        int indexOfOAuth1 = Arrays.asList(ccbAuthType.getItems()).indexOf(OAUTH_1_0);
        ccbAuthType.select(indexOfOAuth1);

        oauth1Headers.forEach(header -> {
            String name = header.getName();
            String value = header.getValue();
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_CONSUMER_KEY)) {
                txtConsumerKey.setText(value);
                return;
            }
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_CONSUMER_SECRET)) {
                txtConsumerSecret.setText(value);
                return;
            }
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_SIGNATURE_METHOD)) {
                int index = Arrays.asList(ccbOAuth1SignatureMethod.getItems()).indexOf(value);
                ccbOAuth1SignatureMethod.select(index);
                return;
            }
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_TOKEN)) {
                txtToken.setText(value);
                return;
            }
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_TOKEN_SECRET)) {
                txtTokenSecret.setText(value);
                return;
            }
            if (StringUtils.equals(name, AUTHORIZATION_OAUTH_REALM)) {
                txtRealm.setText(value);
                return;
            }
        });
    }

    protected void createOAuth1Headers(String consumerKey, String consumerSecretOrPrivateKey, String token,
            String tokenSecret, String signatureMethod, String realm) {
        removeOAuth1Headers();
        oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_TYPE, OAUTH_1_0));
        oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_CONSUMER_KEY, consumerKey));
        oauth1Headers
                .add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_CONSUMER_SECRET, consumerSecretOrPrivateKey));
        oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_SIGNATURE_METHOD, signatureMethod));
        if (StringUtils.isNotBlank(token)) {
            oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_TOKEN, token));
        }
        if (StringUtils.isNotBlank(tokenSecret)) {
            oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_TOKEN_SECRET, tokenSecret));
        }
        if (StringUtils.isNotBlank(realm)) {
            oauth1Headers.add(new WebElementPropertyEntity(AUTHORIZATION_OAUTH_REALM, realm));
        }
        tblHeaders.addRows(oauth1Headers);
    }

    protected void removeOAuth1Headers() {
        tblHeaders.deleteRows(oauth1Headers);
        oauth1Headers.clear();
    }

    protected void populateBasicAuthFromHeader() {
        java.util.Optional<WebElementPropertyEntity> authHeader = tblHeaders.getInput()
                .stream()
                .filter(i -> HTTP_HEADER_AUTHORIZATION.equalsIgnoreCase(i.getName())
                        && StringUtils.startsWithIgnoreCase(i.getValue(), BASIC_AUTH_PREFIX_VALUE))
                .findFirst();
        if (!authHeader.isPresent()) {
            // Not a basic authorization
            return;
        }

        String[] authValueArr = StringUtils.split(authHeader.get().getValue(), StringConstants.CR_SPACE);
        if (authValueArr.length != 2) {
            return;
        }

        String[] usernamePassword = Base64.basicDecode(authValueArr[1]);
        txtUsername.setText(usernamePassword[0]);
        txtPassword.setText(usernamePassword[1]);
        ccbAuthType.select(Arrays.asList(ccbAuthType.getItems()).indexOf(BASIC_AUTH));
    }

    protected boolean isBodySupported() {
        String requestMethod = wsApiControl.getRequestMethod();
        return !(WebServiceRequestEntity.GET_METHOD.equalsIgnoreCase(requestMethod)
                || WebServiceRequestEntity.DELETE_METHOD.equalsIgnoreCase(requestMethod));
    }

    protected boolean isSOAP() {
        return WebServiceRequestEntity.SOAP.equals(originalWsObject.getServiceType());
    }

    protected void setDirty() {
        dirtyable.setDirty(true);
    }

    protected String getPrettyHeaders(ResponseObject reponseObject) {
        StringBuilder sb = new StringBuilder();
        reponseObject.getHeaderFields().forEach((key, value) -> sb.append((key == null) ? "" : key + ": ")
                .append(StringUtils.join(value, "\t"))
                .append("\r\n"));
        return sb.toString();
    }

    protected boolean isInvalidURL(String url) {
        return StringUtils.isBlank(url) || !(new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(url));
    }

    protected void renderAuthenticationUI(String authType) {
        setCompositeVisible(userComposite, BASIC_AUTH.equals(authType));
        setCompositeVisible(oauthComposite, OAUTH_1_0.equals(authType));
        if (StringUtils.isBlank(authType)) {
            ccbAuthType.select(0);
        }
        
        ui.getAuthorizationPartComposite().layout(true, true);
//        sComposite.setMinSize(mainComposite.computeSize(MIN_PART_WIDTH, SWT.DEFAULT));
    }

    public void updateIconURL(String imageURL) {
        MPartStack stack = (MPartStack) modelService.find(IdConstants.COMPOSER_CONTENT_PARTSTACK_ID, application);
        int index = stack.getChildren().indexOf(mPart);
        MPart mPart = (MPart) stack.getChildren().get(index);

        // Work around to update Icon URL for MPart.
        mPart.getTransientData().put(ICON_URI_FOR_PART, imageURL);
        mPart.setIconURI(imageURL);
    }
    
    public void updateDirty(boolean dirty) {
        dirtyable.setDirty(dirty);
    }

    @Override
    public List<MPart> getChildParts() {
        return Arrays.asList(
                    ui.getApiControlsPart(), 
                    ui.getHeadersPart(), 
                    ui.getAuthorizationPart(), 
                    ui.getBodyPart(), 
                    ui.getScriptEditorPart(),
                    ui.getSnippetPart(),
                    ui.getResponsePart(),
                    ui.getVariablePart()
                );
    }
    
    private class ToolBarForVerificationPart extends ToolBarForMPart {

        public ToolBarForVerificationPart(MPart part) {
            super(part);
            ToolItem toolItem = new ToolItem(this, SWT.PUSH);
            toolItem.setImage(null);
            toolItem.setText(HIDE_SNIPPETS);
            toolItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    MPart snippetPart = ui.getSnippetPart();
                    if (HIDE_SNIPPETS.equals(toolItem.getText())) {
                        partService.hidePart(snippetPart, false);

                        toolItem.setText(SHOW_SNIPPETS);
                    } else {
                        partService.activate(snippetPart);
                        createSnippetComposite();

                        toolItem.setText(HIDE_SNIPPETS);

                        ui.getVerificationPartComposite().layout(true, true);
                    }
                }
            });
        }

    }
}