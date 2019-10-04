package com.kms.katalon.integration.analytics.handler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.kms.katalon.application.KatalonApplicationActivator;
import com.kms.katalon.integration.analytics.constants.ComposerAnalyticsStringConstants;
import com.kms.katalon.integration.analytics.constants.IntegrationAnalyticsMessages;
import com.kms.katalon.integration.analytics.entity.AnalyticsOrganization;
import com.kms.katalon.integration.analytics.entity.AnalyticsProject;
import com.kms.katalon.integration.analytics.entity.AnalyticsTeam;
import com.kms.katalon.integration.analytics.entity.AnalyticsTokenInfo;
import com.kms.katalon.integration.analytics.exceptions.AnalyticsApiExeception;
import com.kms.katalon.integration.analytics.providers.AnalyticsApiProvider;
import com.kms.katalon.integration.analytics.setting.AnalyticsSettingStore;
import com.kms.katalon.logging.LogUtil;

public class AnalyticsAuthorizationHandler {
    
	public static AnalyticsTokenInfo getTokenNew(String serverUrl, String email, String password, AnalyticsSettingStore settingStore) {
        try {
            AnalyticsTokenInfo tokenInfo = requestToken(serverUrl, email, password);
            settingStore.setToken(tokenInfo.getAccess_token());
            return tokenInfo;
        } catch(Exception ex) {
            LogUtil.logError(ex);
            try {
                settingStore.enableIntegration(false);
            } catch (IOException | GeneralSecurityException e) {
                LogUtil.logError(e);
            }
        }
        return null;
    }

    public static AnalyticsTokenInfo getToken(String serverUrl, String email, String password, AnalyticsSettingStore settingStore) {
        try {
            AnalyticsTokenInfo tokenInfo = requestToken(serverUrl, email, password);
            settingStore.setToken(tokenInfo.getAccess_token());
            return tokenInfo;
        } catch(Exception ex) {
            LogUtil.logError(ex);
            try {
                settingStore.enableIntegration(false);
            } catch (IOException | GeneralSecurityException e) {
                LogUtil.logError(e);
            }
            try {
                String message = KatalonApplicationActivator.getFeatureActivator().getTestOpsMessage(ex.getMessage());
                LogUtil.logError(MessageFormat.format(IntegrationAnalyticsMessages.MSG_ERROR_WITH_REASON, message));
//                MultiStatusErrorDialog.showErrorDialog(ex, ComposerAnalyticsStringConstants.ERROR,
//                        message);
            } catch (Exception e) {
                //Cannot get message from TestOps
//                MultiStatusErrorDialog.showErrorDialog(ex, ComposerAnalyticsStringConstants.ERROR,
//                        IntegrationAnalyticsMessages.MSG_REQUEST_TOKEN_ERROR);
            }
        }
        return null;
    }

    public static List<AnalyticsOrganization> getOrganizations(final String serverUrl, AnalyticsTokenInfo tokenInfo) {
        final List<AnalyticsOrganization> organizations = new ArrayList<>();
        List<AnalyticsOrganization> loaded;
        try {
            loaded = AnalyticsApiProvider.getOrganizations(serverUrl, tokenInfo.getAccess_token());
            if (loaded != null && !loaded.isEmpty()) {
                organizations.addAll(loaded);
            }
        } catch (AnalyticsApiExeception e) {
            LoggerSingleton.logError(e);
        }
        return organizations;
    }

    public static List<AnalyticsOrganization> getOrganizations(final String serverUrl, AnalyticsTokenInfo tokenInfo) {
        final List<AnalyticsOrganization> organizations = new ArrayList<>();
        List<AnalyticsOrganization> loaded;
        try {
            loaded = AnalyticsApiProvider.getOrganizations(serverUrl, tokenInfo.getAccess_token());
            if (loaded != null && !loaded.isEmpty()) {
                organizations.addAll(loaded);
            }
        } catch (AnalyticsApiExeception e) {
            LogUtil.logError(ex);
        }
        return organizations;
    }
    
    public static List<AnalyticsProject> getProjects(final String serverUrl, final AnalyticsTeam team, AnalyticsTokenInfo tokenInfo) {
        final List<AnalyticsProject> projects = new ArrayList<>();
        List<AnalyticsProject> loaded;
        try {
            loaded = AnalyticsApiProvider.getProjects(serverUrl, team, tokenInfo.getAccess_token());
            if (loaded != null && !loaded.isEmpty()) {
                projects.addAll(loaded);
            }
        } catch (AnalyticsApiExeception e) {
            LogUtil.logError(e);
        }
        return projects;
    }
    
