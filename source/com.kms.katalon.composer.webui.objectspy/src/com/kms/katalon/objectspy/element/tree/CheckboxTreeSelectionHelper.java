package com.kms.katalon.objectspy.element.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.kms.katalon.objectspy.constants.StringConstants;
import com.kms.katalon.objectspy.element.HTMLPageElement;

/**
 * Adds "standard" selection behavior to CheckboxTreeSelectionHelper:
 * checking an element affects the following changes:
 * <ol>
 * <li>changes all descendants to the same check state</li>
 * <li>if all siblings of the element are selected (and the element itself), sets parent element to checked state</li>
 * <li>if some but not all siblings are in a checked state (including the element itself), parent is grayed</li>
 * <li>if all siblings (and the element itself) are unchecked, parent is unchecked</li>
 * <li>previous logic is applied to all ancestors of the element until the root is reached</li>
 * </ol>
 * 
 * Example usage: <br>
 * <br>
 * <code>
 * 	CheckboxTreeSelectionHelper.attach(myCheckboxTreeViewer, myContentProvider);<br>
 *  myCheckboxTreeViewer.setInput(myInput);
 * </code>
 */
public class CheckboxTreeSelectionHelper {

    /** The viewer. */
    private CheckboxTreeViewer viewer;

    /** The content provider. */
    private ITreeContentProvider contentProvider;

    /**
     * Instantiates a new checkbox tree selection helper.
     *
     * @param viewer the viewer
     * @param contentProvider the content provider
     */
    private CheckboxTreeSelectionHelper(CheckboxTreeViewer viewer, ITreeContentProvider contentProvider) {
        this.viewer = viewer;
        if (contentProvider == null)
            throw new IllegalArgumentException(StringConstants.TREE_ERROR_MSG_CONTENT_PROVIDER_IS_REQUIRED);

        this.contentProvider = contentProvider;
        init();
    }

    /**
     * Inits the listener and sets the content provider if necessary.
     */
    private void init() {

        viewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {

                Object node = event.getElement();
                if (viewer.getGrayed(node)) {
                    viewer.setGrayChecked(node, false);
                }

                List<Object> checkedElements = getCheckedElements();
                updateTree(node, checkedElements, event.getChecked());
            }
        });

        if (viewer.getContentProvider() != contentProvider)
            viewer.setContentProvider(contentProvider);
    }

    /**
     * Attach.
     *
     * @param viewer the viewer
     * @param contentProvider the content provider
     * @return the checkbox tree selection helper
     */
    public static CheckboxTreeSelectionHelper attach(CheckboxTreeViewer viewer, ITreeContentProvider contentProvider) {
        return new CheckboxTreeSelectionHelper(viewer, contentProvider);
    }

    /**
     * Update tree.
     *
     * @param node the node
     * @param checkedElements the checked elements
     * @param checked the checked
     */
    private void updateTree(Object node, List<Object> checkedElements, boolean checked) {

        List<Object> descendants = getDescendants(node);
        Set<Object> checkedSet = new HashSet<Object>(checkedElements);
        for (Object n : descendants) {
            viewer.setGrayChecked(n, false);
            viewer.setChecked(n, checked);
            if (checked)
                checkedSet.add(n);
            else checkedSet.remove(n);
        }

        updateAncestors(node, checkedSet);
    }

    /**
     * Update ancestors.
     *
     * @param child the child
     * @param checkedElements the checked elements
     */
    private void updateAncestors(Object child, Set<Object> checkedElements) {
        Object parent = contentProvider.getParent(child);
        if (parent == null)
            return;

        boolean isGreyed = viewer.getChecked(child) && viewer.getGrayed(child);
        if (isGreyed) {
            // if child is greyed then everying up should be greyed as well
            viewer.setGrayChecked(parent, true);
        } else {
            Object[] children = contentProvider.getChildren(parent);
            List<Object> cloned = new ArrayList<Object>();
            cloned.addAll(Arrays.asList(children));
            cloned.removeAll(checkedElements);
            if (cloned.isEmpty()) {
                // every child is checked
                viewer.setGrayed(parent, false);
                viewer.setChecked(parent, true);
                checkedElements.add(parent);
            } else {

                if (viewer.getChecked(parent) && !viewer.getGrayed(parent))
                    checkedElements.remove(parent);

                viewer.setGrayChecked(parent, false);

                // some children selected but not all
                if (cloned.size() < children.length)
                    viewer.setGrayChecked(parent, true);

            }
        }
        updateAncestors(parent, checkedElements);
    }

    /**
     * Gets the descendants.
     *
     * @param node the node
     * @return the descendants
     */
    private List<Object> getDescendants(Object node) {
        List<Object> desc = new ArrayList<Object>();
        getDescendantsHelper(desc, node);
        return desc;
    }

    /**
     * Gets the descendants helper.
     *
     * @param descendants the descendants
     * @param node the node
     * @return the descendants helper
     */
    private void getDescendantsHelper(List<Object> descendants, Object node) {
        Object[] children = contentProvider.getChildren(node);
        if (children == null || children.length == 0)
            return;
        descendants.addAll(Arrays.asList(children));
        for (Object child : children) {
            getDescendantsHelper(descendants, child);
        }
    }

    /**
     * Gets the checked elements (excluding grayed out elements).
     *
     * @return the checked elements
     */
    public List<Object> getCheckedElements() {
        List<Object> checkedElements = new ArrayList<Object>(Arrays.asList(viewer.getCheckedElements()));
        checkedElements.removeAll(getGrayedElements());
        return checkedElements;
    }

    /**
     * Gets the grayed elements.
     *
     * @return the grayed elements
     */
    public List<Object> getGrayedElements() {
        return Arrays.asList(viewer.getGrayedElements());
    }
    /**
     * checked = true  : Check all items. </br>
     * checked = false : Uncheck all items.</br>
     * @param checked
     */
    public void setCheckAllItems(boolean checked) {
        Object input = viewer.getInput();

        if (input == null) {
            return;
        }

        List<?> inputList = null;
        if (input != null && input.getClass().isArray()) {
            inputList = Arrays.asList((Object[]) input);
        } else if (input != null && input instanceof List<?>) {
            inputList = (List<?>) input;
        } else {
            return;
        }

        for (Object item : (List<?>) inputList) {
            setCheckItem(item, checked);
        }
    }

    public void setCheckItem(Object item, boolean checked) {
        viewer.setChecked(item, checked);
        List<Object> descendants = getDescendants(item);
        for (Object node : descendants) {
            viewer.setChecked(node, checked);
        }
    }

    public void checkAllItemInTree(List<HTMLPageElement> elements) {
        for (HTMLPageElement element : elements) {
            viewer.setChecked(element, true);
            List<Object> descendants = getDescendants(element);
            for (Object node : descendants) {
                viewer.setChecked(node, true);
            }
        }
    }
}
