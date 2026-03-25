/********************************************************************************/
/*										*/
/*		BoardMail.java							*/
/*										*/
/*	Bubbles attribute and property management mail handler			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.board;


import edu.brown.cs.ivy.bower.BowerMailer;

/**
 *	This class is used to create and send email messages.
 **/

public final class BoardMail implements BoardConstants {




/********************************************************************************/
/*										*/
/*	Static interface							*/
/*										*/
/********************************************************************************/

/**
 *	Create a new email message being sent to the given address.
 **/

public static BowerMailer createMessage(String to)
{
   BowerMailer bm = new BowerMailer(to,"Code Bubbles",null);
   BoardProperties bp = BoardProperties.getProperties("Board");
   String mailsender = bp.getString("Board.mail.from","Code Bubbles");
   String mailuser = bp.getString("Board.mail.user");
   String mailpwd = bp.getString("Board.mail.password");
   String smtp = bp.getString("Board.mail.host");      
   if (smtp != null) bm.setSmtpHost(smtp);
   bm.setSender(mailsender,mailuser,mailpwd);
   
   return bm;
}



}	// end of class BoardMail



/* end of BoardMail.java */
