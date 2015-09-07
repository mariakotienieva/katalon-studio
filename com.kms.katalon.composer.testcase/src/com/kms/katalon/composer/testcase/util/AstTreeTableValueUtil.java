package com.kms.katalon.composer.testcase.util;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.eclipse.jdt.core.IType;

import com.kms.katalon.composer.components.impl.tree.TestCaseTreeEntity;
import com.kms.katalon.composer.components.impl.tree.TestDataTreeEntity;
import com.kms.katalon.composer.components.impl.tree.WebElementTreeEntity;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.controller.ObjectRepositoryController;
import com.kms.katalon.controller.TestCaseController;
import com.kms.katalon.controller.TestDataController;
import com.kms.katalon.core.groovy.GroovyParser;
import com.kms.katalon.core.model.FailureHandling;
import com.kms.katalon.entity.repository.WebElementEntity;
import com.kms.katalon.entity.testcase.TestCaseEntity;
import com.kms.katalon.entity.testdata.DataFileEntity;

public class AstTreeTableValueUtil {

	public static final int[] OPERATION_CODES = new int[] { Types.COMPARE_EQUAL, Types.COMPARE_GREATER_THAN,
			Types.COMPARE_LESS_THAN, Types.COMPARE_GREATER_THAN_EQUAL, Types.COMPARE_LESS_THAN_EQUAL,
			Types.COMPARE_NOT_EQUAL, Types.LOGICAL_AND, Types.LOGICAL_OR, Types.EQUALS, Types.PLUS, Types.MINUS,
			Types.MULTIPLY, Types.DIVIDE, Types.PLUS_EQUAL, Types.MINUS_EQUAL, Types.MULTIPLY_EQUAL,
			Types.DIVIDE_EQUAL, Types.LOGICAL_AND_EQUAL, Types.LOGICAL_OR_EQUAL, Types.INTDIV, Types.MOD,
			Types.STAR_STAR, Types.INTDIV_EQUAL, Types.MOD_EQUAL, Types.POWER_EQUAL, Types.COMPARE_IDENTICAL,
			Types.COMPARE_NOT_IDENTICAL, Types.COMPARE_TO, Types.LEFT_SHIFT, Types.LEFT_SHIFT_EQUAL, Types.RIGHT_SHIFT,
			Types.RIGHT_SHIFT_UNSIGNED, Types.RIGHT_SHIFT_EQUAL, Types.RIGHT_SHIFT_UNSIGNED_EQUAL };

	public static Object getValue(Object object, ClassNode scriptClass) {
		if (object instanceof Expression) {
			return getValue((Expression) object, scriptClass);
		} else if (object instanceof Statement) {
			return getValue((Statement) object, scriptClass);
		} else if (object instanceof Token) {
			return getValue((Token) object);
		} else if (object instanceof Parameter) {
			return getValue((Parameter) object);
		}
		return StringUtils.EMPTY;
	}

	private static Object getValue(Parameter parameter) {
		if (parameter != ForStatement.FOR_LOOP_DUMMY) {
			return parameter.getName();
		}
		return null;
	}

	private static Object getValue(Token token) {
		for (int index = 0; index < OPERATION_CODES.length; index++) {
			if (OPERATION_CODES[index] == token.getType()) {
				return index;
			}
		}
		return null;
	}

	private static Object getValue(Expression expression, ClassNode scriptClass) {
		if (expression instanceof VariableExpression) {
			return getValue((VariableExpression) expression);
		} else if (expression instanceof ConstantExpression) {
			return getValue((ConstantExpression) expression);
		} else if (expression instanceof PropertyExpression) {
			return getValue((PropertyExpression) expression, scriptClass);
		}
		return expression;
	}

