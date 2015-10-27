package com.kms.katalon.composer.integration.qtest.wizard;

import java.util.ArrayList;
import java.util.List;

public class WizardManager {
    private List<IWizardPage> wizardPages;
    private int currentIdx;

    public WizardManager() {
        currentIdx = 0;
    }

    public WizardManager addPage(IWizardPage wizardPage) {
        getWizardPages().add(wizardPage);
        return this;
    }

    public IWizardPage nextPage() {
        if (currentIdx > getWizardPages().size() - 1) {
            return null;
        }
        currentIdx++;

        return getCurrentPage();
    }

    public IWizardPage backPage() {
        if (currentIdx <= 0) {
            return null;
        }
        currentIdx--;

        return getCurrentPage();
    }

    public IWizardPage getCurrentPage() {
        return wizardPages.get(currentIdx);
    }

    public List<IWizardPage> getWizardPages() {
        if (wizardPages == null) {
            wizardPages = new ArrayList<IWizardPage>();
        }
        return wizardPages;
    }

    public void setWizardPages(List<IWizardPage> wizardPages) {
        this.wizardPages = wizardPages;
    }
}