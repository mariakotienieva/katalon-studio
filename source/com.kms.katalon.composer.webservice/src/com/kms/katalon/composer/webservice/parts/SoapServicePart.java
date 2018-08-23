package com.kms.katalon.composer.webservice.parts;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.wsdl.WSDLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.components.impl.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.impl.dialogs.ProgressMonitorDialogWithThread;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.composer.webservice.constants.ComposerWebserviceMessageConstants;
import com.kms.katalon.composer.webservice.constants.StringConstants;
import com.kms.katalon.composer.webservice.editor.SoapRequestMessageEditor;
import com.kms.katalon.composer.webservice.soap.response.body.SoapResponseBodyEditorsComposite;
import com.kms.katalon.composer.webservice.util.WSDLHelper;
import com.kms.katalon.composer.webservice.util.WebServiceUtil;
import com.kms.katalon.composer.webservice.view.xml.ColorManager;
import com.kms.katalon.composer.webservice.view.xml.XMLConfiguration;
import com.kms.katalon.composer.webservice.view.xml.XMLPartitionScanner;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.controller.WebServiceController;
import com.kms.katalon.core.testobject.ObjectRepository;
import com.kms.katalon.core.testobject.RequestObject;
import com.kms.katalon.core.testobject.ResponseObject;
import com.kms.katalon.core.util.internal.ExceptionsUtil;
import com.kms.katalon.core.webservice.common.BasicRequestor;
import com.kms.katalon.entity.repository.WebElementPropertyEntity;
import com.kms.katalon.entity.repository.WebServiceRequestEntity;
import com.kms.katalon.execution.preferences.ProxyPreferences;
import com.kms.katalon.tracking.service.Trackings;

public class SoapServicePart extends WebServicePart {

    private static final String[] FILTER_EXTS = new String[] { "*.xml; *.wsdl; *.txt" };

    private static final String[] FILTER_NAMES = new String[] { "XML content files (*.xml, *.wsdl, *.txt)" };

    protected SoapResponseBodyEditorsComposite soapResponseBodyEditor;
    
    private ProgressMonitorDialogWithThread progress;

    private CCombo ccbOperation;
    
    protected SoapRequestMessageEditor requestBodyEditor;

