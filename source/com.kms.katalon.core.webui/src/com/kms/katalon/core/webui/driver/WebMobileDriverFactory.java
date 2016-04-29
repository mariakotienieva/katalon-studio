package com.kms.katalon.core.webui.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.AndroidServerFlag;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Platform;
import org.openqa.selenium.net.UrlChecker;
import org.openqa.selenium.net.UrlChecker.TimeoutException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;

import com.kms.katalon.core.configuration.RunConfiguration;
import com.kms.katalon.core.exception.StepFailedException;
import com.kms.katalon.core.logging.KeywordLogger;
import com.kms.katalon.core.webui.constants.StringConstants;
import com.kms.katalon.core.webui.exception.AppiumStartException;
import com.kms.katalon.core.webui.exception.IOSWebkitStartException;
import com.kms.katalon.core.webui.exception.MobileDriverInitializeException;

public class WebMobileDriverFactory {
    private static final String DEFAULT_APPIUM_SERVER_ADDRESS = "127.0.0.1";

    private static final String IOS_WEBKIT_DEBUG_PROXY_EXECUTABLE = "ios_webkit_debug_proxy";

    public static final String MOBILE_DRIVER_PROPERTY = StringConstants.CONF_PROPERTY_MOBILE_DRIVER;

    public static final String APPIUM_LOG_PROPERTY = StringConstants.CONF_APPIUM_LOG_FILE;

    private static final int DEFAULT_WEB_PROXY_PORT = 27753;

    private static final String IOS_WEBKIT_LOG_FILE_NAME = "appium-proxy-server.log";

    private static final String MSG_START_IOS_WEBKIT_SUCCESS = "ios_webkit_debug_proxy server started on port "
            + DEFAULT_WEB_PROXY_PORT;

    private static final String LOCALHOST_PREFIX = "http://localhost:";

    private static final ThreadLocal<Process> localStorageWebProxyProcess = new ThreadLocal<Process>() {
        @Override
        protected Process initialValue() {
            return null;
        }
    };

    private static final ThreadLocal<AppiumDriverLocalService> localStorageAppiumServer = new ThreadLocal<AppiumDriverLocalService>() {
        @Override
        protected AppiumDriverLocalService initialValue() {
            return null;
        }
    };

    private static final ThreadLocal<AppiumDriver<?>> localStorageAppiumDriver = new ThreadLocal<AppiumDriver<?>>() {
        @Override
        protected AppiumDriver<?> initialValue() {
            return null;
        }
    };

