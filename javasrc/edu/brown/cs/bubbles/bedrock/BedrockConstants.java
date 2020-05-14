/********************************************************************************/
/*										*/
/*		BedrockConstants.java						*/
/*										*/
/*	Constants for Eclipse-Bubbles interface package 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;





public interface BedrockConstants {


/********************************************************************************/
/*										*/
/*	Standard names								*/
/*										*/
/********************************************************************************/

String BEDROCK_PLUGIN = "edu.brown.cs.bubbles.bedrock";
String BEDROCK_UPDATER = "edu.brown.cs.bubbles.bedrock.BedrockBuilder";



/********************************************************************************/
/*										*/
/*	Other names								*/
/*										*/
/********************************************************************************/

String BEDROCK_DUMMY_HANDLE = "*DUMMY*";
String BEDROCK_UNMANAGED_HANDLE = "*UNMANAGED*";



/********************************************************************************/
/*										*/
/*	Limits									*/
/*										*/
/********************************************************************************/

int	MAX_TEXT_SEARCH_RESULTS = 128;




/********************************************************************************/
/*										*/
/*	Mint constants								*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	BEDROCK_MESSAGE_ID = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");
// String BEDROCK_MESSAGE_ID = "BUBBLES";




/********************************************************************************/
/*										*/
/*	Debugger constants							*/
/*										*/
/********************************************************************************/

enum BedrockDebugAction {
   NONE,
   TERMINATE,
   RESUME,
   STEP_INTO,
   STEP_OVER,
   STEP_RETURN,
   SUSPEND,
   DROP_TO_FRAME
}

String	BEDROCK_LAUNCH_ID_PROP = "edu.brown.cs.bubbles.bedrock.ID";
String	BEDROCK_LAUNCH_ORIGID_PROP = "edu.brown.cs.bubbles.bedrock.ORIGID";
String	BEDROCK_LAUNCH_IGNORE_PROP = "edu.brown.cs.bubbles.bedrock.IGNORE";




/********************************************************************************/
/*										*/
/*	Edit information							*/
/*										*/
/********************************************************************************/

interface EditData {

   public int getOffset();
   public int getLength();
   public String getText();

}	// end of subinterface EditData



/********************************************************************************/
/*										*/
/*	Logging constants							*/
/*										*/
/********************************************************************************/

enum BedrockLogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}



}	// end of interface BedrockConstants




/* end of BedrockConstants.java */








