    @Override
    protected void createAPIControls(Composite parent) {
        super.createAPIControls(parent);

        wsApiControl.addRequestMethodSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccbOperation.removeAll();
            }
        });

        Composite operationComposite = new Composite(parent, SWT.NONE);
        GridLayout glOperation = new GridLayout(3, false);
        glOperation.marginWidth = 0;
        glOperation.marginHeight = 0;
        operationComposite.setLayout(glOperation);
        operationComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblOperation = new Label(operationComposite, SWT.NONE);
        lblOperation.setText(StringConstants.PA_LBL_SERVICE_FUNCTION);
        GridData gdLblOperation = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblOperation.widthHint = 102;
        lblOperation.setLayoutData(gdLblOperation);

        ccbOperation = new CCombo(operationComposite, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);
        ccbOperation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        ccbOperation.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setDirty();
            }
        });

        Button btnLoadFromWSDL = new Button(operationComposite, SWT.FLAT);
        btnLoadFromWSDL.setText(StringConstants.LBL_LOAD_FROM_WSDL);
        GridData gdBtnLoadFromWSDL = new GridData(SWT.CENTER, SWT.FILL, false, false);
        gdBtnLoadFromWSDL.widthHint = 100; // same width with send button
        btnLoadFromWSDL.setLayoutData(gdBtnLoadFromWSDL);
        btnLoadFromWSDL.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // Load operations from WS
                String requestURL = wsApiControl.getRequestURL().trim();
                if (isInvalidURL(requestURL)) {
                    return;
                }

                try {
                    Shell activeShell = Display.getCurrent().getActiveShell();
                    new ProgressMonitorDialogWithThread(activeShell).run(true, true, new IRunnableWithProgress() {

                        @Override
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            monitor.beginTask(StringConstants.MSG_FETCHING_FROM_WSDL, IProgressMonitor.UNKNOWN);
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        List<String> servFuncs = WSDLHelper
                                                .newInstance(requestURL, getAuthorizationHeaderValue())
                                                .getOperationNamesByRequestMethod(wsApiControl.getRequestMethod());
                                        ccbOperation.setItems(servFuncs.toArray(new String[0]));
                                        if (servFuncs.size() > 0) {
                                            ccbOperation.select(0);
                                        }
                                        setDirty();
                                    } catch (WSDLException e) {
                                        LoggerSingleton.logError(e);
                                        MessageDialog.openError(activeShell, StringConstants.ERROR_TITLE,
                                                StringConstants.MSG_CANNOT_LOAD_WS);
                                    } finally {
                                        monitor.done();
                                    }
                                }
                            });
                        }
                    });
                } catch (InvocationTargetException | InterruptedException ex) {
                    LoggerSingleton.logError(ex);
                }
            }
        });
    }
    
    @Override
    protected void sendRequest(boolean runVerificationScript) {
        if (dirtyable.isDirty()) {
            boolean isOK = MessageDialog.openConfirm(null, StringConstants.WARN,
                    ComposerWebserviceMessageConstants.PART_MSG_DO_YOU_WANT_TO_SAVE_THE_CHANGES);
            if (!isOK) {
                return;
            }
            save();
        }

        clearPreviousResponse();

        String requestURL = wsApiControl.getRequestURL().trim();
        if (isInvalidURL(requestURL)) {
            LoggerSingleton.logError("URL is invalid");
            MessageDialog.openError(null, StringConstants.ERROR, "URL is invalid");
            return;
        }

        if (ccbOperation.getText().isEmpty()) {
            LoggerSingleton.logError("Service Function is empty");
            MessageDialog.openError(null, StringConstants.ERROR, "Service Function is empty");
            return;
        }

        if (wsApiControl.getSendingState()) {
            progress.getProgressMonitor().setCanceled(true);
            wsApiControl.setSendButtonState(false);
            return;
        }

        try {
            Trackings.trackTestWebServiceObject(runVerificationScript);
            wsApiControl.setSendButtonState(true);
            progress = new ProgressMonitorDialogWithThread(Display.getCurrent().getActiveShell());
            progress.setOpenOnRun(false);
            displayResponseContentBasedOnSendingState(true);
            progress.run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(ComposerWebserviceMessageConstants.PART_MSG_SENDING_TEST_REQUEST,
                                IProgressMonitor.UNKNOWN);

                        String projectDir = ProjectController.getInstance().getCurrentProject().getFolderLocation();

                        WebServiceRequestEntity requestEntity = getWSRequestObject();
                        
                        Map<String, String> evaluatedVariables = evaluateRequestVariables();

                        ResponseObject responseObject = WebServiceController.getInstance().sendRequest(requestEntity,
                                projectDir, ProxyPreferences.getProxyInformation(),
                                Collections.<String, Object>unmodifiableMap(evaluatedVariables));

                        if (monitor.isCanceled()) {
                            return;
                        }

                        String bodyContent = responseObject.getResponseText();
                        Display.getDefault().asyncExec(() -> {
                            setResponseStatus(responseObject);
                            mirrorEditor.setText(getPrettyHeaders(responseObject));
                            if (bodyContent == null) {
                                return;
                            }
                            soapResponseBodyEditor.setInput(responseObject);

                        });

                        if (runVerificationScript) {
                            executeVerificationScript(responseObject);
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        UISynchronizeService.syncExec(() -> wsApiControl.setSendButtonState(false));
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target == null) {
                return;
            }
            LoggerSingleton.logError(target);
            MultiStatusErrorDialog.showErrorDialog(
                    ComposerWebserviceMessageConstants.PART_MSG_CANNOT_SEND_THE_TEST_REQUEST, target.getMessage(),
                    ExceptionsUtil.getStackTraceForThrowable(target));
        } catch (InterruptedException ignored) {
        }
        displayResponseContentBasedOnSendingState(false);
    }

    @Override
    protected void createParamsComposite(Composite parent) {
        // SOAP does not need params
    }

    @Override
    protected void addTabBody(CTabFolder parent) {
        super.addTabBody(parent);
        tabBody.setText(StringConstants.PA_LBL_XML_REQ_MSG);
        Composite tabComposite = (Composite) tabBody.getControl();
        requestBodyEditor = new SoapRequestMessageEditor(tabComposite, SWT.NONE, this);
        requestBodyEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    protected void createResponseComposite(Composite parent) {
        super.createResponseComposite(parent);
        soapResponseBodyEditor = new SoapResponseBodyEditorsComposite(responseBodyComposite, SWT.NONE);
        soapResponseBodyEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    protected void preSaving() {
        originalWsObject.setWsdlAddress(wsApiControl.getRequestURL());
        originalWsObject.setSoapRequestMethod(wsApiControl.getRequestMethod());
        originalWsObject.setSoapServiceFunction(ccbOperation.getText());

        tblHeaders.removeEmptyProperty();
        originalWsObject.setHttpHeaderProperties(httpHeaders);

        originalWsObject.setSoapBody(requestBodyEditor.getHttpBodyContent());
        updateIconURL(WebServiceUtil.getRequestMethodIcon(originalWsObject.getServiceType(), originalWsObject.getSoapRequestMethod()));
    }

    @Override
    protected void populateDataToUI() {
        wsApiControl.getRequestURLControl().setText(originalWsObject.getWsdlAddress());
        String soapRequestMethod = originalWsObject.getSoapRequestMethod();
        int index = Arrays.asList(WebServiceRequestEntity.SOAP_REQUEST_METHODS).indexOf(soapRequestMethod);
        wsApiControl.getRequestMethodControl().select(index < 0 ? 0 : index);
        ccbOperation.setText(originalWsObject.getSoapServiceFunction());

        tempPropList = new ArrayList<WebElementPropertyEntity>(originalWsObject.getHttpHeaderProperties());
        httpHeaders.clear();
        httpHeaders.addAll(tempPropList);
        tblHeaders.refresh();

        populateBasicAuthFromHeader();
        populateOAuth1FromHeader();
        renderAuthenticationUI(ccbAuthType.getText());

//        requestBody.setDocument(createXMLDocument(originalWsObject.getSoapBody()));
        requestBodyEditor.setInput((WebServiceRequestEntity)originalWsObject.clone());
        dirtyable.setDirty(false);
    }

    private SourceViewer createXMLSourceViewer(Composite parent) {
        SourceViewer sv = createSourceViewer(parent, new GridData(SWT.FILL, SWT.FILL, true, true));
        sv.configure(new XMLConfiguration(new ColorManager()));
        return sv;
    }

    private IDocument createXMLDocument(String documentContent) {
        IDocument document = new Document(documentContent);
        IDocumentPartitioner partitioner = new FastPartitioner(new XMLPartitionScanner(),
                new String[] { XMLPartitionScanner.XML_START_TAG, XMLPartitionScanner.XML_PI,
                        XMLPartitionScanner.XML_END_TAG, XMLPartitionScanner.XML_TEXT, XMLPartitionScanner.XML_CDATA,
                        XMLPartitionScanner.XML_COMMENT });
        partitioner.connect(document);
        document.setDocumentPartitioner(partitioner);
        document.addDocumentListener(new IDocumentListener() {

            @Override
            public void documentChanged(DocumentEvent event) {
                setDirty();
            }

            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
                // do nothing
            }
        });
        return document;
    }

    private void formatRequestBody() {
        try {
            StyledText requestBodyWidget = requestBody.getTextWidget();
            String sw = formatXMLContent(requestBodyWidget.getText());
            requestBodyWidget.setText(sw);
            setDirty();
        } catch (Exception ex) {
            ErrorDialog.openError(null, StringConstants.ERROR_TITLE,
                    ComposerWebserviceMessageConstants.PART_MSG_CANNOT_FORMAT_THE_XML_CONTENT,
                    new Status(IStatus.ERROR, WS_BUNDLE_NAME, ex.getMessage(), ex));
        }
    }

    private String formatXMLContent(String content) throws DocumentException, IOException {
        org.dom4j.Document doc = DocumentHelper.parseText(content);
        StringWriter sw = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndent(TAB_SPACE);
        format.setNewLineAfterDeclaration(false);
        XMLWriter xw = new XMLWriter(sw, format);
        xw.write(doc);
        return sw.toString();
    }

    private String getAuthorizationHeaderValue() {
        Optional<WebElementPropertyEntity> definedAuthorization = httpHeaders.stream()
                .filter(header -> HttpHeaders.AUTHORIZATION.equals(header.getName()))
                .findFirst();
        if (definedAuthorization.isPresent()) {
            return definedAuthorization.get().getValue();
        }

        Map<String, String> map = oauth1Headers.stream()
                .collect(Collectors.toMap(WebElementPropertyEntity::getName, WebElementPropertyEntity::getValue));
        String authType = map.get(AUTHORIZATION_TYPE);
        if (StringUtils.isBlank(authType)) {
            return null;
        }

        if (OAUTH_1_0.equals(authType)) {
            try {
                String oauth1AuthorizationHeader = BasicRequestor
                        .createOAuth1AuthorizationHeaderValue(wsApiControl.getRequestURL().trim(), map);
                return StringUtils.isBlank(oauth1AuthorizationHeader) ? null : oauth1AuthorizationHeader;
            } catch (GeneralSecurityException e) {
                LoggerSingleton.logError(e);
            } catch (IOException e) {
                LoggerSingleton.logError(e);
            }
        }

        return null;
    }

}