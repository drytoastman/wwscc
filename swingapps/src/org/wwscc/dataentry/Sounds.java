/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


package org.wwscc.dataentry;

import java.applet.*;

public class Sounds 
{
	static AudioClip menuClip;
	static AudioClip startClip;
	static AudioClip blockedClip;

	static
	{
		try { menuClip = Applet.newAudioClip(ClassLoader.getSystemResource("org/wwscc/sounds/menu.wav")); } catch (Exception e) {}
		try { startClip = Applet.newAudioClip(ClassLoader.getSystemResource("org/wwscc/sounds/start.wav")); } catch (Exception e) {}
		try { blockedClip = Applet.newAudioClip(ClassLoader.getSystemResource("org/wwscc/sounds/blocked.wav")); } catch (Exception e) {}
	}

	public static void playMenu() { if (menuClip != null) menuClip.play(); }
	public static void playStart() { if (startClip != null) startClip.play(); }
	public static void playBlocked() { if (blockedClip != null) blockedClip.play(); }
}

