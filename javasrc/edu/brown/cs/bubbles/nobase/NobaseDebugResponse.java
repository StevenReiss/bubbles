/********************************************************************************/
/*										*/
/*		NobaseDebugResponse.java					*/
/*										*/
/*	Response from a command 						*/
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

import org.json.JSONObject;


class NobaseDebugResponse implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JSONObject			result_object;
private JSONObject                      error_object;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugResponse(JSONObject resp)
{
   if (resp == null) {
      result_object = null;
      error_object = null;
    }
   else {
      result_object = resp.optJSONObject("result");
      error_object = resp.optJSONObject("error");
    }
   if (!isSuccess() && error_object != null) {
      NobaseMain.logE("Bad response: " + getError() + " : " + resp);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isSuccess()
{
   return result_object != null;
}


String getError()
{
   if (error_object == null) return null;
   return error_object.optString("message",null);
}


JSONObject getResult()
{
   return result_object;
}



}	// end of class NobaseDebugResponse




/* end of NobaseDebugResponse.java */

