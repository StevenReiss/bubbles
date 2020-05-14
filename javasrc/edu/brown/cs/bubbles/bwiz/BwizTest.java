package edu.brown.cs.bubbles.bwiz;

import javax.swing.JFrame;

public class BwizTest implements BwizConstants
{
    public static void main(String args[])
    {

	BwizStartWizard pnl=new BwizStartWizard();
	JFrame awiz = new JFrame("BWIZ TEST");
	awiz.setContentPane(pnl);
	awiz.pack();
	awiz.setVisible(true);


    }
}
