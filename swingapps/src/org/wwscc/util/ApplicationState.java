/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2017 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.util;

import org.wwscc.storage.Event;

public class ApplicationState {

	   String host;
	   String seriesName;
	   Event currentEvent;
	   int currentCourse;
	   int currentRunGroup;
	   int currentChallengeId;
	
	   public void setCurrentEvent(Event e) { currentEvent = e; }
	   public void setCurrentCourse(int course) { currentCourse = course; }
	   public void setCurrentRunGroup(int rungroup) { currentRunGroup = rungroup; }
	   public void setCurrentChallengeId(int challengeid) { currentChallengeId = challengeid; }
	
	   public String getCurrentHost() { return host; }
	   public String getCurrentSeries() { return seriesName; }
	   public Event getCurrentEvent() { return currentEvent; }
	   public int getCurrentEventId() { return currentEvent.getEventId(); }
	   public int getCurrentCourse() { return currentCourse; }
	   public int getCurrentRunGroup() { return currentRunGroup; }
	   public int getCurrentChallengeId() { return currentChallengeId; }	
}
