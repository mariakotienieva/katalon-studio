package com.kms.katalon.composer.execution.constants;

import com.kms.katalon.constants.GlobalStringConstants;

public class StringConstants extends GlobalStringConstants {

	// LogPropertyDialog
	public static final String DIA_LOG_LVL_START = "START";
	public static final String DIA_LOG_LVL_END = "END";
	public static final String DIA_LOG_LVL_INFO = "INFO";
	public static final String DIA_LOG_LVL_PASSED = "PASSED";
	public static final String DIA_LOG_LVL_FAILED = "FAILED";
	public static final String DIA_LOG_LVL_ERROR = "ERROR";
	public static final String DIA_LOG_LVL_WARNING = "WARNING";
	public static final String DIA_LBL_TIME = TIME;
	public static final String DIA_LBL_LEVEL = LEVEL;
	public static final String DIA_LBL_MESSAGE = MESSAGE;
	public static final String DIA_TITLE_PROPERTIES = "Properties";

	// ExecuteHandler
	public static final String ERROR_TITLE = ERROR;
	public static final String HAND_ERROR_MSG_NO_DEVICE = "No device is selected";
	public static final String HAND_ERROR_MSG_UNABLE_TO_EXECUTE_TEST_SCRIPT = "Unable to execute test script";
	public static final String HAND_ERROR_MSG_UNABLE_TO_EXECUTE_SELECTED_TEST_CASE = "Unable to execute the current selected test case.";
	public static final String HAND_ERROR_MSG_UNABLE_TO_EXECUTE_SELECTED_TEST_SUITE = "Unable to execute the current selected test suite.";
	public static final String HAND_WARN_MSG_NO_TEST_CASE_IN_TEST_SUITE = "The current selected test suite has no test case.";
	public static final String HAND_ERROR_MSG_REASON_WRONG_SYNTAX = "Wrong syntax";
	public static final String HAND_ERROR_MSG_REASON_INVALID_TEST_SUITE = "Test suite is not valid.";
	public static final String HAND_LAUNCHING_TEST_CASE = "Launching test case...";
	public static final String HAND_LAUNCHING_TEST_SUITE = "Launching test suite...";
	public static final String HAND_VALIDATING_TEST_SUITE = "Validating test suite...";
	public static final String HAND_ACTIVATING_VIEWERS = "Activating viewers...";
	public static final String HAND_BUILDING_SCRIPTS = "Building scripts...";

	// LogViewerPart
	public static final String PA_COLLAPSE_ALL = "Collapse all";
	public static final String PA_EXPAND_ALL = "Expand all";
	public static final String PA_PREV_FAILURE = "Show previous failure";
	public static final String PA_NEXT_FAILURE = "Show next failure";
	public static final String PA_LOADING_LOG = "Loading log";
	public static final String PA_LOADING_LOGS = PA_LOADING_LOG + "s";
	public static final String PA_LBL_START = "Start:";
	public static final String PA_LBL_END = "End:";
	public static final String PA_LBL_ELAPSED_TIME = "Elapsed time:";
	public static final String PA_LBL_MESSAGE = MESSAGE + ":";
	public static final String PA_LBL_RUNS = "Runs:";
	public static final String PA_LBL_PASSES = "Passes:";
	public static final String PA_LBL_FAILURES = "Failures:";
	public static final String PA_LBL_ERRORS = "Errors:";
	public static final String PA_TIP_ALL = ALL;
	public static final String PA_TIP_INFO = INFO;
	public static final String PA_TIP_PASSED = PASSED;
	public static final String PA_TIP_FAILED = FAILED;
	public static final String PA_TIP_ERROR = ERROR;
	public static final String PA_COL_LEVEL = LEVEL;
	public static final String PA_COL_TIME = TIME;
	public static final String PA_COL_MESSAGE = MESSAGE;
	public static final String PA_LOG_CONTEXT_MENU_PROPERTIES = DIA_TITLE_PROPERTIES;

	// LogExceptionNavigator
	public static final String WARN_TITLE = WARN;
	public static final String TRACE_WARN_MSG_NOT_FOUND = NOT_FOUND;
	public static final String TRACE_WARN_MSG_TEST_CASE_NOT_FOUND = TEST_CASE
			+ NOT_FOUND;
	public static final String TRACE_WARN_MSG_UNABLE_TO_OPEN_TEST_CASE = "Unable to open test case.";
	public static final String TRACE_WARN_MSG_UNABLE_TO_OPEN_KEYWORD_FILE = "Unable to open keyword's file";
	
	//Debug
	public static final String DBG_STRING_TYPE_NAME = "org.eclipse.jdt.debug.core.typeName";
	public static final String DBG_STRING_LINE_NUMBER = "lineNumber";
	public static final String DBG_COMMAND_SUSPEND = "org.eclipse.debug.ui.commands.Suspend";
	public static final String DBG_COMMAND_RESUME = "org.eclipse.debug.ui.commands.Resume";
}