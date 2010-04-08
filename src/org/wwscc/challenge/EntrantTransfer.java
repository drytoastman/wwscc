/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */
package org.wwscc.challenge;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.wwscc.storage.Entrant;


public class EntrantTransfer implements Transferable, ClipboardOwner
{
	Entrant entrant;
	static DataFlavor myFlavor;

	static
	{
		myFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + 
				"; class=org.wwscc.storage.Entrant", "Entrant");
	}

	public EntrantTransfer(Entrant e)
	{
		entrant = e;
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
			return entrant;
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
	}
}