	private static Object getValue(PropertyExpression propertyExpression, ClassNode scriptClass) {
		if (AstTreeTableInputUtil.isGlobalVariablePropertyExpression(propertyExpression)) {
			return AstTreeTableInputUtil.getGlobalVariableIndex(propertyExpression);
		}
		Class<?> type = AstTreeTableInputUtil.loadType(propertyExpression.getObjectExpression().getText(), scriptClass);
		if (type != null && type.getName().equals(FailureHandling.class.getName())) {
			for (int i = 0; i < type.getEnumConstants().length; i++) {
				if (propertyExpression.getPropertyAsString().equals(type.getEnumConstants()[i].toString())) {
					return i;
				}
			}
			return 0;
		}
		return propertyExpression;
	}

	private static Object getValue(ConstantExpression constantExpression) {
		if (constantExpression.getValue() instanceof Boolean) {
			if (((Boolean) constantExpression.getValue())) {
				return 0;
			} else {
				return 1;
			}
		} else if (constantExpression.getValue() != null) {
			return constantExpression.getText();
		}
		return "";
	}

	private static Object getValue(VariableExpression variableExpression) {
		return variableExpression.getText();
	}

	private static Object getValue(Statement statement, ClassNode scriptClass) {
		if (statement instanceof AssertStatement) {
			return ((AssertStatement) statement).getBooleanExpression();
		} else if (statement instanceof ExpressionStatement) {
			return getValue(((ExpressionStatement) statement).getExpression(), scriptClass);
		} else if (statement instanceof IfStatement) {
			return getValue((IfStatement) statement);
		} else if (statement instanceof ForStatement) {
			return statement;
		} else if (statement instanceof WhileStatement) {
			return ((WhileStatement) statement).getBooleanExpression();
		}
		return null;
	}

	private static Object getValue(IfStatement ifStatement) {
		BooleanExpression booleanExpression = null;
		if (ifStatement.getBooleanExpression() != null) {
			if (ifStatement.getBooleanExpression().getExpression() instanceof NotExpression) {
				booleanExpression = (NotExpression) ifStatement.getBooleanExpression().getExpression();
			} else {
				booleanExpression = ifStatement.getBooleanExpression();
			}
		}
		return booleanExpression;
	}

	public static Object setValue(Object object, Object value, ClassNode scriptClass) {
		if (object instanceof Expression) {
			return setValue((Expression) object, value, scriptClass);
		} else if (object instanceof Statement) {
			return setValue((Statement) object, value, scriptClass);
		} else if (object instanceof Token) {
			return setValue((Token) object, value);
		} else if (object instanceof Parameter) {
			return setValue((Parameter) object, value);
		}
		return null;
	}

	private static Object setValue(Parameter parameter, Object value) {
		if (value instanceof String) {
			return new Parameter(new ClassNode(Object.class), (String) value);
		}
		return null;
	}

	private static Object setValue(Token token, Object value) {
		if (value instanceof Integer) {
			return Token.newSymbol(OPERATION_CODES[(int) value], -1, -1);
		}
		return null;
	}

	private static Expression setValue(Expression expression, Object value, ClassNode scriptClass) {
		if (expression instanceof BooleanExpression) {
			return setValue((BooleanExpression) expression, value, scriptClass);
		} else if (expression instanceof VariableExpression) {
			return setValue((VariableExpression) expression, value);
		} else if (expression instanceof MethodCallExpression) {
			return setValue((MethodCallExpression) expression, value);
		} else if (expression instanceof ConstantExpression) {
			return setValue((ConstantExpression) expression, value);
		} else if (expression instanceof PropertyExpression) {
			return setValue((PropertyExpression) expression, value, scriptClass);
		} else if (value instanceof Expression) {
			return (Expression) value;
		}
		return null;
	}

