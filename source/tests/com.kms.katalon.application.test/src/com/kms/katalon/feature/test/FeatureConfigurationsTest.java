package com.kms.katalon.feature.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.kms.katalon.application.helper.LicenseHelper;
import com.kms.katalon.application.helper.LicenseHelperFactory;
import com.kms.katalon.feature.FeatureConfigurations;
import com.kms.katalon.feature.KSEFeature;

public class FeatureConfigurationsTest {

    @Mock
    private LicenseHelper licenseHelperFree;

    @Mock
    private LicenseHelper licenseHelperEnterprise;

    private static final KSEFeature FREE_FEATURE = KSEFeature.DEBUG_MODE;

    private static final KSEFeature ENTERPRISE_FEATURE = KSEFeature.SMART_XPATH;

    @Spy
    private FeatureConfigurations featureService = new FeatureConfigurations();

    private static final String CORE_FEATURES_FIELD = "coreFeatures";

    private static final String CUSTOM_FEATURES_FIELD = "customFeatures";

    private static final String LOAD_FEATURES_METHOD = "loadFeatures";

    @SuppressWarnings("serial")
    private static final Map<KSEFeature, Boolean> featuresMap = new HashMap<KSEFeature, Boolean>() {
        {
            put(ENTERPRISE_FEATURE, Boolean.TRUE);
            put(FREE_FEATURE, Boolean.FALSE);
        }
    };

    @Before
    public void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        MockitoAnnotations.initMocks(this);
        setUpFeatureService();

        Mockito.when(licenseHelperFree.isFreeLicense()).thenReturn(true);
        Mockito.when(licenseHelperFree.isNonPaidLicense()).thenReturn(true);
        Mockito.when(licenseHelperFree.isNotFreeLicense()).thenReturn(false);
        Mockito.when(licenseHelperFree.isPaidLicense()).thenReturn(false);

