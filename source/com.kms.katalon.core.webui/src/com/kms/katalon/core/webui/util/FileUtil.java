package com.kms.katalon.core.webui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;

import com.kms.katalon.core.exception.StepFailedException;
import com.kms.katalon.core.logging.KeywordLogger;
import com.kms.katalon.core.webui.driver.DriverFactory;

public class FileUtil {

	private static final String SCREENSHOT_FOLDER = "resources/screen";

	public static String takesScreenshot() throws IOException, WebDriverException, StepFailedException {
		String fileName = System.currentTimeMillis() + ".png";
		FileUtils.copyFile(((TakesScreenshot) DriverFactory.getWebDriver()).getScreenshotAs(OutputType.FILE), new File(
				new File(KeywordLogger.getInstance().getLogFilePath()).getParent() + File.separator + fileName), false);
		return fileName;
	}

	public static File extractScreenFiles() throws Exception {
		String path = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		path = URLDecoder.decode(path, "utf-8");
		File jarFile = new File(path);
		if (jarFile.isFile()) {
			JarFile jar = new JarFile(jarFile);
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry jarEntry = entries.nextElement();
				String name = jarEntry.getName();
				if (name.startsWith(SCREENSHOT_FOLDER) && name.endsWith(".png")) {
					String mappingFileName = name.replace(SCREENSHOT_FOLDER + "/", "");
					File tmpFile = new File(System.getProperty("java.io.tmpdir") + mappingFileName);
					if (tmpFile.exists()) {
						tmpFile.delete();
					}
					FileOutputStream fos = new FileOutputStream(tmpFile);
					IOUtils.copy(jar.getInputStream(jarEntry), fos);
					fos.flush();
					fos.close();
				}
			}
			jar.close();
			return new File(System.getProperty("java.io.tmpdir"));
		} else { // Run with IDE
			File folder = new File(path + "../" + SCREENSHOT_FOLDER);
			return folder;
		}
	}
}