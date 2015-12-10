package com.kms.katalon.composer.testcase.ast.editors;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.testcase.ast.dialogs.AstBuilderDialog;
import com.kms.katalon.composer.testcase.ast.dialogs.ThrowableInputBuilderDialog;

public class ThrowableInputCellEditor extends AbstractAstDialogCellEditor {

	public ThrowableInputCellEditor(Composite parent, String defaultContent, ClassNode scriptClass) {
		super(parent, defaultContent, scriptClass);
		this.setValidator(new ICellEditorValidator() {
			@Override
			public String isValid(Object value) {
				if (value instanceof ConstructorCallExpression || value == null) {
					return null;
				}
				return getValidatorMessage(ConstructorCallExpression.class.getName());
			}
		});
	}

	@Override
	protected AstBuilderDialog getDialog(Shell shell) {
		return new ThrowableInputBuilderDialog(shell, (getValue() == null) ? null : (ConstructorCallExpression) getValue(), scriptClass);
	}

}