	private static Expression setValue(PropertyExpression propertyExpression, Object value, ClassNode scriptClass) {
		if (value instanceof Integer) {
			if (AstTreeTableInputUtil.isGlobalVariablePropertyExpression(propertyExpression)) {
				return AstTreeTableInputUtil.getGlobalVariableExpression((int) value);
			}
			Class<?> type = AstTreeTableInputUtil.loadType(propertyExpression.getObjectExpression().getText(),
					scriptClass);
			if (type != null && type.getName().equals(FailureHandling.class.getName()) && ((int) value) >= 0
					&& ((int) value) < type.getEnumConstants().length) {
				return new PropertyExpression(propertyExpression.getObjectExpression(), new ConstantExpression(
						type.getEnumConstants()[(int) value].toString()));
			}
		}
		if (value instanceof PropertyExpression) {
			return (PropertyExpression) value;
		}
		if (value instanceof IType) {
			return AstTreeTableEntityUtil.createNewPropertyExpressionFromTypeName(((IType) value)
					.getFullyQualifiedName());
		}
		return null;
	}

	private static Expression setValue(VariableExpression variableExpression, Object value) {
		if (value instanceof String) {
			return new VariableExpression(String.valueOf(value));
		}
		if (value instanceof IType) {
			return AstTreeTableEntityUtil.createNewPropertyExpressionFromTypeName(((IType) value)
					.getFullyQualifiedName());
		}
		return null;
	}

