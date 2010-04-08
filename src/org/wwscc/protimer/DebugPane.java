/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.protimer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

import org.wwscc.util.*;

public class DebugPane extends JPanel implements ActionListener
{
	JTextArea text;
	JTextField input;
	JButton enter;
	OutputStreamWriter file;

	PrintStream aPrintStream;

	public DebugPane() throws FileNotFoundException
	{
		super(new BorderLayout());

		text = new JTextArea();
		input = new JTextField(40);
		enter = new JButton("Send");
		enter.addActionListener(this);

		aPrintStream  = new PrintStream( new FilteredStream( new FileOutputStream("NUL:")));
		System.setOut(aPrintStream);
		System.setErr(aPrintStream);

		try {
			file = new FileWriter("debug.log", true);
		} catch (IOException ioe) {
			System.out.println("Can't open debug log");
		}

		JScrollPane sp = new JScrollPane(text);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		JPanel p = new JPanel();
		p.add(input);
		p.add(enter);

		add(p, BorderLayout.NORTH);
		add(sp, BorderLayout.CENTER);
	}


	public void actionPerformed(ActionEvent e)
	{
		String s = input.getText();
		if (!s.equals(""))
			Messenger.sendEvent(MT.INPUT_TEXT, s);

		input.setText("");
	}


	public void log(String s)
	{
		try { file.write(s); file.flush(); } catch (Exception e) {}
		text.append(s);
		text.setCaretPosition(text.getDocument().getLength());
	}


	class FilteredStream extends FilterOutputStream
	{
		public FilteredStream(OutputStream aStream)
		{ 
			super(aStream);
		}
	
		@Override
		public void write(byte b[]) throws IOException
		{
			String aString = new String(b);
			log(aString);
		}
		
		@Override
		public void write(byte b[], int off, int len) throws IOException
		{
			String aString = new String(b , off , len);
			log(aString);
		}
	}
}



