package com.kms.katalon.core.ast;

import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.DoWhileStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import com.kms.katalon.core.annotation.RequireAstTestStepTransformation
import com.kms.katalon.core.constants.StringConstants
import com.kms.katalon.core.keyword.BuiltinKeywords
import com.kms.katalon.core.logging.KeywordLogger

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class AstTestStepTransformation implements ASTTransformation {

	private static final String KEYWORD_LOGGER_GET_INSTANCE_METHOD_NAME = "getInstance";

	private static final String KEYWORD_LOGGER_SET_PENDING_DESCRIPTION_METHOD_NAME = "setPendingDescription";

	private static final String KEYWORD_MAIN_RUN_KEYWORD_METHOD_NAME = "runKeyword";

	private static final List<ImportNode> importNodes = new ArrayList<ImportNode>()

	private static final String KEYWORD_DEFAULT_NAME = "Statement"

	private static final String RUN_METHOD_NAME = "run";

	@CompileStatic
	public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
		if (!(astNodes != null)
		|| !(astNodes[0] != null)
		|| !(astNodes[1] != null)
		|| (!(astNodes[0] instanceof AnnotationNode))
		|| !((AnnotationNode) astNodes[0]).getClassNode().getName()
		.equals(RequireAstTestStepTransformation.class.getName())
		|| (!(astNodes[1] instanceof ClassNode))) {
			return;
		}
		ClassNode annotatedClass = (ClassNode) astNodes[1];

		importNodes.clear();
		for (ImportNode importNode : annotatedClass.getModule().getImports()) {
			importNodes.add(importNode);
		}
		for (MethodNode method : annotatedClass.getMethods()) {
			if (method.getName().equalsIgnoreCase(RUN_METHOD_NAME) && method.getCode() instanceof BlockStatement) {
				visit((BlockStatement) method.getCode(), 1);
			}
		}
	}

	@CompileStatic
	private Class<?> loadClass(String className) {
		Class<?> type = null;
		try {
			type = Class.forName(className);
			return type;
		} catch (ClassNotFoundException ex) {
			// not found, do nothing
		}
		for (ImportNode importNode : importNodes) {
			if (importNode.getClassName().endsWith(className)) {
				try {
					type = Class.forName(importNode.getClassName());
					return type;
				} catch (ClassNotFoundException ex) {
					continue;
				}
			}
		}
		return null;
	}

	@CompileStatic
	private String getComment(Statement statement) {
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			if (expressionStatement.getExpression() instanceof ConstantExpression) {
				ConstantExpression constant = (ConstantExpression) expressionStatement.getExpression();
				if (constant.getValue() instanceof String) {
					return constant.getValue();
				}
			}
		}
		return null;
	}

	// exclude loop statements
	@CompileStatic
	private boolean isParentStatement(Statement statement) {
		return (statement instanceof BlockStatement || statement instanceof IfStatement
				|| statement instanceof CatchStatement || statement instanceof SwitchStatement
				|| statement instanceof CaseStatement || statement instanceof TryCatchStatement);
	}

	@CompileStatic
	public void visit(Statement statement, int nestedLevel) {
		if (statement instanceof BlockStatement) {
			visit((BlockStatement) statement, nestedLevel);
		} else if (statement instanceof ForStatement) {
			visit((ForStatement) statement, nestedLevel);
		} else if (statement instanceof WhileStatement) {
			visit((WhileStatement) statement, nestedLevel);
		} else if (statement instanceof IfStatement) {
			visit((IfStatement) statement, nestedLevel);
		} else if (statement instanceof TryCatchStatement) {
			visit((TryCatchStatement) statement, nestedLevel);
		} else if (statement instanceof CatchStatement) {
			visit((CatchStatement) statement, nestedLevel);
		} else if (statement instanceof SwitchStatement) {
			visit((SwitchStatement) statement, nestedLevel);
		} else if (statement instanceof CaseStatement) {
			visit((CaseStatement) statement, nestedLevel);
		}
	}

	@CompileStatic
	public void visit(ForStatement forStatement, int nestedLevel) {
		visit(forStatement.getLoopBlock(), nestedLevel);
	}

	@CompileStatic
	public void visit(IfStatement ifStatement, int nestedLevel) {
		visit(ifStatement.getIfBlock(), nestedLevel);
		visit(ifStatement.getElseBlock(), nestedLevel);
	}

	@CompileStatic
	public void visit(WhileStatement whileStatement, int nestedLevel) {
		visit(whileStatement.getLoopBlock(), nestedLevel);
	}

	@CompileStatic
	public void visit(DoWhileStatement doWhileStatement, int nestedLevel) {
		visit(doWhileStatement.getLoopBlock(), nestedLevel);
	}

	@CompileStatic
	public void visit(TryCatchStatement tryCatchStatement, int nestedLevel) {
		visit(tryCatchStatement.getTryStatement(), nestedLevel);
		for (CatchStatement catchStatement : tryCatchStatement.getCatchStatements()) {
			visit(catchStatement, nestedLevel);
		}
		visit(tryCatchStatement.getFinallyStatement(), nestedLevel);
	}

	@CompileStatic
	public void visit(CatchStatement catchStatement, int nestedLevel) {
		visit(catchStatement.getCode(), nestedLevel);
	}

	@CompileStatic
	public void visit(CaseStatement caseStatement, int nestedLevel) {
		visit(caseStatement.getCode(), nestedLevel);
	}

	@CompileStatic
	public void visit(SwitchStatement switchStatement, int nestedLevel) {
		for (CaseStatement caseStatement : switchStatement.getCaseStatements()) {
			visit(caseStatement, nestedLevel);
		}
		visit(switchStatement.getDefaultStatement(), nestedLevel);
	}

	@CompileStatic
	public void visit(BlockStatement blockStatement, int nestedLevel) {
		int index = 0;
		while (index < blockStatement.getStatements().size()) {
			Statement statement = blockStatement.getStatements().get(index);
			String comment = getComment(statement);
			if (comment != null && !comment.isEmpty()) {
				MethodCallExpression methodCall = createNewAddDescriptionMethodCall(comment);
				blockStatement.getStatements().set(blockStatement.getStatements().indexOf(statement),
						new ExpressionStatement(methodCall));
				index++;
				continue;
			}
			if (!(statement instanceof BlockStatement)) {
				String keywordName = getKeywordNameForStatement(statement);
				blockStatement.getStatements().add(index, new ExpressionStatement(createNewStartKeywordMethodCall(keywordName, statement, nestedLevel)));
			}
			visit(statement, nestedLevel + 1);
			index += 2;
		}
	}

	@CompileStatic
	private String getKeywordNameForStatement(Statement statement) {
		String keywordName = null;
		String builtinKeywordName = getBuiltinKeywordMethodCallStatement(statement);
		if (builtinKeywordName != null) {
			keywordName = builtinKeywordName;
		} else {
			String customKeywordName = getCustomKeywordMethodCallStatement(statement);
			if (customKeywordName != null) {
				keywordName = customKeywordName;
			} else {
				keywordName = KEYWORD_DEFAULT_NAME + " - " + AstTextValueUtil.getTextValue(statement);
			}
		}
		return keywordName;
	}

	@CompileStatic
	private String getBuiltinKeywordMethodCallStatement(Statement statement) {
		if (!(statement instanceof ExpressionStatement)) {
			return null;
		}
		ExpressionStatement expressionStatement = (ExpressionStatement) statement;
		if (!(expressionStatement.getExpression() instanceof MethodCallExpression)) {
			return null;
		}
		MethodCallExpression methodCall = (MethodCallExpression) expressionStatement.getExpression();
		Class<?> methodClass = loadClass(methodCall.getObjectExpression().getText());
		if (methodClass != null && BuiltinKeywords.class.isAssignableFrom(methodClass)) {
			return methodCall.getMethod().getText();
		}
		return null;
	}

	@CompileStatic
	private String getCustomKeywordMethodCallStatement(Statement statement) {
		if (!(statement instanceof ExpressionStatement)) {
			return null;
		}
		ExpressionStatement expressionStatement = (ExpressionStatement) statement;
		if (!(expressionStatement.getExpression() instanceof MethodCallExpression)) {
			return null;
		}
		MethodCallExpression methodCall = (MethodCallExpression) expressionStatement.getExpression();
		if (methodCall.getObjectExpression().getText().equals(StringConstants.CUSTOM_KEYWORD_CLASS_NAME)) {
			return methodCall.getMethod().getText();
		}
		return null;
	}

	@CompileStatic
	private MethodCallExpression createNewAddDescriptionMethodCall(String comment) {
		List<Expression> expressionArguments = new ArrayList<Expression>();
		MethodCallExpression loggerGetInstanceMethodCall = new MethodCallExpression(
				new ClassExpression(new ClassNode(KeywordLogger.class)), KEYWORD_LOGGER_GET_INSTANCE_METHOD_NAME,
				new ArgumentListExpression(expressionArguments));
		expressionArguments = new ArrayList<Expression>();
		expressionArguments.add(new ConstantExpression(comment));
		MethodCallExpression methodCall = new MethodCallExpression(loggerGetInstanceMethodCall,
				KEYWORD_LOGGER_SET_PENDING_DESCRIPTION_METHOD_NAME, new ArgumentListExpression(expressionArguments))
		return methodCall
	}

	@CompileStatic
	private MethodCallExpression createNewStartKeywordMethodCall(String keywordName, Statement statement, int nestedLevel) {
		List<Expression> expressionArguments = new ArrayList<Expression>();
		MethodCallExpression loggerGetInstanceMethodCall = new MethodCallExpression(
				new ClassExpression(new ClassNode(KeywordLogger.class)), KEYWORD_LOGGER_GET_INSTANCE_METHOD_NAME,
				new ArgumentListExpression(expressionArguments));
		expressionArguments = new ArrayList<Expression>();
		expressionArguments.add(new ConstantExpression(keywordName));
		expressionArguments.add(createPropertiesMapExpressionForKeyword(statement));
		expressionArguments.add(new ConstantExpression(nestedLevel));
		MethodCallExpression methodCall = new MethodCallExpression(loggerGetInstanceMethodCall,
				StringConstants.LOG_START_KEYWORD_METHOD, new ArgumentListExpression(expressionArguments))
		return methodCall
	}

	@CompileStatic
	private MapExpression createPropertiesMapExpressionForKeyword(Statement statement) {
		List<MapEntryExpression> mapEntryList = new ArrayList<MapEntryExpression>();
		mapEntryList.add(new MapEntryExpression(new ConstantExpression(StringConstants.XML_LOG_START_LINE_PROPERTY), new ConstantExpression(statement.getLineNumber().toString())));
		MapExpression map = new MapExpression(mapEntryList)
		return map
	}
}