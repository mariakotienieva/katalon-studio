package com.kms.katalon.composer.testcase.ast.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.testcase.constants.StringConstants;
import com.kms.katalon.composer.testcase.model.ICustomInputValueType;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.composer.testcase.providers.AstInputConstantTypeLabelProvider;
import com.kms.katalon.composer.testcase.providers.AstInputTypeLabelProvider;
import com.kms.katalon.composer.testcase.providers.AstInputValueLabelProvider;
import com.kms.katalon.composer.testcase.support.AstInputBuilderConstantTypeColumnSupport;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueColumnSupport;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueTypeColumnSupport;
import com.kms.katalon.composer.testcase.util.AstTreeTableEntityUtil;
import com.kms.katalon.core.groovy.GroovyParser;

public class RangeInputBuilderDialog extends AbstractAstBuilderWithTableDialog {
    private final InputValueType[] defaultInputValueTypes = { InputValueType.Constant, InputValueType.Variable,
            InputValueType.GlobalVariable, InputValueType.TestDataValue, InputValueType.MethodCall,
            InputValueType.Binary, InputValueType.Property };

    private static final String DIALOG_TITLE = StringConstants.DIA_TITLE_RANGE_INPUT;
    private static final String[] COLUMN_NAMES = new String[] { StringConstants.DIA_COL_OBJ,
            StringConstants.DIA_COL_VALUE_TYPE, StringConstants.DIA_COL_CONSTANT_TYPE, StringConstants.DIA_COL_VALUE };

    private RangeExpression rangeExpression;
    private Expression fromExpression;
    private Expression toExpression;

    public RangeInputBuilderDialog(Shell parentShell, RangeExpression rangeExpression, ClassNode scriptClass) {
        super(parentShell, scriptClass);
        if (rangeExpression != null) {
            this.rangeExpression = GroovyParser.cloneRangeExpression(rangeExpression);
        } else {
            this.rangeExpression = AstTreeTableEntityUtil.getNewRangeExpression();
        }
        fromExpression = this.rangeExpression.getFrom();
        toExpression = this.rangeExpression.getTo();
    }

    @Override
    public void refresh() {
        List<Object> expressionList = new ArrayList<Object>();
        expressionList.add(fromExpression);
        expressionList.add(toExpression);
        rangeExpression = new RangeExpression(fromExpression, toExpression, true);
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setInput(expressionList);
    }

    @Override
    public RangeExpression getReturnValue() {
        return rangeExpression;
    }

    @Override
    public void changeObject(Object originalObject, Object newObject) {
        if (newObject instanceof Expression) {
            if (originalObject == fromExpression) {
                fromExpression = (Expression) newObject;
                refresh();
            } else if (originalObject == toExpression) {
                toExpression = (Expression) newObject;
                refresh();
            }
        }
    }

    @Override
    public String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Override
    protected void addTableColumns() {
        TableViewerColumn tableViewerColumnObject = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnObject.getColumn().setWidth(100);
        tableViewerColumnObject.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element == fromExpression) {
                    return "From Expression";
                } else if (element == toExpression) {
                    return "To Expression";
                }
                return StringUtils.EMPTY;
            }
        });

        TableViewerColumn tableViewerColumnValueType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnValueType.getColumn().setWidth(100);
        tableViewerColumnValueType.setLabelProvider(new AstInputTypeLabelProvider(scriptClass));
        tableViewerColumnValueType.setEditingSupport(new AstInputBuilderValueTypeColumnSupport(tableViewer,
                defaultInputValueTypes, ICustomInputValueType.TAG_RANGE, this, scriptClass));

        TableViewerColumn tableViewerColumnConstantType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnConstantType.getColumn().setWidth(100);
        tableViewerColumnConstantType.setLabelProvider(new AstInputConstantTypeLabelProvider());
        tableViewerColumnConstantType
                .setEditingSupport(new AstInputBuilderConstantTypeColumnSupport(tableViewer, this));

        TableViewerColumn tableViewerColumnValue = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnValue.getColumn().setWidth(300);
        tableViewerColumnValue.setLabelProvider(new AstInputValueLabelProvider(scriptClass));
        tableViewerColumnValue.setEditingSupport(new AstInputBuilderValueColumnSupport(tableViewer, this, scriptClass));

        // set column's name
        for (int i = 0; i < tableViewer.getTable().getColumnCount(); i++) {
            tableViewer.getTable().getColumn(i).setText(COLUMN_NAMES[i]);
        }

    }
}
