package com.kms.katalon.dal.fileservice.dataprovider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kms.katalon.dal.IUserFileDataProvider;
import com.kms.katalon.dal.exception.DALException;
import com.kms.katalon.dal.fileservice.manager.EntityFileServiceManager;
import com.kms.katalon.entity.file.FileEntity;
import com.kms.katalon.entity.file.UserFileEntity;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.folder.FolderEntity.FolderType;
import com.kms.katalon.entity.project.ProjectEntity;

public class UserFileServiceDataProvider implements IUserFileDataProvider {

    @Override
    public List<FileEntity> getChildren(FolderEntity parentFolder) throws DALException {
        File folder = new File(parentFolder.getLocation());
        if (!folder.exists() || folder.listFiles() == null) {
            return Collections.emptyList();
        }
        List<FileEntity> fileEntities = new ArrayList<>();
        for (File file : folder.listFiles(EntityFileServiceManager.fileFilter)) {
            if (file.isFile()) {
                UserFileEntity fileEntity = new UserFileEntity(file);
                fileEntity.setParentFolder(parentFolder);
                fileEntity.setProject(parentFolder.getProject());
                
                fileEntities.add(fileEntity);
            } else {
                FolderEntity childFolder = new FolderEntity();
                childFolder.setFolderType(FolderType.USER);
                childFolder.setName(file.getName());
                childFolder.setParentFolder(parentFolder);
                childFolder.setProject(parentFolder.getProject());
                
                fileEntities.add(childFolder);
            }
        }
        fileEntities.sort(new Comparator<FileEntity>() {

            @Override
            public int compare(FileEntity fileA, FileEntity fileB) {
                if (fileA instanceof FolderEntity && fileB instanceof UserFileEntity) { 
                    return 1;
                }
                if (fileB instanceof FolderEntity && fileA instanceof UserFileEntity) { 
                    return -1;
                }
                return fileA.getName().compareToIgnoreCase(fileB.getName());
            }
        });
        return fileEntities;
    }

    @Override
    public UserFileEntity newFile(String name, FolderEntity parentFolder) throws DALException {
        try {
            File file = new File(parentFolder.getLocation(), name);
            file.createNewFile();
            
            UserFileEntity fileEntity = new UserFileEntity(file);
            fileEntity.setParentFolder(parentFolder);
            fileEntity.setProject(parentFolder.getProject());
            return fileEntity;
        } catch (IOException e) {
            throw new DALException(e);
        }
    }
    
    @Override
    public UserFileEntity newRootFile(String name, ProjectEntity project) throws DALException {
        try {
            File file = new File(project.getFolderLocation(), name);
            file.createNewFile();
            
            UserFileEntity fileEntity = new UserFileEntity(file);
            fileEntity.setProject(project);
            return fileEntity;
        } catch (IOException e) {
            throw new DALException(e);
        }
    }
    
    @Override
    public UserFileEntity renameFile(String newName, UserFileEntity userFileEntity) {
        File newFile;
        if (userFileEntity.getParentFolder() != null) {
            newFile = new File(userFileEntity.getParentFolder().getLocation(), newName);
        } else {
            newFile = new File(userFileEntity.getProject().getFolderLocation(), newName);
        }
        userFileEntity.getFile().renameTo(newFile);
        userFileEntity.setFile(newFile);
        return userFileEntity;
    }

    @Override
    public void deleteFile(UserFileEntity userFileEntity) {
        userFileEntity.getFile().delete();
    }

}