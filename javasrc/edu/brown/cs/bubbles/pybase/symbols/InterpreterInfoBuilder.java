
package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import org.python.pydev.core.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author fabioz
 */
public class InterpreterInfoBuilder {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseInterpreter	interpreter_info;
private boolean 		is_disposed;

private static final InterpreterBuilderJob builder_job = new InterpreterBuilderJob();



boolean isDisposed()
{
   return this.is_disposed;
}


public void dispose()
{
   is_disposed = true;
}



public void setInfo(PybaseInterpreter info)
{
   setInfo(info, 20 * 1000); // Default: check 20 seconds after starting up...
}



public void setInfo(PybaseInterpreter info,int schedule)
{
   this.interpreter_info = info;
   builder_job.buildersToCheck.add(this);
   PybaseMain.getPybaseMain().startTask(builder_job);
   // builder_job.schedule(schedule);
}




static class InterpreterBuilderJob implements Runnable {

   private volatile Set<InterpreterInfoBuilder> buildersToCheck = new HashSet<InterpreterInfoBuilder>();

   public InterpreterBuilderJob() { }

   @Override public void run() {
      Set<InterpreterInfoBuilder> builders = buildersToCheck;
      buildersToCheck = new HashSet<InterpreterInfoBuilder>();

      for (InterpreterInfoBuilder builder : builders) {
	 boolean ret = checkEarlyReturn(builder);
	 if (ret) continue;

	 PythonPathHelper pythonPathHelper = new PythonPathHelper();
	 pythonPathHelper.setPythonPath(builder.interpreter_info.lib_set);
	 ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure();
	 ret = checkEarlyReturn(builder);
	 if (ret) continue;

	 SystemModulesManager modulesManager = builder.interpreter_info.getModulesManager();
	 ModulesKeyTreeMap<ModulesKey, ModulesKey> keysFound = modulesManager.buildKeysFromModulesFound(modulesFound);

	 ret = checkEarlyReturn(builder);
	 if (ret) continue;

	 Tuple<List<ModulesKey>, List<ModulesKey>> diffModules = modulesManager.diffModules(keysFound);
	 if (diffModules.o1.size() > 0 || diffModules.o2.size() > 0) {
	    // Update the modules manager itself (just pass all the keys as that should be
	    // fast)
	    modulesManager.updateKeysAndSave(keysFound);
	  }
       }
    }

   private boolean checkEarlyReturn(InterpreterInfoBuilder builder) {
      if (builder.isDisposed()) {
	 return true;
       }

      if (!builder.interpreter_info.getLoadFinished()) {
	 buildersToCheck.add(builder);
	 PybaseMain.getPybaseMain().startTaskDelayed(this,20*1000);
	 // builder_job.schedule(20 * 1000); // Check again in 20 seconds
	 return true;
       }
      return false;
    }

}	// end of inner class InterpreterBuilderJob



}
