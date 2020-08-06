package com.kms.katalon.execution.webservice.configuration.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.kms.katalon.core.configuration.RunConfiguration;
import com.kms.katalon.execution.configuration.impl.DefaultExecutionSetting;
import com.kms.katalon.execution.entity.IExecutedEntity;
import com.kms.katalon.execution.entity.TestSuiteExecutedEntity;
import com.kms.katalon.execution.webservice.setting.WebServiceExecutionSettingStore;
import com.kms.katalon.logging.LogUtil;

public class WebServiceExecutionSetting extends DefaultExecutionSetting {

    public WebServiceExecutionSettingStore getExecutionSettingStore() {
        return WebServiceExecutionSettingStore.getStore();
    }

    @Override
    public Map<String, Object> getGeneralProperties() {
        Map<String, Object> generalProperties = super.getGeneralProperties();
        generalProperties.putAll(getWebServiceExecutionProperties());
        return generalProperties;
    }

    private Map<String, Object> getWebServiceExecutionProperties() {
        Map<String, Object> reportProps = new HashMap<String, Object>();
        WebServiceExecutionSettingStore executionSettingStore = getExecutionSettingStore();
        try {
            IExecutedEntity executedEntity = getExecutedEntity();
            long maxResponseSize = executionSettingStore.getMaxResponseSize();
            if (executedEntity instanceof TestSuiteExecutedEntity) {
                TestSuiteExecutedEntity testSuiteExecutedEntity = (TestSuiteExecutedEntity) executedEntity;
                maxResponseSize = testSuiteExecutedEntity.getWebServiceSettings().getMaxResponseSize();
            }

            reportProps.put(RunConfiguration.REQUEST_CONNECTION_TIMEOUT, executionSettingStore.getConnectionTimeout());
            reportProps.put(RunConfiguration.REQUEST_SOCKET_TIMEOUT, executionSettingStore.getSocketTimeout());
            reportProps.put(RunConfiguration.REQUEST_MAX_RESPONSE_SIZE, maxResponseSize);
        } catch (IOException error) {
            LogUtil.logError(error);
        }
        return reportProps;
    }
}