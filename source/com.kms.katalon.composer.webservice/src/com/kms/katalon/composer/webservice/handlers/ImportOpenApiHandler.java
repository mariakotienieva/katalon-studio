package com.kms.katalon.composer.webservice.handlers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.composer.components.impl.handler.KSEFeatureAccessHandler;
import com.kms.katalon.composer.components.impl.util.TreeEntityUtil;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.tree.ITreeEntity;
import com.kms.katalon.composer.webservice.constants.ComposerWebserviceMessageConstants;
import com.kms.katalon.composer.webservice.constants.StringConstants;
import com.kms.katalon.composer.webservice.openapi.OpenApiImportNode;
import com.kms.katalon.composer.webservice.openapi.OpenApiImporter;
import com.kms.katalon.composer.webservice.openapi.OpenApiProjectImportResult;
import com.kms.katalon.composer.webservice.view.ImportOpenApiDialog;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.FolderController;
import com.kms.katalon.controller.ObjectRepositoryController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.file.FileEntity;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.folder.FolderEntity.FolderType;
import com.kms.katalon.entity.repository.WebElementEntity;
import com.kms.katalon.entity.repository.WebServiceRequestEntity;
import com.kms.katalon.execution.launcher.manager.LauncherManager;
import com.kms.katalon.feature.FeatureServiceConsumer;
import com.kms.katalon.feature.IFeatureService;
import com.kms.katalon.feature.KSEFeature;
import com.kms.katalon.tracking.service.Trackings;

public class ImportOpenApiHandler {

    private IFeatureService featureService = FeatureServiceConsumer.getServiceInstance();

    @CanExecute
    public boolean canExecute() {
        return (ProjectController.getInstance().getCurrentProject() != null)
                && !LauncherManager.getInstance().isAnyLauncherRunning();
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) @Optional Object[] selectedObjects,
            @Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
        Trackings.trackClickImportOpenApi3();
        if (featureService.canUse(KSEFeature.IMPORT_OPENAPI)) {
            try {
                ImportOpenApiDialog dialog = new ImportOpenApiDialog(shell);
                if (dialog.open() == Dialog.OK) {
                    String selectedFilePath = dialog.getSwaggerSpecLocation();
                    if (selectedFilePath != null && selectedFilePath.length() > 0) {
                        FolderEntity importFolderEntity;
                        ITreeEntity parentTreeEntity = findParentTreeEntity(selectedObjects);
                        if (parentTreeEntity == null) {
                            importFolderEntity = FolderController.getInstance()
                                    .getObjectRepositoryRoot(ProjectController.getInstance().getCurrentProject());
                        } else {
                            importFolderEntity = (FolderEntity) parentTreeEntity.getObject();
                        }

                        OpenApiProjectImportResult projectImportResult = OpenApiImporter.getInstance()
                                .importServices(selectedFilePath, importFolderEntity);
                        saveImportedArtifacts(projectImportResult);
                        getEventBroker().post(EventConstants.EXPLORER_REFRESH_TREE_ENTITY,
                                TreeEntityUtil.getFolderTreeEntity(projectImportResult.getFileEntity()));
                        getEventBroker().post(EventConstants.EXPLORER_SET_SELECTED_ITEM,
                                TreeEntityUtil.getFolderTreeEntity(projectImportResult.getFileEntity()));
                    }
                }
            } catch (Exception e) {
                MessageDialog.openError(Display.getCurrent().getActiveShell(), StringConstants.ERROR,
                        ComposerWebserviceMessageConstants.ERROR_MSG_FAIL_TO_IMPORT_OPENAPI);
                LoggerSingleton.logError(e);
            }
        } else {
            KSEFeatureAccessHandler.handleUnauthorizedAccess(KSEFeature.IMPORT_OPENAPI);
        }
    }

    private void saveImportedArtifacts(OpenApiProjectImportResult projectImportResult) throws Exception {
        List<OpenApiImportNode> importNodes = flatten(projectImportResult).collect(Collectors.toList());
        for (OpenApiImportNode importNode : importNodes) {
            FileEntity fileEntity = importNode.getFileEntity();
            if (fileEntity != null && fileEntity instanceof FolderEntity) {
                FolderController.getInstance().saveFolder((FolderEntity) fileEntity);
            }
            if (fileEntity != null && fileEntity instanceof WebServiceRequestEntity) {
                ObjectRepositoryController.getInstance().saveNewTestObject((WebServiceRequestEntity) fileEntity);
            }
        }
    }

    private Stream<? extends OpenApiImportNode> flatten(OpenApiImportNode importNode) {
        return Stream.concat(Stream.of(importNode),
                Stream.of(importNode.getChildImportNodes()).flatMap(n -> flatten(n)));
    }

    private IEventBroker getEventBroker() {
        return EventBrokerSingleton.getInstance().getEventBroker();
    }

    public static ITreeEntity findParentTreeEntity(Object[] selectedObjects) throws Exception {
        if (selectedObjects != null) {
            for (Object entity : selectedObjects) {
                if (entity instanceof ITreeEntity) {
                    Object entityObject = ((ITreeEntity) entity).getObject();
                    if (entityObject instanceof FolderEntity) {
                        FolderEntity folder = (FolderEntity) entityObject;
                        if (folder.getFolderType() == FolderType.WEBELEMENT) {
                            return (ITreeEntity) entity;
                        }
                    } else if (entityObject instanceof WebElementEntity) {
                        return ((ITreeEntity) entity).getParent();
                    }
                }
            }
        }
        return null;
    }
}