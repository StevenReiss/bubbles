/********************************************************************************/
/*										*/
/*		NobaseDebugCommand.java 					*/
/*										*/
/*	Command to send to the debugger 					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.nobase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

abstract class NobaseDebugCommand implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int	sequence_number;
private String	wire_name;
private NobaseDebugResponse response_object;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected NobaseDebugCommand(NobaseDebugTarget tgt,String cmd)
{
   sequence_number = tgt.getNextSequence();
   response_object = null;
   wire_name = cmd;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getSequence()			{ return sequence_number; }

String getOutgoing()
{
   JSONObject obj = new JSONObject();
   obj.put("id",sequence_number);
   obj.put("method",getCommandName());
   JSONObject args = getCommandArguments();
   if (args == null) args = new JSONObject();
   args.put("method",getCommandName());
   if (args != null) {
      obj.put("params",args);
    }

   return obj.toString();
}


protected String getCommandName()
{
   String nm = wire_name;
   if (!nm.contains(".")) nm = "Debugger." + nm;
   return nm;
}


protected JSONObject getCommandArguments()
{
   return null;
}



void processResponse(JSONObject response)
{
   synchronized(this) {
      response_object = new NobaseDebugResponse(response);
      notifyAll();
    }
}

NobaseDebugResponse getResponse()
{
   synchronized (this) {
      while (response_object == null) {
	 try {
	    wait(1000);
	  }
	 catch (InterruptedException e) { }
       }
    }
   return response_object;
}



/********************************************************************************/
/*										*/
/*	Helper methods								 */
/*										*/
/********************************************************************************/

private static JSONObject createLocation(String file,int line)
{
   JSONObject rslt = new JSONObject();
   rslt.put("scriptId",createScriptId(file));
   rslt.put("lineNumber",line);
   return rslt;
}


private static String createScriptId(String file)
{
   return null;
}


private static JSONObject createValue(String val)
{
   JSONObject vobj = new JSONObject();
   vobj.put("value",val);
   return vobj;
}



/********************************************************************************/
/*										*/
/*	V8 Chrome Debug Commands						*/
/*										*/
/********************************************************************************/


//
//	CONTINUE TO LOCATION
//		Continues execution until specific lcoaiton is reached
//

static class ContinueToLocation extends NobaseDebugCommand {

   private String file_name;
   private int line_number;
   private boolean any_frame;

   ContinueToLocation(NobaseDebugTarget tgt,String file,int line,boolean anyframe) {
      super(tgt,"continueToLocation");
      file_name = null; 
      line_number = line;
      any_frame = anyframe;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject rslt = new JSONObject();
      rslt.put("location",createLocation(file_name,line_number));
      rslt.put("targetCallFrames",(any_frame ? "any" : "current"));
      return rslt;
    }

}	// end of inner class ContinueToLocation



//
//	DISABLE
//		Disables debugger for given page
//

static class Disable extends NobaseDebugCommand {

   Disable(NobaseDebugTarget tgt) {
      super(tgt,"disable");
    }

}	// end of inner class Disable



//
//	ENABLE
//		Enables debugger for the given page. Clients should not assume that
//		the debugging has been enabled until the result for this command
//		is received
//

static class Enable extends NobaseDebugCommand {

   Enable(NobaseDebugTarget tgt) {
      super(tgt,"enable");
    }

}	// end of inner class Enable



//
//	EVALUATE ON CALL FRAME
//		Evaluates expression on a given call frame
//

static class EvaluateOnCallFrame extends NobaseDebugCommand {
   private int frame_id;
   private String eval_expression;
   private boolean is_silent;
   private long eval_timeout;

   EvaluateOnCallFrame(NobaseDebugTarget tgt,int frameid,String expr,boolean silent,
	 long timeout) {
      super(tgt,"evaluateOnCallFrame");
      frame_id = frameid;
      eval_expression = expr;
      is_silent = silent;
      eval_timeout = timeout;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject rslt = new JSONObject();
      rslt.put("callFrameId",frame_id);
      rslt.put("expression",eval_expression);
      rslt.put("silent",is_silent);
      if (eval_timeout > 0) {
	 rslt.put("timeout",eval_timeout);
       }
      return rslt;
    }

}	// end of inner class EvaluateOnCallFrame



//
// GET POSSIBLE BREAKPOINTS
//		Returns possible locaions for breakpoint. sciprtId in start and
//		end range locations should be the same
//

static class GetPossibleBreakpoints extends NobaseDebugCommand {

   private String for_file;
   private int for_line;

   GetPossibleBreakpoints(NobaseDebugTarget tgt,String file,int line) {
      super(tgt,"getPossibleBreakpoints");
      for_file = file;
      for_line = line;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("start",createLocation(for_file,for_line));
      obj.put("restrictToFunction",true);
      return obj;
    }

}	// end of inner class GetPossibleBreakpoints



