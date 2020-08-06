package com.kms.katalon.composer.toolbar;

import java.text.MessageFormat;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.kms.katalon.application.constants.ApplicationStringConstants;
import com.kms.katalon.application.utils.ApplicationInfo;
import com.kms.katalon.composer.components.impl.control.DropdownToolItemSelectionListener;
import com.kms.katalon.composer.handlers.DeactivateHandler;
import com.kms.katalon.composer.handlers.KatalonStoreLoginHandler;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.handlers.KatalonStoreLoginHandler;
import com.kms.katalon.composer.handlers.ManageKStoreCLIKeysHandler;
import com.kms.katalon.composer.handlers.ManagePluginsHandler;
import com.kms.katalon.composer.handlers.OpenPlanGridExecutionHandler;
import com.kms.katalon.composer.handlers.OpenPluginHelpPageHandler;
import com.kms.katalon.composer.handlers.ReloadPluginsHandler;
import com.kms.katalon.composer.handlers.SearchPluginsHandler;
import com.kms.katalon.composer.handlers.ViewDashboardHandler;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.ImageConstants;
import com.kms.katalon.constants.StringConstants;

public class PluginStoreToolControl {

    @Inject
    IEventBroker eventBroker;

    @PostConstruct
    void createWidget(Composite parent, MToolControl toolControl) {
        ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
        toolbar.setForeground(ColorUtil.getToolBarForegroundColor());
        ToolItem accountToolItem = new ToolItem(toolbar, SWT.DROP_DOWN);
        accountToolItem.setImage(ImageConstants.IMG_KATALON_ACCOUNT_24);
        accountToolItem.addSelectionListener(new DropdownToolItemSelectionListener() {

            @Override
            protected Menu getMenu() {
                Menu menu = new Menu(toolbar);

                if (!isLoggedIn()) {
                    MenuItem loginMenuItem = new MenuItem(menu, SWT.PUSH);
                    loginMenuItem.setText(StringConstants.Account_LOG_IN);
                    loginMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new KatalonStoreLoginHandler().execute();
                        }
                    });

                    new MenuItem(menu, SWT.SEPARATOR);
                }

                if (isLoggedIn()) {
                    MenuItem userNameMenuItem = new MenuItem(menu, SWT.PUSH);
                    String userName = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_EMAIL);
                    userNameMenuItem.setText(MessageFormat.format(StringConstants.Account_LOGIN_AS, userName));

                    new MenuItem(menu, SWT.SEPARATOR);

                    MenuItem viewDashboardMenuItem = new MenuItem(menu, SWT.PUSH);
                    viewDashboardMenuItem.setText(StringConstants.Account_VIEW_DASHBOARD);
                    viewDashboardMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new ViewDashboardHandler().execute();
                        }
                    });

                    MenuItem planGridExecutionMenuItem = new MenuItem(menu, SWT.PUSH);
                    planGridExecutionMenuItem.setText(StringConstants.Account_PLAN_GRID_EXECUTION);
                    planGridExecutionMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new OpenPlanGridExecutionHandler().execute();
                        }
                    });

                    new MenuItem(menu, SWT.SEPARATOR);
                }

                if (isLoggedIn()) {
                    MenuItem visitStoreMenuItem = new MenuItem(menu, SWT.PUSH);
                    visitStoreMenuItem.setText(StringConstants.Account_VISIT_PLUGIN);
                    visitStoreMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new SearchPluginsHandler().execute();
                        }
                    });
                }

                MenuItem reloadPluginMenuItem = new MenuItem(menu, SWT.PUSH);
                reloadPluginMenuItem.setText(StringConstants.Account_RELOAD_PLUGIN);
                reloadPluginMenuItem.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        new ReloadPluginsHandler().execute();
                    }
                });

                if (isLoggedIn()) {
                    MenuItem managePluginMenuItem = new MenuItem(menu, SWT.PUSH);
                    managePluginMenuItem.setText(StringConstants.Account_MANAGE_PLUGIN);
                    managePluginMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new ManagePluginsHandler().execute();
                        }
                    });
                }

                if (isLoggedIn()) {
                    new MenuItem(menu, SWT.SEPARATOR);

                    MenuItem manageApiKeyMenuItem = new MenuItem(menu, SWT.PUSH);
                    manageApiKeyMenuItem.setText(StringConstants.Account_MANAGE_API_KEY);
                    manageApiKeyMenuItem.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            new ManageKStoreCLIKeysHandler().execute();
                        }
                    });
                }

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem logoutMenuItem = new MenuItem(menu, SWT.PUSH);
                logoutMenuItem.setText(StringConstants.Account_DEACTIVATE);
                logoutMenuItem.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        new DeactivateHandler().execute();
                    }
                });

                MenuItem helpMenuItem = new MenuItem(menu, SWT.PUSH);
                helpMenuItem.setText(StringConstants.Account_HELP);
                helpMenuItem.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        new OpenPluginHelpPageHandler().execute();
                    }
                });

                return menu;
            }
        });

        eventBroker.subscribe(EventConstants.ACTIVATION_CHECKED, new EventHandler() {

            @Override
            public void handleEvent(Event event) {
                // PluginPreferenceStore store = new PluginPreferenceStore();
                // if (store.hasReloadedPluginsBefore()) {
                // new ReloadPluginsHandler().reloadPlugins(true);
                // } else {
                // eventBroker.post(EventConstants.WORKSPACE_PLUGIN_LOADED,
                // null);
                // }
            }
        });

        eventBroker.subscribe(EventConstants.PROJECT_OPENED, new EventHandler() {

            @Override
            public void handleEvent(Event event) {
                new ReloadPluginsHandler().reloadPlugins(true);
            }
        });
    }

    private boolean isLoggedIn() {
        String username = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_EMAIL);
        String encryptedPassword = ApplicationInfo.getAppProperty(ApplicationStringConstants.ARG_PASSWORD);
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(encryptedPassword);
    }
}