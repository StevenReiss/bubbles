/********************************************************************************/
/*										*/
/*		BgtaSASLDigestMD5Mechanism.java 				*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Sumner Warren		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


package edu.brown.cs.bubbles.bgta;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/*
 * Implementation of the SASL DIGEST-MD5 mechanism for connecting to chat.facebook.com
 *
 */
public class BgtaSASLDigestMD5Mechanism extends SASLMechanism {



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BgtaSASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
{
   super(saslAuthentication);
}



/********************************************************************************/
/*										*/
/*	SASL Mechanism implementation						*/
/*										*/
/********************************************************************************/

@Override protected void authenticate() throws IOException, XMPPException
{
   String[] mechanisms = { getName() };
   Map<String, String> props = new HashMap<String, String>();
   sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props, this);

   super.authenticate();
}



@Override public void authenticate(String username,String host,String pwd) throws IOException,
	 XMPPException
{
   this.authenticationId = username;
   this.password = pwd;
   this.hostname = host;

   String[] mechanisms = { getName() };
   Map<String, String> props = new HashMap<String, String>();
   sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
   super.authenticate();
}



@Override public void authenticate(String username,String host,CallbackHandler cbh)
	throws IOException, XMPPException
{
   String[] mechanisms = { getName() };
   Map<String, String> props = new HashMap<String, String>();
   sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
   super.authenticate();
}



@Override protected String getName()
{
   return "DIGEST-MD5";
}



@Override public void challengeReceived(String challenge) throws IOException
{
   // Build the challenge response stanza encoding the response text
   StringBuilder stanza = new StringBuilder();

   byte response[];
   if (challenge != null) {
      response = sc.evaluateChallenge(Base64.decode(challenge));
    }
   else {
      response = sc.evaluateChallenge(null);
    }

   String authenticationText = "";

   if (response != null) {
      authenticationText = Base64.encodeBytes(response, Base64.DONT_BREAK_LINES);
      if (authenticationText.equals("")) {
	 authenticationText = "=";
       }
    }

   stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
   stanza.append(authenticationText);
   stanza.append("</response>");

   // Send the authentication to the server
   getSASLAuthentication().send(stanza.toString());
}



}	// end of class BgtaSASLDigestMD5Mechansim



/* end of BgtaSASLDigestMD5Mechanism.java */
