/********************************************************************************/
/*                                                                              */
/*              BhelpException.java                                             */
/*                                                                              */
/*      Exception for use in bubbles help demonstrations                        */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;



class BhelpException extends Exception
{

   
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BhelpException(String msg)
{
   super(msg);
}  


BhelpException(String msg,Throwable t)
{
   super(msg,t);
}


}       // end of class BhelpException




/* end of BhelpException.java */
