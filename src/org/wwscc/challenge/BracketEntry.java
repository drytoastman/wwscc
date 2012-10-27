/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.challenge;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.wwscc.storage.Entrant;

/**
 */
public class BracketEntry 
{
	public Id.Entry source;
	public Entrant entrant;
	public double dialin;

	BracketEntry(Id.Entry source, Entrant entrant, double dialin) 
	{
		this.source = source;
		this.entrant = entrant;
		this.dialin = dialin;
	}
	
	public static class Transfer implements Transferable, ClipboardOwner
	{
		BracketEntry entry;
		static DataFlavor myFlavor;

		static
		{
			myFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + 
					"; class=org.wwscc.challenge.BracketEntry", "BracketEntry");
		}

		public Transfer(BracketEntry b)
		{
			entry = b;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors()
		{
			return new DataFlavor[] { myFlavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return (flavor.equals(myFlavor));
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
		{
			if (flavor.equals(myFlavor))
				return entry;
			throw new UnsupportedFlavorException(flavor);
		}

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents)
		{
		}
	}

}
