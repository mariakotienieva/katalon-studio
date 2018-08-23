package com.kms.katalon.composer.report.parts.integration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.entity.report.ReportEntity;

public abstract class TestCaseIntegrationColumn implements IntegrationColumnContributor {

    protected ReportEntity reportEntity;

    public TestCaseIntegrationColumn(ReportEntity reportEntity) {
        this.reportEntity = reportEntity;
    }

    public ReportEntity getReportEntity() {
        return reportEntity;
    }
    
    protected static void openBrowserToLink(String url) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException exception) {
            LoggerSingleton.logError(exception);
        }
    }
}