//
//	GET SCRIPT SOURCE
//		Returns source for the script given id
//

static class GetScriptSource extends NobaseDebugCommand {

   private String for_file;

   GetScriptSource(NobaseDebugTarget tgt,String file) {
      super(tgt,"getScriptSource");
      for_file = file;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("scriptId",createScriptId(for_file));
      return obj;
    }

}	// end of inner class GetScriptSources



//
//	GET STACK TRACE
//		Returns stack trace with given stackFrameId
//

static class GetStackTrace extends NobaseDebugCommand {

   private String trace_id;

   GetStackTrace(NobaseDebugTarget tgt,String id) {
      super(tgt,"getStackTrace");
      trace_id = id;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject rslt = new JSONObject();
      rslt.put("stackTraceId",trace_id);
      return rslt;
    }

}	// end of inner class GetStackTrace



//
//	PAUSE
//		Stops on the next JavaScript statement
//

static class Pause extends NobaseDebugCommand {

   Pause(NobaseDebugTarget tgt) {
      super(tgt,"pause");
    }

}	// end of inner class Pause



//
//	REMOVE BREAKPOINT
//		Removes JavaScript breakpoint
//

static class RemoveBreakpoint extends NobaseDebugCommand {

   private int breakpoint_id;

   RemoveBreakpoint(NobaseDebugTarget tgt,int bid) {
      super(tgt,"removeBreakpoint");
      breakpoint_id = bid;
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("breakpointId",breakpoint_id);
      return obj;
    }

}	// end of inner class RemoveBreakpoint



//
//	RESTART FRAME
//		Restarts particular call frame from the beginning
//

static class RestartFrame extends NobaseDebugCommand {

   private String frame_id;

   RestartFrame(NobaseDebugTarget tgt,String fid) {
      super(tgt,"restartFrame");
      frame_id = fid;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("callFrameId",frame_id);
      return obj;
    }

}	// end of inner class RestartFrame



//
//	RESUME
//		Resumes JavaScript execution
//

static class Resume extends NobaseDebugCommand {

   Resume(NobaseDebugTarget tgt) {
      super(tgt,"resume");
    }

}	// end of inner class Resume



//
//	SEARCH IN CONTENT
//		Searches for given string in script content
//

static class SearchInContent extends NobaseDebugCommand {

   private String for_file;
   private String search_query;
   private boolean use_case;
   private boolean is_regex;

   SearchInContent(NobaseDebugTarget tgt,String file,String query,boolean usecase,boolean regex) {
      super(tgt,"searchInContent");
      for_file = file;
      search_query = query;
      use_case = usecase;
      is_regex = regex;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("scriptId",createScriptId(for_file));
      obj.put("query",search_query);
      obj.put("caseSensitive",use_case);
      obj.put("isRegex",is_regex);
      return obj;
    }

}	// end of inner class SearchInContent



//
//	SET ASYNC CALL STACK DEPTH
//		Enables or disables async call stacks tracking
//

static class SetAsyncCallStackDepth extends NobaseDebugCommand {

   private int max_depth;

   SetAsyncCallStackDepth(NobaseDebugTarget tgt,int depth) {
      super(tgt,"setAsyncCallStackDepth");
      max_depth = depth;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("maxDepth",max_depth);
      return obj;
    }

}	// end of inner class SetAsyncCallStackDepth



//
//	SET BREAKPOINTS ACTIVE
//		Activates / deactivates all breakpoints on the page
//

static class SetBreakpointsActive extends NobaseDebugCommand {

   private boolean is_active;

   SetBreakpointsActive(NobaseDebugTarget tgt,boolean active) {
      super(tgt,"setBreakpointsActive");
      is_active = active;
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("active",is_active);
      return obj;
    }

}	// end of inner class SetBreakpointsActive




//
//	SET LINE BREAKPOINT
//		Sets JavaScript breakpoint at a given location
//

static class SetLineBreakpoint extends NobaseDebugCommand {

   private String file_name;
   private int line_number;

   SetLineBreakpoint(NobaseDebugTarget tgt,String file,int line) {
      super(tgt,"setBreakpoint");
      file_name = file;
      line_number = line;
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("location",createLocation(file_name,line_number));
      return obj;
    }

}	// end of inner class SetLineBreakpoint



//
//	SET URL BREAKPOINT
//		Sets JavaScript breakpoint at given location specified by URL or
//		URL REGEX.  Once this command is issued, all existing parsed scripts
//		will have breakpoints resolved and returned in `locations` property.
//		Further matching script parsing will result in subsequent
//		`breakpointResolved` events issued.  This logical breakpoint will
//		survive page reloads.
//

static class SetUrlBreakpoint extends NobaseDebugCommand {

   private String url_name;
   private int line_number;