    public static void cleanup() throws InterruptedException, IOException {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            killProcessOnWin("adb.exe");
            killProcessOnWin("node.exe");
        } else {
            killProcessOnMac("adb");
            killProcessOnMac("node");
            killProcessOnMac("instruments");
            killProcessOnMac("deviceconsole");
            killProcessOnMac(IOS_WEBKIT_DEBUG_PROXY_EXECUTABLE);
        }
    }

    private static void killProcessOnWin(String processName) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder("taskkill", "/f", "/im", processName, "/t");
        pb.start().waitFor();
    }

    private static void killProcessOnMac(String processName) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder("killall", processName);
        pb.start().waitFor();
    }

    private static void ensureWebProxyServerStarted(String deviceId) throws IOException, InterruptedException,
            IOSWebkitStartException {
        if (!isWebProxyServerStarted(1)) {
            startWebProxyServer(deviceId);
        }
    }

    public static void startMobileDriver(String deviceId, WebUIDriverType WebUIDriverType) throws AppiumStartException,
            IOException, InterruptedException, MobileDriverInitializeException, IOSWebkitStartException {
        ensureServicesStarted(WebUIDriverType, deviceId);
        createMobileDriver(WebUIDriverType, deviceId);
    }

    private static void ensureServicesStarted(WebUIDriverType osType, String deviceId) throws IOException,
            InterruptedException, AppiumStartException {
        if (osType == WebUIDriverType.IOS_DRIVER) {
            // Proxy server is optional
            try {
                ensureWebProxyServerStarted(deviceId);
            } catch (IOException | InterruptedException | IOSWebkitStartException e) {
                KeywordLogger.getInstance().logInfo(e.getMessage());
            }
        }
        startAppiumServerJS(RunConfiguration.getTimeOut());
    }

    private static void startAppiumServerJS(int timeout) throws AppiumStartException, IOException {
        if (localStorageAppiumServer.get() != null && localStorageAppiumServer.get().isRunning()) {
            return;
        }
        String appiumHome = System.getenv("APPIUM_HOME");
        String appium = appiumHome + "/bin/appium.js";
        String appiumTemp = System.getProperty("java.io.tmpdir") + File.separator + "Katalon" + File.separator
                + "Appium" + File.separator + "Temp" + System.currentTimeMillis();
        int freePort = getFreePort();
        AppiumDriverLocalService service = AppiumDriverLocalService.buildService(new AppiumServiceBuilder().withArgument(
                GeneralServerFlag.LOG_LEVEL, "info")
                .withArgument(GeneralServerFlag.TEMP_DIRECTORY, appiumTemp)
                .withArgument(GeneralServerFlag.SESSION_OVERRIDE)
                .withAppiumJS(new File(appium))
                .withIPAddress(DEFAULT_APPIUM_SERVER_ADDRESS)
                .usingPort(freePort)
                .withArgument(GeneralServerFlag.CHROME_DRIVER_PORT, Integer.toString(getFreePort()))
                .withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, Integer.toString(getFreePort()))
                .withLogFile(new File(RunConfiguration.getAppiumLogFilePath()))
                .withStartUpTimeOut(new Long(timeout), TimeUnit.SECONDS));
        service.start();
        localStorageAppiumServer.set(service);
        KeywordLogger.getInstance().logInfo("Appium server started on port " + freePort);
    }

    private static boolean isServerStarted(int timeToWait, URL url) {
        try {
            new UrlChecker().waitUntilAvailable(timeToWait, TimeUnit.SECONDS, url);
            return true;
        } catch (TimeoutException ex1) {}
        return false;
    }

    private static boolean isWebProxyServerStarted(int timeOut) {
        if (localStorageWebProxyProcess.get() == null) {
            return false;
        }
        try {
            localStorageWebProxyProcess.get().exitValue();
            return false;
        } catch (IllegalThreadStateException e) {}
        try {
            return isServerStarted(timeOut, new URL("http://localhost:" + DEFAULT_WEB_PROXY_PORT));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static synchronized int getFreePort() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (IOException e) {
            // do nothing
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                // do nothing
            }
        }
        return -1;
    }

    /**
     * Start proxy server, this server is optional
     * 
     * @param deviceId
     * @throws Exception
     */
    private static void startWebProxyServer(String deviceId) throws IOException, InterruptedException,
            IOSWebkitStartException {
        String[] webProxyServerCmd = { IOS_WEBKIT_DEBUG_PROXY_EXECUTABLE, "-c", deviceId + ":" + DEFAULT_WEB_PROXY_PORT };
        ProcessBuilder webProxyServerProcessBuilder = new ProcessBuilder(webProxyServerCmd);
        webProxyServerProcessBuilder.redirectOutput(new File(
                new File(RunConfiguration.getAppiumLogFilePath()).getParent() + File.separator
                        + IOS_WEBKIT_LOG_FILE_NAME));

        Process webProxyProcess = webProxyServerProcessBuilder.start();

        // Check again if proxy server started
        if (!isServerStarted(10, new URL(LOCALHOST_PREFIX + DEFAULT_WEB_PROXY_PORT))) {
            throw new IOSWebkitStartException();
        }
        localStorageWebProxyProcess.set(webProxyProcess);
        KeywordLogger.getInstance().logInfo(MSG_START_IOS_WEBKIT_SUCCESS);
    }

    public static void quitServer() {
        if (localStorageAppiumServer.get() != null && localStorageAppiumServer.get().isRunning()) {
            localStorageAppiumServer.get().stop();
            localStorageAppiumServer.set(null);
        }
        if (localStorageAppiumDriver.get() != null) {
            localStorageAppiumDriver.get().quit();
        }
        if (localStorageWebProxyProcess.get() != null) {
            localStorageWebProxyProcess.get().destroy();
            localStorageWebProxyProcess.set(null);
        }
    }

    public static AppiumDriver<?> getDriver() throws StepFailedException {
        verifyWebDriverIsOpen();
        return localStorageAppiumDriver.get();
    }

    private static void verifyWebDriverIsOpen() throws StepFailedException {
        if (localStorageAppiumDriver.get() == null) {
            throw new StepFailedException("No application is started yet.");
        }
    }

    public static void closeDriver() {
        AppiumDriver<?> webDriver = localStorageAppiumDriver.get();
        if (null != webDriver && null != ((RemoteWebDriver) webDriver).getSessionId()) {
            webDriver.quit();
        }
        RunConfiguration.removeDriver(webDriver);
        localStorageAppiumDriver.set(null);
    }

    private static DesiredCapabilities toDesireCapabilities(Map<String, Object> propertyMap,
            WebUIDriverType WebUIDriverType) {
        DesiredCapabilities desireCapabilities = new DesiredCapabilities();
        for (Entry<String, Object> property : propertyMap.entrySet()) {
            KeywordLogger.getInstance().logInfo(
                    MessageFormat.format(StringConstants.KW_LOG_WEB_UI_PROPERTY_SETTING, property.getKey(),
                            property.getValue()));
            desireCapabilities.setCapability(property.getKey(), property.getValue());
        }
        return desireCapabilities;
    }

    private static DesiredCapabilities createCapabilities(WebUIDriverType osType, String deviceId) {

        DesiredCapabilities capabilities = new DesiredCapabilities();
        Map<String, Object> driverPreferences = RunConfiguration.getDriverPreferencesProperties(MOBILE_DRIVER_PROPERTY);

        if (driverPreferences != null && osType == WebUIDriverType.IOS_DRIVER) {
            capabilities.merge(toDesireCapabilities(driverPreferences, WebUIDriverType.IOS_DRIVER));
            capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Safari");
        } else if (driverPreferences != null && osType == WebUIDriverType.ANDROID_DRIVER) {
            capabilities.merge(toDesireCapabilities(driverPreferences, WebUIDriverType.ANDROID_DRIVER));
            capabilities.setPlatform(Platform.ANDROID);
            capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Chrome");
        }
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, deviceId);
        capabilities.setCapability("udid", deviceId);
        capabilities.setCapability("newCommandTimeout", 1800);
        // capabilities.setCapability("waitForAppScript", true);
        return capabilities;
    }

    @SuppressWarnings("rawtypes")
    private static void createMobileDriver(WebUIDriverType osType, String deviceId)
            throws MobileDriverInitializeException, MalformedURLException {
        AppiumDriverLocalService appiumService = localStorageAppiumServer.get();
        if (appiumService == null) {
            throw new MobileDriverInitializeException("Appium server is not started");
        }
        URL appiumServerUrl = new URL(appiumService.getUrl().toString());
        DesiredCapabilities capabilities = createCapabilities(osType, deviceId);
        int time = 0;
        long currentMilis = System.currentTimeMillis();
        AppiumDriver<?> appiumDriver = null;
        while (time < RunConfiguration.getTimeOut()) {
            try {
                if (osType == WebUIDriverType.IOS_DRIVER) {
                    appiumDriver = new IOSDriver(appiumServerUrl, capabilities);
                } else if (osType == WebUIDriverType.ANDROID_DRIVER) {
                    appiumDriver = new SwipeableAndroidDriver(appiumServerUrl, capabilities);
                }
                localStorageAppiumDriver.set(appiumDriver);
                return;
            } catch (UnreachableBrowserException e) {
                long newMilis = System.currentTimeMillis();
                time += ((newMilis - currentMilis) / 1000);
                currentMilis = newMilis;
            }
        }
        throw new MobileDriverInitializeException("Could not connect to appium server after "
                + RunConfiguration.getTimeOut() + " seconds");
    }
}