	private static Expression setValue(MethodCallExpression methodCallExpression, Object value) {
		if (value instanceof MethodCallExpression) {
			return (MethodCallExpression) value;
		}
		try {
			if (value instanceof TestDataTreeEntity
					&& ((TestDataTreeEntity) value).getObject() instanceof DataFileEntity) {
				return setValueForTestData(value);
			}
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		try {
			if (value instanceof WebElementTreeEntity
					&& ((WebElementTreeEntity) value).getObject() instanceof WebElementEntity) {
				return setValueForTestObject(value);
			}
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		try {
			if (value instanceof TestCaseTreeEntity
					&& ((TestCaseTreeEntity) value).getObject() instanceof TestCaseEntity) {
				return setValueForCallTestCase(value);
			}
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		if (value instanceof VariableExpression) {
			return (Expression) value;
		}
		return null;
	}

	private static Expression setValueForTestData(Object value) {
		try {
			String testDataPk = TestDataController.getInstance().getIdForDisplay(
					(DataFileEntity) ((TestDataTreeEntity) value).getObject());
			return AstTreeTableEntityUtil.getNewTestDataExpression(new ConstantExpression(testDataPk));
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		return null;
	}

	private static Expression setValueForTestObject(Object value) {
		try {
			String objectPk = ObjectRepositoryController.getInstance().getIdForDisplay(
					(WebElementEntity) ((WebElementTreeEntity) value).getObject());
			return AstTreeTableInputUtil.generateObjectMethodCall(objectPk);
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		return null;
	}

	private static Expression setValueForCallTestCase(Object value) {
		try {
			return AstTreeTableInputUtil.generateTestCaseMethodCall(TestCaseController.getInstance().getIdForDisplay(
					(TestCaseEntity) ((TestCaseTreeEntity) value).getObject()));
		} catch (Exception e) {
			LoggerSingleton.logError(e);
		}
		return null;
	}

	private static Expression setValue(BooleanExpression booleanExpression, Object value, ClassNode scriptClass) {
		if (value instanceof BooleanExpression) {
			return (BooleanExpression) value;
		} else {
			Object newExpression = setValue(booleanExpression.getExpression(), value, scriptClass);
			if (newExpression instanceof Expression) {
				if (booleanExpression instanceof NotExpression) {
					return new NotExpression((Expression) newExpression);
				} else {
					return new BooleanExpression((Expression) newExpression);
				}
			}
		}
		return null;
	}

	private static Expression setValue(ConstantExpression constantExpression, Object value) {
		Expression newExpression = null;
		if (constantExpression.getValue() instanceof Integer) {
			if (value instanceof String) {
				try {
					newExpression = new ConstantExpression(Integer.parseInt((String) value));
				} catch (NumberFormatException e) {
					newExpression = new ConstantExpression(0);
				}
			}
		} else if (constantExpression.getValue() instanceof Boolean) {
			if (value instanceof String) {
				newExpression = new ConstantExpression(Boolean.parseBoolean((String) value));
			} else if (value instanceof Integer) {
				if (((int) value) == 0) {
					newExpression = new ConstantExpression(true);
				} else {
					newExpression = new ConstantExpression(false);
				}
			}
		} else {
			newExpression = new ConstantExpression(value);
		}
		return newExpression;
	}

	public static boolean setValue(Statement statement, Object value, ClassNode scriptClass) {
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			Expression newExpression = setValue(expressionStatement.getExpression(), value, scriptClass);
			if (!compareAstNode(newExpression, expressionStatement.getExpression())) {
				expressionStatement.setExpression(newExpression);
				return true;
			}
		} else if (statement instanceof AssertStatement && value instanceof BooleanExpression
				&& !compareAstNode(((AssertStatement) statement).getBooleanExpression(), value)) {
			((AssertStatement) statement).setBooleanExpression((BooleanExpression) value);
			return true;
		} else if (statement instanceof IfStatement && value instanceof BooleanExpression
				&& !compareAstNode(((IfStatement) statement).getBooleanExpression(), value)) {
			if (value instanceof NotExpression) {
				((IfStatement) statement).setBooleanExpression(new BooleanExpression((NotExpression) value));
			} else {
				((IfStatement) statement).setBooleanExpression((BooleanExpression) value);
			}
			return true;
		} else if (statement instanceof WhileStatement && value instanceof BooleanExpression
				&& !compareAstNode(((WhileStatement) statement).getBooleanExpression(), value)) {
			((WhileStatement) statement).setBooleanExpression((BooleanExpression) value);
			return true;
		}
		return false;
	}

	public static InputValueType getTypeValue(Object object, ClassNode scriptClass) {
		if (object instanceof Statement) {
			return getTypeValue((Statement) object, scriptClass);
		} else if (object instanceof Expression) {
			return getTypeValue((Expression) object, scriptClass);
		}
		return null;
	}

	public static InputValueType getTypeValue(Statement statement, ClassNode scriptClass) {
		if (statement instanceof ExpressionStatement) {
			return getTypeValue(((ExpressionStatement) statement).getExpression(), scriptClass);
		}
		return null;
	}

	public static InputValueType getTypeValue(Expression expression, ClassNode scriptClass) {
		return AstTreeTableInputUtil.getInputValueTypeForExpression(expression, scriptClass);
	}

	public static ASTNode setTypeValue(Object astNode, InputValueType type) {
		if (astNode instanceof Statement) {
			return setTypeValue((Statement) astNode, type);
		} else if (astNode instanceof Expression) {
			return setTypeValue((Expression) astNode, type);
		}
		return null;
	}

	private static ASTNode setTypeValue(Statement statement, InputValueType type) {
		if (statement instanceof ExpressionStatement) {
			return setTypeValue(((ExpressionStatement) statement).getExpression(), type);
		}
		return null;
	}

	private static ASTNode setTypeValue(Expression expression, InputValueType type) {
		return AstTreeTableInputUtil.generateNewExpression(expression, type);
	}

	public static boolean compareAstNode(Object astNode, Object anotherAstNode) {
		if ((astNode instanceof Expression && anotherAstNode instanceof Expression)
				|| (astNode instanceof Statement && anotherAstNode instanceof Statement)) {
			StringBuilder stringBuilder = new StringBuilder();
			new GroovyParser(stringBuilder).parse(astNode);
			String expressionValue = stringBuilder.toString();

			stringBuilder = new StringBuilder();
			new GroovyParser(stringBuilder).parse(anotherAstNode);
			String anotherExpressionValue = stringBuilder.toString();
			return expressionValue.equals(anotherExpressionValue);
		}
		return false;
	}
}