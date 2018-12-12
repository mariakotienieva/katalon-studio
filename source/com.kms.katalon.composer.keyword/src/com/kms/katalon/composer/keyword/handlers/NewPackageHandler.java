package com.kms.katalon.composer.keyword.handlers;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.components.impl.tree.FolderTreeEntity;
import com.kms.katalon.composer.components.impl.tree.KeywordTreeEntity;
import com.kms.katalon.composer.components.impl.tree.PackageTreeEntity;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.tree.ITreeEntity;
import com.kms.katalon.composer.keyword.constants.StringConstants;
import com.kms.katalon.composer.keyword.dialogs.NewRenamePackageDialog;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.folder.FolderEntity.FolderType;
import com.kms.katalon.groovy.util.GroovyUtil;
import com.kms.katalon.tracking.service.Trackings;

public class NewPackageHandler {

    @Inject
    IEventBroker eventBroker;

    @Inject
    private ESelectionService selectionService;

    private FolderTreeEntity keywordFolderTreeRoot;

    @CanExecute
    private boolean canExecute() {
        return ProjectController.getInstance().getCurrentProject() != null;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell) {
        try {
            Object[] selectedObjects = (Object[]) selectionService.getSelection(IdConstants.EXPLORER_PART_ID);
            ITreeEntity selectedTreeEntity = null;
            Object parentTreeEntity = null;
            IPackageFragment packageFragment = null;
            IPackageFragmentRoot packageRoot = null;

            if (selectedObjects[0] instanceof ITreeEntity) {
                selectedTreeEntity = (ITreeEntity) selectedObjects[0];
            }

            if (selectedTreeEntity != null) {
                if (selectedTreeEntity instanceof FolderTreeEntity) {
                    FolderEntity folder = (FolderEntity) selectedTreeEntity.getObject();
                    parentTreeEntity = selectedTreeEntity;

                    IProject groovyProject = GroovyUtil.getGroovyProject(ProjectController.getInstance()
                            .getCurrentProject());
                    packageRoot = JavaCore.create(groovyProject).getPackageFragmentRoot(groovyProject.getFolder(folder.getRelativePath())); // default package
                } else if (selectedTreeEntity instanceof KeywordTreeEntity) {
                    if (selectedTreeEntity.getParent() != null
                            && selectedTreeEntity.getParent() instanceof PackageTreeEntity) {
                        packageFragment = (IPackageFragment) ((PackageTreeEntity) selectedTreeEntity.getParent()).getObject();
                        
                        parentTreeEntity = ((PackageTreeEntity) selectedTreeEntity.getParent()).getParent();

                        packageRoot = (IPackageFragmentRoot) packageFragment.getParent();
                    }
                } else if (selectedTreeEntity instanceof PackageTreeEntity) {
                    packageFragment = (IPackageFragment) ((PackageTreeEntity) selectedTreeEntity).getObject();
                    parentTreeEntity = selectedTreeEntity.getParent();

                    packageRoot = (IPackageFragmentRoot) packageFragment.getParent();
                }
            }

            if (packageRoot != null) {
                NewRenamePackageDialog dialog = new NewRenamePackageDialog(parentShell, packageRoot, true);
                dialog.open();
                if (dialog.getReturnCode() == Dialog.OK) {
                    if (dialog.getName().length() > StringConstants.MAX_PKG_AND_CLASS_NAME_LENGTH) {
                        MessageDialog.openError(parentShell, StringConstants.ERROR_TITLE, MessageFormat.format(
                                StringConstants.HAND_ERROR_MSG_EXCEED_PKG_NAME_LENGTH, dialog.getName().length(),
                                StringConstants.MAX_PKG_AND_CLASS_NAME_LENGTH));
                        return;
                    }
                    // Create package
                    IProgressMonitor monitor = new NullProgressMonitor();
                    IPackageFragment newPackageFragment = packageRoot.createPackageFragment(dialog.getName(), true, monitor);
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    
                    Trackings.trackCreatingObject("package");

                    eventBroker.send(EventConstants.EXPLORER_REFRESH_TREE_ENTITY, parentTreeEntity);
                    // remove any working copy of child complicationUnit that exists in the current package
                    for (ICompilationUnit compicationUnit : newPackageFragment.getCompilationUnits()) {
                        compicationUnit.discardWorkingCopy();
                    }
                    eventBroker.send(EventConstants.EXPLORER_SET_SELECTED_ITEM, new PackageTreeEntity(
                            newPackageFragment, (ITreeEntity) parentTreeEntity));
                }
            }

        } catch (Exception e) {
            LoggerSingleton.logError(e);
            MessageDialog.openError(parentShell, StringConstants.ERROR_TITLE,
                    StringConstants.HAND_ERROR_MSG_UNABLE_TO_CREATE_PACKAGE);
        }
    }

    public static ITreeEntity findParentTreeEntity(Object[] selectedObjects) throws Exception {
        if (ArrayUtils.isNotEmpty(selectedObjects) && selectedObjects[0] instanceof ITreeEntity) {
            ITreeEntity parentTreeEntity = (ITreeEntity) selectedObjects[0];
            if (parentTreeEntity instanceof FolderTreeEntity) {
                FolderEntity parentFolder = (FolderEntity) parentTreeEntity.getObject();
                if (parentFolder.getFolderType() == FolderType.KEYWORD) {
                    return parentTreeEntity;
                }
                if (FolderController.getInstance().isSourceFolder(ProjectController.getInstance().getCurrentProject(), parentFolder)) {
                    return parentTreeEntity;
                }
            } else if (parentTreeEntity instanceof KeywordTreeEntity || parentTreeEntity instanceof PackageTreeEntity) {
                return parentTreeEntity;
            }
        }
        return null;
    }

    @Inject
    @Optional
    private void catchKeywordTreeEntitiesRoot(
            @UIEventTopic(EventConstants.EXPLORER_RELOAD_INPUT) List<Object> treeEntities) {
        try {
            for (Object o : treeEntities) {
                Object entityObject = ((ITreeEntity) o).getObject();
                if (entityObject instanceof FolderEntity) {
                    FolderEntity folder = (FolderEntity) entityObject;
                    if (folder.getFolderType() == FolderType.KEYWORD) {
                        keywordFolderTreeRoot = (FolderTreeEntity) o;
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LoggerSingleton.logError(e);
        }
    }

    @Inject
    @Optional
    private void execute(@UIEventTopic(EventConstants.PACKAGE_NEW) Object eventData) {
        if (!canExecute()) {
            return;
        }
        execute(Display.getCurrent().getActiveShell());
    }

}