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

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 *	This class is used to create and send email messages.
 **/

public class BoardMail implements BoardConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static BoardMail mail_handler;


static {
   mail_handler = new BoardMail();
}



/********************************************************************************/
/*										*/
/*	Static interface							*/
/*										*/
/********************************************************************************/

/**
 *	Create a new email message being sent to the given address.
 **/

public static BoardMailMessage createMessage(String to)
{
   return mail_handler.createMessageImpl(to);
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardMail()
{ }



/********************************************************************************/
/*										*/
/*	Message creation methods						*/
/*										*/
/********************************************************************************/

private MessageImpl createMessageImpl(String to)
{
   return new MessageImpl(to);
}



/********************************************************************************/
/*										*/
/*	Message implementation							*/
/*										*/
/********************************************************************************/

private static class MessageImpl implements BoardMailMessage
{
   private String mail_to;
   private String subject_text;
   private String body_text;
   private String reply_to;
   private List<File> attachment_files;

   MessageImpl() {
      mail_to = null;
      subject_text = "Code Bubbles";
      body_text = null;
      reply_to = null;
      attachment_files = new ArrayList<>();
    }

   MessageImpl(String to) {
      this();
      mail_to = to;
    }

   @Override public void setSubject(String s)			{ subject_text = s; }
   @Override public void setReplyTo(String s)			{ reply_to = s; }
   @Override public void addBodyText(String t) {
      if (body_text == null) body_text = t;
      else {
	 if (!body_text.endsWith("\n")) body_text += "\n";
	 body_text += t;
       }
    }

   @Override public void addAttachment(File f) {
      if (f == null) return;
      if (attachment_files == null) attachment_files = new ArrayList<>();
      attachment_files.add(f);
    }

   @Override public void addAttachments(List<File> lf) {
      if (lf == null) return;
      if (attachment_files == null) attachment_files = new ArrayList<>();
      attachment_files.addAll(lf);
    }


   @Override public void send() {
      // Sender's email ID needs to be mentioned
      String from = "728238@gmail.com";//change accordingly
      final String username = "728238";//change accordingly
      final String password = "Read3Care";//change accordingly
   
      // Assuming you are sending email through relay.jangosmtp.net
      String host = "smtp.gmail.com";
   
      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", "587");
   
      // Get the Session object.
      Session session = Session.getInstance(props,
            new javax.mail.Authenticator() {
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
          }
       });
   
      try {
         // Create a default MimeMessage object.
         Message message = new MimeMessage(session);
   
         // Set From: header field of the header.
         message.setFrom(new InternetAddress(from));
   
         // Set To: header field of the header.
         message.setRecipients(Message.RecipientType.TO,
               InternetAddress.parse(mail_to));
   
         // Set Subject: header field
         message.setSubject(subject_text);
   
         if (reply_to != null) {
            Address [] rply = new Address[] { new InternetAddress(reply_to) };
            message.setReplyTo(rply);
          }
   
         // Now set the actual message
         if (attachment_files == null || attachment_files.size() == 0) {
            message.setText(body_text);
          }
         else {
            message.setText(body_text);
            MimeMultipart mutlipart = new MimeMultipart();
            MimeBodyPart bp = new MimeBodyPart();
            bp.setText(body_text);
            mutlipart.addBodyPart(bp);
            for (File f : attachment_files) {
               try {
                  MimeBodyPart fpt = new MimeBodyPart();
                  fpt.attachFile(f);
                  mutlipart.addBodyPart(fpt);
                }
               catch (IOException e) {
                  BoardLog.logE("BOARD","Problem attaching file " + f + " to mail message");
        	}
               // bp = new MimeBodyPart();
               // DataSource fds = new FileDataSource(f);
               // bp.setDataHandler(new DataHandler(fds));
               // mutlipart.addBodyPart(bp);
             }
            message.setContent(mutlipart);
          }
   
         // Send message
         Transport.send(message);
       }
      catch (MessagingException e) {
         BoardLog.logE("BOARD","Problem sending mail",e);
       }
   }

}	// end of inner class MessageImpl




}	// end of class BoardMail



/* end of BoardMail.java */