   SetUrlBreakpoint(NobaseDebugTarget tgt,String url,int line) {
      super(tgt,"setBreakpointByUrl");
      url_name = url;
      line_number = line;
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("url",url_name);
      obj.put("lineNumber",line_number);
      return obj;
    }

}	// end of inner class SetUrlBreakpoint



//
//	SET PAUSE ON EXCEPTIONS
//		Defines pause on exceptions state.  Can be set to stop on all
//		exceptions, uncaught exceptions, or no exceptions.  Initial puase
//		on exceptions state is `none`.
//

static class SetPauseOnExceptions extends NobaseDebugCommand {

   private String exception_state;

   SetPauseOnExceptions(NobaseDebugTarget tgt,boolean caught,boolean uncaught) {
      super(tgt,"setPauseOnExceptions");
      exception_state = "none";
      if (uncaught) exception_state = "uncaught";
      else if (caught) exception_state = "all";
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("state",exception_state);
      return obj;
    }

}	// end of inner class SetPauseOnExceptions



//
//	SET SCRIPT SOURCE
//		Edits JavaScript source live
//

static class SetScriptSource extends NobaseDebugCommand {

   private String file_name;
   private String new_contents;

   SetScriptSource(NobaseDebugTarget tgt,String file,String cnts) {
      super(tgt,"setScriptSource");
      file_name = file;
      new_contents = cnts;
    }

   @Override public JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("scriptId",createScriptId(file_name));
      obj.put("scriptSource",new_contents);
      return obj;
    }

}	// end of inner class SetScriptSource



//
//	SET SKIP ALL PAUSES
//		Makes page not interrupt on any pauses (breakpoint, exception, dom
//		exception, etc.
//

static class SetSkipAllPauses extends NobaseDebugCommand {

   SetSkipAllPauses(NobaseDebugTarget tgt) {
      super(tgt,"setSkipAllPauses");
    }

}	// end of inner class SetSkipAllPauses



//
//	SET VARIABLE VALUE
//		Changes value of a variable in a callframe.  Object-based scopes are
//		not supported and must be mutated manually
//

static class SetVariableValue extends NobaseDebugCommand {

   private int scope_number;
   private String variable_name;
   private String variable_value;
   private String call_frame;

   SetVariableValue(NobaseDebugTarget tgt,String var,String val,String frame,int scope) {
      super(tgt,"setVariableValue");
      scope_number = scope;
      variable_name = var;
      variable_value = val;
      call_frame = frame;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("scopeNumber",scope_number);
      obj.put("variableName",variable_name);
      obj.put("newValue",createValue(variable_value));
      obj.put("callFrameId",call_frame);
      return obj;
    }

}	// end of inner class SetVariableValue




//
//	STEP INTO
//		Steps into the function call
//

static class StepInto extends NobaseDebugCommand {

   StepInto(NobaseDebugTarget tgt) {
      super(tgt,"stepInto");
    }

}	// end of inner class StepInto



//
//	STEP OUT
//		Steps out of the function call
//

static class StepOut extends NobaseDebugCommand {

   StepOut(NobaseDebugTarget tgt) {
      super(tgt,"stepOut");
    }

}	// end of inner class StepOut



//
//	STEP OVER
//		Steps over the statement
//

static class StepOver extends NobaseDebugCommand {

   StepOver(NobaseDebugTarget tgt) {
      super(tgt,"stepOver");
    }

}	// end of inner class StepOver




/********************************************************************************/
/*										*/
/*	Profiler commands							*/
/*										*/
/********************************************************************************/

//
//	DISABLE
//

static class ProfilerDisable extends NobaseDebugCommand {

   ProfilerDisable(NobaseDebugTarget tgt) {
      super(tgt,"Profiler.disable");
    }

}	// end of inner class ProfilerDisable



//
//	ENABLE
//

static class ProfilerEnable extends NobaseDebugCommand {

   ProfilerEnable(NobaseDebugTarget tgt) {
      super(tgt,"Profiler.enable");
    }

}	// end of inner class ProfilerEnable



/********************************************************************************/
/*										*/
/*	Runtime commands							*/
/*										*/
/********************************************************************************/

//
//	RuntimeCallFunctionOn
//		Calls functions with given declaration ont he given object.  Object
//		group of the result is inherite from the target object
//

static class RuntimeCallFunctionOn extends NobaseDebugCommand {

   private String function_name;
   private String object_id;
   private List<String> arg_values;
   private boolean is_silent;

