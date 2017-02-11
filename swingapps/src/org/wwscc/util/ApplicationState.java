package org.wwscc.util;

import java.util.UUID;

import org.wwscc.storage.Event;

public class ApplicationState {

	   String host;
	   String seriesName;
	   Event currentEvent;
	   int currentCourse;
	   int currentRunGroup;
	   UUID currentChallengeId;
	
	   public void setCurrentEvent(Event e) { currentEvent = e; }
	   public void setCurrentCourse(int course) { currentCourse = course; }
	   public void setCurrentRunGroup(int rungroup) { currentRunGroup = rungroup; }
	   public void setCurrentChallengeId(UUID challengeid) { currentChallengeId = challengeid; }
	
	   public String getCurrentHost() { return host; }
	   public String getCurrentSeries() { return seriesName; }
	   public Event getCurrentEvent() { return currentEvent; }
	   public UUID getCurrentEventId() { return currentEvent.getEventId(); }
	   public int getCurrentCourse() { return currentCourse; }
	   public int getCurrentRunGroup() { return currentRunGroup; }
	   public UUID getCurrentChallengeId() { return currentChallengeId; }	
}