        Mockito.when(licenseHelperEnterprise.isFreeLicense()).thenReturn(false);
        Mockito.when(licenseHelperEnterprise.isNonPaidLicense()).thenReturn(false);
        Mockito.when(licenseHelperEnterprise.isNotFreeLicense()).thenReturn(true);
        Mockito.when(licenseHelperEnterprise.isPaidLicense()).thenReturn(true);
        LicenseHelperFactory.set(licenseHelperEnterprise);
    }

    private void setUpFeatureService() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Mockito.doReturn(featuresMap).when(featureService).getCoreFeaturesMap();

        Class<?> featureServiceClass = FeatureConfigurations.class;

        Field coreFeaturesField = featureServiceClass.getDeclaredField(CORE_FEATURES_FIELD);
        coreFeaturesField.setAccessible(true);
        coreFeaturesField.set(featureService, null);

        Field customFeaturesField = featureServiceClass.getDeclaredField(CUSTOM_FEATURES_FIELD);
        customFeaturesField.setAccessible(true);
        customFeaturesField.set(featureService, null);

        Method loadFeaturesMethod = featureServiceClass.getDeclaredMethod(LOAD_FEATURES_METHOD);
        loadFeaturesMethod.setAccessible(true);
        loadFeaturesMethod.invoke(featureService);
    }

    @Test
    public void loadFeaturesTest() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, NoSuchMethodException, InvocationTargetException {
        // Given
        Mockito.doReturn(featuresMap).when(featureService).getCoreFeaturesMap();

        Class<?> featureServiceClass = FeatureConfigurations.class;

        Field coreFeaturesField = featureServiceClass.getDeclaredField(CORE_FEATURES_FIELD);
        coreFeaturesField.setAccessible(true);
        coreFeaturesField.set(featureService, null);
        Assert.assertNull(coreFeaturesField.get(featureService));

        Field customFeaturesField = featureServiceClass.getDeclaredField(CUSTOM_FEATURES_FIELD);
        customFeaturesField.setAccessible(true);
        customFeaturesField.set(featureService, null);
        Assert.assertNull(customFeaturesField.get(featureService));

        // When
        Method loadFeaturesMethod = featureServiceClass.getDeclaredMethod(LOAD_FEATURES_METHOD);
        loadFeaturesMethod.setAccessible(true);
        loadFeaturesMethod.invoke(featureService);

        // Then
        Properties coreFeatures = (Properties) coreFeaturesField.get(featureService);
        Properties customFeatures = (Properties) customFeaturesField.get(featureService);

        Assert.assertNotNull("The coreFeatures field must be not null", coreFeatures);
        Assert.assertNotNull("The customFeatures field must be not null", customFeatures);

        Assert.assertTrue(String.format("The coreFeatures must contains key \"%s\"", FREE_FEATURE),
                coreFeatures.containsKey(FREE_FEATURE.name()));
        Assert.assertTrue(String.format("The coreFeatures must contains key \"%s\"", ENTERPRISE_FEATURE),
                coreFeatures.containsKey(ENTERPRISE_FEATURE.name()));
    }

    @Test
    public void enableTest() {
        // Given
        String customFeature = "CustomFeature";
        featureService.clear();
        Assert.assertFalse(featureService.canUse(customFeature));

        // When
        featureService.enable(customFeature);

        // Then
        Assert.assertTrue(String.format("The \"%s\" must be enabled", customFeature),
                featureService.canUse(customFeature));
    }

    @Test
    public void disableTest() {
        // Given
        String customFeature = "CustomFeature";
        featureService.clear();
        featureService.enable(customFeature);
        Assert.assertTrue(featureService.canUse(customFeature));

        // When
        featureService.disable(customFeature);

        // Then
        Assert.assertFalse(String.format("The \"%s\" must be disabled", customFeature),
                featureService.canUse(customFeature));
    }

    @Test(expected = Test.None.class)
    public void canUseCustomFeatureTest() {
        // Given
        String customFeature = "CustomFeature";
        featureService.clear();
        Assert.assertFalse(featureService.canUse(customFeature));

        // When
        featureService.enable(customFeature);
        boolean canUse = featureService.canUse(customFeature);

        // Then
        Assert.assertTrue(String.format("The \"%s\" must be usable", customFeature), canUse);
    }

    @Test
    public void canUseFreeLicenseTest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Given
        LicenseHelperFactory.set(licenseHelperFree);

        // When
        setUpFeatureService();

        // Then
        Assert.assertTrue("Free users must be able to use free features", featureService.canUse(FREE_FEATURE));
        Assert.assertFalse("Free users must not be able to use enterprise features",
                featureService.canUse(ENTERPRISE_FEATURE));
    }

    @Test
    public void canUseEnterpriseLicenseTest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Given
        LicenseHelperFactory.set(licenseHelperEnterprise);

        // When
        setUpFeatureService();

        // Then
        Assert.assertTrue("Enterprise users must be able to use free features",
                featureService.canUse(FREE_FEATURE));
        Assert.assertTrue("Enterprise users must be able to use enterprise features",
                featureService.canUse(ENTERPRISE_FEATURE));
    }

    @Test
    public void clearTest()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        // Given
        String customFeature = "CustomFeature";
        featureService.enable(customFeature);

        Class<?> featureServiceClass = FeatureConfigurations.class;
        
        Field customFeaturesField = featureServiceClass.getDeclaredField(CUSTOM_FEATURES_FIELD);
        customFeaturesField.setAccessible(true);
        Properties customFeatures = (Properties) customFeaturesField.get(featureService);
        Assert.assertThat(customFeatures.size(), Matchers.greaterThan(0));
        
        Field coreFeaturesField = featureServiceClass.getDeclaredField(CORE_FEATURES_FIELD);
        coreFeaturesField.setAccessible(true);
        Properties coreFeatures = (Properties) coreFeaturesField.get(featureService);
        int numCoreFeatures = coreFeatures.size();

        // When
        featureService.clear();

        // Then
        Assert.assertEquals("All custom features must be cleared", 0, customFeatures.size());
        Assert.assertEquals("All core features must be kept", numCoreFeatures, coreFeatures.size());
    }
}