   RuntimeCallFunctionOn(NobaseDebugTarget tgt,String fct,String objid,
	 List<String> args,boolean silent) {
      super(tgt,"Runtime.callFunctionOn");
      function_name = fct;
      object_id = objid;
      arg_values = new ArrayList<>();
      if (args != null) arg_values.addAll(args);
      is_silent = silent;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("functionDeclaration",function_name);
      if (object_id != null) obj.put("objectId",object_id);
      JSONArray args = new JSONArray();
      for (int i = 0; i < arg_values.size(); ++i) {
	 args.put(i,createValue(arg_values.get(i)));
       }
      obj.put("arguments",args);
      obj.put("silent",is_silent);
      return obj;
    }

}	// end of inner class RuntimeCallFunctionOn



//
//	COMPILE SCRIPT
//		Compiles expression
//

static class RuntimeCompileScript extends NobaseDebugCommand {

   String expression_text;
   String source_url;
   boolean is_persistent;

   RuntimeCompileScript(NobaseDebugTarget tgt,String expr,String source,boolean keep) {
      super(tgt,"Runtime.compileScript");
      expression_text = expr;
      source_url = source;
      is_persistent = keep;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("expression",expression_text);
      obj.put("sourceURL",source_url);
      obj.put("persistScript",is_persistent);
      return obj;
    }

}	// end of inner class RuntimeCompileScript



//
//	RUNTIME DISABLE
//		Disables reporting of execution contexts creation
//

static class RuntimeDisable extends NobaseDebugCommand {

   RuntimeDisable(NobaseDebugTarget tgt) {
      super(tgt,"Runtime.disable");
    }

}	// end of inner class RuntimeDisable



//
//	RUNTIME DISCARD CONSOLE ENTRIES
//		Discards collected exceptions and console API calls
//

static class RuntimeDiscardConsoleEntries extends NobaseDebugCommand {

   RuntimeDiscardConsoleEntries(NobaseDebugTarget tgt) {
      super(tgt,"Runtime.discardConsoleEntries");
    }

}	// end of inner class RuntimeDiscardConsolEntries



//
//	RUNTIME ENABLE
//		Enables eporting of execution contexts creation by means of
//		`executionContexCreated` events
//

static class RuntimeEnable extends NobaseDebugCommand {

   RuntimeEnable(NobaseDebugTarget tgt) {
      super(tgt,"Runtime.enable");
    }

}	// end of inner class RuntimeEnable



//
//	RUNTIME EVALUATE
//		Evaluates expression on global object
//

static class RuntimeEvaluate extends NobaseDebugCommand {

   private String expression_text;
   private boolean is_silent;
   private long time_out;

   RuntimeEvaluate(NobaseDebugTarget tgt,String expr,boolean silent,long timeout) {
      super(tgt,"Runtime.evaluate");
      expression_text = expr;
      is_silent = silent;
      time_out = timeout;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("expression",expression_text);
      obj.put("silent",is_silent);
      if (time_out > 0) obj.put("timeout",time_out);
      return obj;
    }

}	// end of inner class RuntimeEvaluate



//
//	RUNTIME GET PROPERTIES
//		Returns properties of a given object.  Object group of the result is
//		inherited from the target
//

static class RuntimeGetProperties extends NobaseDebugCommand {

   private String object_id;

   RuntimeGetProperties(NobaseDebugTarget tgt,String obj) {
      super(tgt,"Runtime.getProperties");
      object_id = obj;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("objectId",object_id);
      obj.put("accessorPropertiesOnly",false);
      obj.put("generatePreview",true);
      obj.put("ownProperties",false);
      return obj;
    }

}	// end of inner class RuntimeGetProperties



//
//	RUNTIME GLOBAL LEXCIAL SCOPE NAMES
//		Returns all let, const and class varialbles from global scope
//

static class RuntimeGlobalLexicalScopeNames extends NobaseDebugCommand {

   RuntimeGlobalLexicalScopeNames(NobaseDebugTarget tgt) {
      super(tgt,"Runtime.globalLexicalScopeNames");
    }

}	// end of inner class RuntimeGlobalLexicalScopeNames




//
//	RUNTIME QUERY OBJECTS
//

static class RuntimeQueryObjects extends NobaseDebugCommand {

   private String proto_id;

   RuntimeQueryObjects(NobaseDebugTarget tgt,String proto) {
      super(tgt,"Runtime.queryObjects");
      proto_id = proto;
    }

   @Override protected JSONObject getCommandArguments() {
      JSONObject obj = new JSONObject();
      obj.put("prototypeObjectId",proto_id);
      return obj;
    }

}	// end of inner class RuntimeQueryObjects



//
//	RUNTIME RUN IF WAITING FOR DEBUGGER
//

static class RuntimeRunIfWaitingForDebugger extends NobaseDebugCommand {

   RuntimeRunIfWaitingForDebugger(NobaseDebugTarget tgt) {
      super(tgt,"Runtime.runIfWaitingForDebugger");
    }

}	// end of inner class RuntimeRunIfWaitingForDebugger




}	// end of class NobaseDebugCommand




/* end of NobaseDebugCommand.java */

