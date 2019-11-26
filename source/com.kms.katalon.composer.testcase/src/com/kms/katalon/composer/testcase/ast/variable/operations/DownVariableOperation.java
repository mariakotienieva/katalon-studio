package com.kms.katalon.composer.testcase.ast.variable.operations;

import com.kms.katalon.composer.testcase.parts.TableActionOperator;

public class DownVariableOperation extends UpVariableOperation {
    public DownVariableOperation(TableActionOperator testCaseVariablePart) {
        super(DownVariableOperation.class.getName(), testCaseVariablePart);
    }

    @Override
    protected int getOffset() {
        return 1;
    }
    
    @Override
    protected boolean isOutOfBound(int index) {
        return index >= variableList.size() - 1;
    }
}