    public static List<AnalyticsProject> getProjects(final String serverUrl, final AnalyticsTeam team, AnalyticsTokenInfo tokenInfo, ProgressMonitorDialog monitorDialog) {
        final List<AnalyticsProject> projects = new ArrayList<>();
        try {
            monitorDialog.run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(IntegrationAnalyticsMessages.MSG_DLG_PRG_RETRIEVING_PROJECTS, 2);
                        monitor.subTask(IntegrationAnalyticsMessages.MSG_DLG_PRG_GETTING_PROJECTS);
                        final List<AnalyticsProject> loaded = AnalyticsApiProvider.getProjects(serverUrl, team,
                                tokenInfo.getAccess_token());
                        if (loaded != null && !loaded.isEmpty()) {
                            projects.addAll(loaded);
                        }
                        monitor.worked(1);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
            return projects;
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof AnalyticsApiExeception) {
//                MultiStatusErrorDialog.showErrorDialog(exception, ComposerAnalyticsStringConstants.ERROR,
//                        cause.getMessage());
            } else {
                LogUtil.logError(cause);
            }
        } catch (InterruptedException e) {
            // Ignore this
        }
        return projects;
    }
    
    public static List<AnalyticsTeam> getTeams(final String serverUrl, Long orgId, AnalyticsTokenInfo tokenInfo) {
        final List<AnalyticsTeam> teams = new ArrayList<>();
        List<AnalyticsTeam> loaded;
        try {
            loaded = AnalyticsApiProvider.getTeams(serverUrl, tokenInfo.getAccess_token(), orgId);
            if (loaded != null && !loaded.isEmpty()) {
                teams.addAll(loaded);
            }
        } catch (AnalyticsApiExeception e) {
            LogUtil.logError(e);
        }
        
        return teams;
    }
    
    public static List<AnalyticsTeam> getTeams(final String serverUrl, Long orgId, AnalyticsTokenInfo tokenInfo,
            ProgressMonitorDialog monitorDialog) {
        final List<AnalyticsTeam> teams = new ArrayList<>();
        try {
            monitorDialog.run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(IntegrationAnalyticsMessages.MSG_DLG_PRG_RETRIEVING_TEAMS, 2);
                        monitor.subTask(IntegrationAnalyticsMessages.MSG_DLG_PRG_GETTING_TEAMS);
                        final List<AnalyticsTeam> loaded = AnalyticsApiProvider.getTeams(serverUrl,
                                tokenInfo.getAccess_token(), orgId);
                        if (loaded != null && !loaded.isEmpty()) {
                            teams.addAll(loaded);
                        }
                        monitor.worked(1);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
            return teams;
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof AnalyticsApiExeception) {
//                MultiStatusErrorDialog.showErrorDialog(exception, ComposerAnalyticsStringConstants.ERROR,
//                        cause.getMessage());
            } else {
                LogUtil.logError(cause);
            }
        } catch (InterruptedException e) {
            // Ignore this
        }
        return teams;
    }
    
    public static int getDefaultTeamIndex(AnalyticsSettingStore analyticsSettingStore, List<AnalyticsTeam> teams) {
        int selectionIndex = 0;
        try {
            AnalyticsTeam storedTeam = analyticsSettingStore.getTeam();
            if (storedTeam != null && storedTeam.getId() != null && teams != null) {
                for (int i = 0; i < teams.size(); i++) {
                    AnalyticsTeam p = teams.get(i);
                    if (storedTeam.getId().equals(p.getId())) {
                        selectionIndex = i;
                        return selectionIndex;
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.logError(e);
        }
        return selectionIndex;
    }

    public static int getTeamIndex(List<AnalyticsTeam> teams, AnalyticsTeam selectedTeam) {
        int selectionIndex = 0;
        try {
            if (selectedTeam != null && selectedTeam.getId() != null && teams != null) {
                for (int i = 0; i < teams.size(); i++) {
                    AnalyticsTeam t = teams.get(i);
                    if (selectedTeam.getId().equals(t.getId())) {
                        selectionIndex = i;
                        return selectionIndex;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logError(e);
        }
        return selectionIndex;
    }
    
    public static List<String> getTeamNames(List<AnalyticsTeam> teams) {
        List<String> names = teams.stream().map(t -> t.getName()).collect(Collectors.toList());
        return names;
    }
    
    public static List<String> getOrganizationNames(List<AnalyticsOrganization> orgs) {
        List<String> names = orgs.stream().map(t -> t.getName()).collect(Collectors.toList());
        return names;
    }
    
    public static List<String> getProjectNames(List<AnalyticsProject> projects) {
        List<String> names = new ArrayList<>();
        projects.forEach(p -> names.add(p.getName()));
        return names;
    }
    
    public static int getProjectIndex(AnalyticsProject analyticsProject,
            List<AnalyticsProject> projects) {
        int selectionIndex = 0;
        try {
            if (analyticsProject != null && analyticsProject.getId() != null) {
                for (int i = 0; i < projects.size(); i++) {
                    AnalyticsProject p = projects.get(i);
                    if (analyticsProject.getId().equals(p.getId())) {
                        selectionIndex = i;
                        return selectionIndex;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logError(e);
        }
        return selectionIndex;
    }
    
    public static int getDefaultProjectIndex(AnalyticsSettingStore analyticsSettingStore,
            List<AnalyticsProject> projects) {
        int selectionIndex = 0;
        try {
            AnalyticsProject storedProject = analyticsSettingStore.getProject();
            if (storedProject != null && storedProject.getId() != null) {
                for (int i = 0; i < projects.size(); i++) {
                    AnalyticsProject p = projects.get(i);
                    if (storedProject.getId().equals(p.getId())) {
                        selectionIndex = i;
                        return selectionIndex;
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.logError(e);
        }
        return selectionIndex;
    }
    
    public static AnalyticsProject createProject(String serverUrl, String projectName, AnalyticsTeam team,
            String accessToken) throws AnalyticsApiExeception {
        return AnalyticsApiProvider.createProject(serverUrl, projectName, team, accessToken);
    }
    
    public static AnalyticsTokenInfo requestToken(String serverUrl, String email, String password)
            throws AnalyticsApiExeception {
       return AnalyticsApiProvider.requestToken(serverUrl, email, password);
    }
}
