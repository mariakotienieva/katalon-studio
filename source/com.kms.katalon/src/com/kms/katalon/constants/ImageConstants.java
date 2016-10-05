package com.kms.katalon.constants;

import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.kms.katalon.composer.components.impl.control.HiDPISupportedImage;
import com.kms.katalon.composer.components.util.ImageUtil;

public class ImageConstants {

    public static final Bundle CURRENT_BUNDLE = FrameworkUtil.getBundle(ImageConstants.class);

    public static final Image IMG_BRANDING_BACKGROUND = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/branding_background.png");

    public static final Image IMG_BRANDING = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/branding_text.png");

    public static final Image IMG_NEW_PROJECT = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/new_project.png");

    public static final Image IMG_OPEN_PROJECT = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/open_project.png");

    public static final Image IMG_RECENT_PROJECT = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/recent_project.png");

    public static final Image IMG_FAQ = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/faqs.png");

    public static final Image IMG_GETTING_STARTED = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/getting_started.png");

    public static final Image IMG_HOW_TO_ARTICLES = ImageUtil.loadImage(CURRENT_BUNDLE, "/icons/how_to_articles.png");
    
    public static final String URI_IMG_WELCOME = "platform:/plugin/org.eclipse.ui.intro/icons/welcome16.gif";

    // FunctionsIntroductionDialog
    public static final Image IMG_INTRO_SCREEN_1 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen1.png");

    public static final Image IMG_INTRO_SCREEN_2 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen2.png");

    public static final Image IMG_INTRO_SCREEN_3 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen3.png");

    public static final Image IMG_INTRO_SCREEN_4 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen4.png");

    public static final Image IMG_INTRO_SCREEN_5 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen5.png");

    public static final Image IMG_INTRO_SCREEN_6 = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/intro/screen6.png");
    
    //Activation
    public static final Image KATALON_IMAGE = HiDPISupportedImage.loadImage(CURRENT_BUNDLE, "/icons/branding_16.png");
}