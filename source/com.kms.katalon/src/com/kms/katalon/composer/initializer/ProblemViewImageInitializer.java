package com.kms.katalon.composer.initializer;

import java.net.MalformedURLException;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.kms.katalon.composer.resources.constants.IImageKeys;
import com.kms.katalon.composer.resources.image.ImageManager;
import com.kms.katalon.composer.resources.util.ImageUtil;
import com.kms.katalon.logging.LogUtil;

@SuppressWarnings("restriction")
public class ProblemViewImageInitializer implements ApplicationInitializer {

    @Override
    public void setup() {
        IEclipseContext context = PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
        context.set("org.eclipse.ui.ISharedImages", new CSharedImages());
    }

    private class CSharedImages implements ISharedImages {
        private static final String ECLIPSE_IDE_BUNDLE_ID = "org.eclipse.ui.ide";

        // TreeItem Images
        private static final String IMG_SHOWINFO_TSK_URL = "$nl$/icons/full/elcl16/showinfo_tsk.png";

        private static final String IMG_SHOWWARN_TSK_URL = "$nl$/icons/full/elcl16/showwarn_tsk.png";

        private static final String IMG_SHOWERR_TSK_URL = "$nl$/icons/full/elcl16/showerr_tsk.png";

        // ProblemView Icons
        private static final String IMG_PROBLEMS_VIEW_WARNING_URL = "$nl$/icons/full/eview16/problems_view_warning.png";

        private static final String IMG_PROBLEMS_VIEW_INFO_URL = "$nl$/icons/full/eview16/problems_view_info.png";

        private static final String IMG_PROBLEMS_VIEW_ERROR_URL = "$nl$/icons/full/eview16/problems_view_error.png";

        private static final String IMG_PROBLEMS_VIEW_URL = "$nl$/icons/full/eview16/problems_view.png";

        private ImageRegistry imageRegistry;

        public CSharedImages() {
            imageRegistry = new ImageRegistry();
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW,
                    getProblemImage(IMG_PROBLEMS_VIEW_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_ERROR,
                    getProblemImage(IMG_PROBLEMS_VIEW_ERROR_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_INFO,
                    getProblemImage(IMG_PROBLEMS_VIEW_INFO_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_WARNING,
                    getProblemImage(IMG_PROBLEMS_VIEW_WARNING_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, getProblemImage(IMG_SHOWERR_TSK_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_OBJS_WARNING_PATH, getProblemImage(IMG_SHOWWARN_TSK_URL));
            imageRegistry.put(IDEInternalWorkbenchImages.IMG_OBJS_INFO_PATH, getProblemImage(IMG_SHOWINFO_TSK_URL));

            try {
                // Preference Dialog
                imageRegistry.put(ISharedImages.IMG_TOOL_BACK, 
                        ImageUtil.loadImageDescriptor(ImageManager.getImageURLString(IImageKeys.MOVE_LEFT_16)));
                imageRegistry.put(ISharedImages.IMG_TOOL_FORWARD,
                        ImageUtil.loadImageDescriptor(ImageManager.getImageURLString(IImageKeys.MOVE_RIGHT_16)));
            } catch (MalformedURLException e) {
                LogUtil.logError(e);
            }
        }

        private ImageDescriptor getProblemImage(String url) {
            return AbstractUIPlugin.imageDescriptorFromPlugin(ECLIPSE_IDE_BUNDLE_ID, url);
        }

        @Override
        public Image getImage(String key) {
            Image image = imageRegistry.get(key);
            if (image != null) {
                return image;
            }
            image = JavaPluginImages.get(key);
            if (image != null) {
                return image;
            }
            return getWorkbenchImage(key);
        }
        
        private Image getWorkbenchImage(String symbolicName) {
            Image image = WorkbenchImages.getImage(symbolicName);
            if (image != null) {
                return image;
            }

            //if there is a descriptor for it, add the image to the registry.
            ImageDescriptor desc = WorkbenchImages.getImageDescriptor(symbolicName);
            if (desc != null) {
                WorkbenchImages.getImageRegistry().put(symbolicName, desc);
                return WorkbenchImages.getImageRegistry().get(symbolicName);
            }
            return null;
        }
        

        @Override
        public ImageDescriptor getImageDescriptor(String symbolicName) {
            ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(symbolicName);
            if (imageDescriptor != null) {
                return imageDescriptor;
            }
            imageDescriptor = JavaPluginImages.getDescriptor(symbolicName);
            if (imageDescriptor != null) {
                return imageDescriptor;
            }
            return WorkbenchImages.getImageDescriptor(symbolicName);
        }
    }
}