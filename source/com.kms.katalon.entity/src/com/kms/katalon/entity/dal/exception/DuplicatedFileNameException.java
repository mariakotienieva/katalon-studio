/**
 * (C) Copyright AutoTrader. 2013. All rights reserved.
  * Warning: This computer program is protected by copyright law and international treaties.
 * Unauthorized reproduction or distribution of this program, or any portion of it, may result
 * in severe civil and criminal penalties, and will be prosecuted to the maximum extent
 * possible under the law.
 */
package com.kms.katalon.entity.dal.exception;

import java.text.MessageFormat;

import com.kms.katalon.entity.constants.StringConstants;

/**
 *
 */
public class DuplicatedFileNameException extends Exception {
    /**  */
    private static final long serialVersionUID = 1L;
    private long parentFolderId;
    private String fileName;

    public DuplicatedFileNameException(long parentFolderId, String fileName){
        super(MessageFormat.format(StringConstants.EXC_DUPLICATED_FILE_NAME, fileName, String.valueOf(parentFolderId)));
        this.parentFolderId= parentFolderId;
        this.fileName = fileName;
    }
    
    public DuplicatedFileNameException(String message){
        super(message);
    }

    public long getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getfileName() {
        return fileName;
    }

    public void setfileName(String fileName) {
        this.fileName = fileName;
    }
}