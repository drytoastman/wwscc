/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2012 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.dataentry;

import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Encompasses all the components in the announcer tab and handles the run change
 * events, keeping track of last to finish as well as next to finish result panels.
 */
public class AnnouncerPanel extends JFXPanel implements MessageListener
{
	WebEngine engine;
	
	public AnnouncerPanel()
	{		
		// uncomment to reload on run change Messenger.register(MT.RUN_CHANGED, this);		
		Platform.runLater(new Runnable() {
            @Override
            public void run() { init(); }
		});
	}
	
	private void init()
	{
		WebView view = new WebView();
		engine = view.getEngine();
		engine.load("http://google.com");
		setScene(new Scene(view));
	}

	@Override
	public void event(MT type, Object o)
	{
		switch (type)
		{
			case RUN_CHANGED:
				if (engine != null) {
					Platform.runLater(new Runnable () {
						@Override
						public void run() { engine.reload(); }
					});
				}
				break;
		}
	}
}
