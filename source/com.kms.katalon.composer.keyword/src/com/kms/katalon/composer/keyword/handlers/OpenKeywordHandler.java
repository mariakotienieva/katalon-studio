package com.kms.katalon.composer.keyword.handlers;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.codehaus.groovy.eclipse.refactoring.actions.FormatGroovyAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.composer.components.impl.constants.ImageConstants;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.keyword.constants.StringConstants;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.groovy.constant.GroovyConstants;
import com.kms.katalon.tracking.service.Trackings;

public class OpenKeywordHandler {

    @Inject
    private IEventBroker eventBroker;

    @PostConstruct
    public void registerEventHandler() {
        eventBroker.subscribe(EventConstants.EXPLORER_OPEN_SELECTED_ITEM, new EventHandler() {
            @Override
            public void handleEvent(Event event) {
                Object object = event.getProperty(EventConstants.EVENT_DATA_PROPERTY_NAME);
                if (object != null && object instanceof ICompilationUnit
                        && ((ICompilationUnit) object).getElementName().endsWith(GroovyConstants.GROOVY_FILE_EXTENSION)) {
                    excute((ICompilationUnit) object);
                }
            }
        });
    }

    /**
     * Open a custom keyword file and validate that file after user save it
     * 
     * @param keywordFile
     */
    private void excute(ICompilationUnit keywordFile) {
        if (keywordFile != null && keywordFile.exists()) {
            try {
                IFile iFile = (IFile) keywordFile.getResource();
                if (!keywordFile.isWorkingCopy()) {
                    keywordFile.becomeWorkingCopy(null);
                }
                
                 IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry()
                 .getDefaultEditor(iFile.getName());
                 desc.getImageDescriptor().createFromImage(ImageConstants.IMG_16_KEYWORD);
                 ITextEditor editor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                 .openEditor(new FileEditorInput(iFile), desc.getId());
                 if (editor != null) {
                     formatEditor(editor);
                 }
                 if (isKeywordFile(iFile)) {
                     Trackings.trackOpenObject("keyword");
                 } else {
                     Trackings.trackOpenObject("groovyScriptFile");
                 }
            } catch (Exception e) {
                LoggerSingleton.logError(e);
                MessageDialog.openError(null, StringConstants.ERROR_TITLE,
                        StringConstants.HAND_ERROR_MSG_CANNOT_OPEN_KEYWORD_FILE);
            }
        }
    }
    
    private boolean isKeywordFile(IFile scriptFile) throws Exception {
        ProjectEntity project = ProjectController.getInstance().getCurrentProject();
        FolderEntity keywordFolder = FolderController.getInstance().getKeywordRoot(project);
        String keywordFolderPath = keywordFolder.getLocation();
        String scriptFilePath = scriptFile.getLocation().toFile().getAbsolutePath();
        return scriptFilePath.startsWith(keywordFolderPath);
    }
    
    private void formatEditor(ITextEditor editor) {
        FormatGroovyAction formatAction = (FormatGroovyAction) editor.getAction("Format");

        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        formatAction.run(new TextSelection(0, document.getLength()));
        editor.doSave(new NullProgressMonitor());
    }
}
