/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

package org.wwscc.challenge;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.ChallengeRound;
import org.wwscc.storage.ChallengeRound.RoundEntrant;
import org.wwscc.storage.ChallengeRun;
import org.wwscc.storage.Database;
import org.wwscc.storage.Dialins;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.LeftRightDialin;
import org.wwscc.storage.Run;
import org.wwscc.timercomm.ServiceFinder;
import org.wwscc.timercomm.ServiceFinder.FoundService;
import org.wwscc.timercomm.TimerClient;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Represents the data storage for the active challenge.
 */
public class ChallengeModel implements MessageListener
{
	private static Logger log = Logger.getLogger(ChallengeModel.class.getCanonicalName());

	Map<Integer, Map<Integer, ChallengeRound>> rounds;
	Map<Integer, Challenge> challenges;
	Map<Integer, Entrant> entrantcache;
	Id.Run activeLeft, activeRight;
	Id.Run nextLeft, nextRight;
	Id.Run thirdLeft, thirdRight;
	TimerClient client;
	Dialins dialins;
		
	public ChallengeModel()
	{
		entrantcache = new HashMap<Integer,Entrant>();
		challenges = new HashMap<Integer, Challenge>();
		rounds = new HashMap<Integer, Map<Integer,ChallengeRound>>();
		activeLeft = activeRight = null;
		nextLeft = nextRight = null;
		thirdLeft = thirdRight = null;
		dialins = new Dialins();
		client = null;

		Messenger.register(MT.CONNECT_REQUEST, this);
		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.NEW_CHALLENGE, this);
		Messenger.register(MT.TIMER_SERVICE_RUN, this);
		Messenger.register(MT.TIMER_SERVICE_DELETE, this);
		Messenger.register(MT.AUTO_WIN, this);
		Messenger.register(MT.MOVE_ROUND, this);
	}
	
	public int getDepth(int id)
	{
		Challenge c = challenges.get(id);
		if (c == null)
			return 0;
		return c.getDepth();
	}
	
	public boolean isBonus(int id)
	{
		Challenge c = challenges.get(id);
		if (c == null)
			return true;
		return c.isBonus();
	}
	
	public void makeActive(Id.Run rid)
	{
		ChallengeRound r = getRound(rid);
		boolean topleft = false;
		LeftRightDialin msg = new LeftRightDialin();

		if (rid.isUpper())
		{
			if (rid.isLeft())
			{
				topleft = true;
				msg.left = r.getTopCar().getDial();
				msg.right = r.getBottomCar().getDial();
			}
			else
			{
				topleft = false;
				msg.left = r.getBottomCar().getDial();
				msg.right = r.getTopCar().getDial();
			}
		}
		else
		{
			if (rid.isLeft())
			{
				topleft = false;
				msg.left = r.getBottomCar().getDial();
				msg.right = r.getTopCar().getDial();
			}
			else
			{
				topleft = true;
				msg.left = r.getTopCar().getDial();
				msg.right = r.getBottomCar().getDial();
			}
		}

		if (client != null)
			client.sendDial(msg);
		
		if ((activeLeft == null) || (nextLeft != null))
		{
			thirdLeft = thirdRight = null;
			if (topleft)
			{
				activeLeft = rid.makeUpperLeft();
				activeRight = rid.makeLowerRight();
				nextLeft = rid.makeLowerLeft();
				nextRight = rid.makeUpperRight();
			}
			else
			{
				activeLeft = rid.makeLowerLeft();
				activeRight = rid.makeUpperRight();
				nextLeft = rid.makeUpperLeft();
				nextRight = rid.makeLowerRight();
			}
		}
		else if (nextLeft == null)
		{
			if (topleft)
			{
				nextLeft = rid.makeUpperLeft();
				nextRight = rid.makeLowerRight();
				thirdLeft = rid.makeLowerLeft();
				thirdRight = rid.makeUpperRight();
			}
			else
			{
				nextLeft = rid.makeLowerLeft();
				nextRight = rid.makeUpperRight();
				thirdLeft = rid.makeUpperLeft();
				thirdRight = rid.makeLowerRight();
			}
		}

		Messenger.sendEvent(MT.ACTIVE_CHANGE, null);
	}
	
	public enum RunState { NONE, PENDING, ACTIVE };
	public RunState getState(Id.Run rid)
	{
		if (rid.equals(activeLeft) || rid.equals(activeRight))
			return RunState.ACTIVE;
		else if (rid.equals(nextLeft) || rid.equals(nextRight))
			return RunState.PENDING;
		else
			return RunState.NONE;		
	}

	public ChallengeRun getRun(Id.Run rid)
	{
		ChallengeRound r = getRound(rid);
		RoundEntrant re = (rid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		return (rid.isLeft()) ? re.getLeft() : re.getRight();
	}


	public void setTime(Id.Run rid, double time)
	{
		ChallengeRun r = getRun(rid);
		if (r != null)
		{
			r.setRaw(time);
			Database.d.updateRun(r);
		}
		else
		{
			r = new ChallengeRun();
			r.setRaw(time);
			r.setStatus("OK");
			r.setChallengeRound(rid);

			ChallengeRound round = getRound(rid);
			RoundEntrant re = (rid.isUpper()) ? round.getTopCar() : round.getBottomCar();
			r.setCarId(re.getCar());
			round.applyRun(r);
			Database.d.insertRun(r);
		}

		checkForWinner(rid);
		Messenger.sendEvent(MT.RUN_CHANGED, r);
	}

	
	public void setCones(Id.Run rid, int cones)
	{
		ChallengeRun r = getRun(rid);
		if (r != null)
		{
			r.setCones(cones);
			Database.d.updateRun(r);
		}
		
		checkForWinner(rid);
		Messenger.sendEvent(MT.RUN_CHANGED, r);
	}
	
	public void setStatus(Id.Run rid, String status)
	{
		ChallengeRun r = getRun(rid);
		if (r != null)
		{
			r.setStatus(status);
			Database.d.updateRun(r);
		}
		
		checkForWinner(rid);
		Messenger.sendEvent(MT.RUN_CHANGED, r);
	}
	
	public void setEntrant(Id.Entry eid, Entrant e)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		if (e != null)
		{
			re.setCar(e.getCarId());
			re.setDial(dialins.getDial(e.getCarId(), isBonus(eid.challengeid)));
		}
		else
		{
			re.setCar(-1);
			re.setDial(0.0);
		}
		Database.d.updateChallengeRound(r);
	}
	
	public Entrant getEntrant(Id.Entry eid)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		Entrant e = entrantcache.get(re.getCar());
		if ((e == null) && (re.getCar() > 0))
		{
			e = Database.d.loadEntrant(re.getCar(), false);  // TODO: Don't need runs per say
			entrantcache.put(re.getCar(), e);
		}		
		return e;
	}
	
	public Double getDial(Id.Entry eid)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		return re.getDial();
	}
	
	public ChallengeRound getRound(Id.Round rid)
	{
		if (rid == null)
			return null;
		Map<Integer,ChallengeRound> m = rounds.get(rid.challengeid);
		if (m == null)
			return null;
		return m.get(rid.round);
	}

	/**
	 * Called to set Run data received from the timer.  Uses the current
	 * active run information to determine where to apply it
	 * @param run the Run data received from the timer
	 */
	private void setRun(Run run)
	{
		/* Get the active left or right run */
		Id.Run rid = (run.course() == Run.LEFT) ? activeLeft: activeRight;
		ChallengeRound rnd = getRound(rid);
		RoundEntrant re = (rid.isUpper()) ? rnd.getTopCar() : rnd.getBottomCar();
		ChallengeRun cr = (rid.isLeft()) ? re.getLeft() : re.getRight();
		
		if (cr == null)
		{
			cr = new ChallengeRun(run);
			cr.setChallengeRound(rid);
			cr.setCarId(re.getCar());
			getRound(rid).applyRun(cr);
			Database.d.insertRun(cr);
		}
		else
		{
			cr.setCourse(run.course());
			cr.setReaction(run.getReaction());
			cr.setSixty(run.getSixty());
			cr.setRaw(run.getRaw());
			cr.setStatus(run.getStatus());
			Database.d.updateRun(cr);
		}
		
		if (run.course() == Run.LEFT)
		{
			checkForWinner(activeLeft);
			activeLeft = nextLeft;
			nextLeft = thirdLeft;
			thirdLeft = null;
		}
		else
		{
			checkForWinner(activeRight);
			activeRight = nextRight;
			nextRight = thirdRight;
			thirdRight = null;
		}
		
		Messenger.sendEvent(MT.ACTIVE_CHANGE, null);
		Messenger.sendEvent(MT.RUN_CHANGED, cr);
	}

	private void checkForWinner(Id.Round rid)
	{
		ChallengeRound r = getRound(rid);
		ChallengeRun tl = r.getTopCar().getLeft();
		ChallengeRun tr = r.getTopCar().getRight();
		ChallengeRun bl = r.getBottomCar().getLeft();
		ChallengeRun br = r.getBottomCar().getRight();
		ChallengeRound.RoundEntrant winner = null;
		
		switch (r.getState())
		{
			case HALFNORMAL:
				if (!tl.isOK())
					winner = r.getBottomCar();
				else if (!br.isOK())
					winner = r.getTopCar();
				break;
				
			case HALFINVERSE:
				if (!tr.isOK())
					winner = r.getBottomCar();
				if (!bl.isOK())
					winner = r.getTopCar();
				break;
				
			case DONE:
				r.getTopCar().setResultByNet(tl.getNet() + tr.getNet());
				r.getBottomCar().setResultByNet(bl.getNet() + br.getNet());
				
				if ((!tl.isOK()) || (!tr.isOK()))
					winner = r.getBottomCar();
				else if ((!bl.isOK()) || (!br.isOK()))
					winner = r.getTopCar();
				else if (r.getTopCar().getResult() < r.getBottomCar().getResult())
					winner = r.getTopCar();
				else if (r.getBottomCar().getResult() < r.getTopCar().getResult())
					winner = r.getBottomCar();
				else
					log.warning("Strange state checking for winner, there is none");
				break;
		}
		
		Database.d.updateChallengeRound(r);
		if (winner == null)
			return;
		
		/* Advance the winner */
		Id.Entry eid = rid.advancesTo();
		ChallengeRound next = getRound(eid);
		if (eid.isUpper())
			next.getTopCar().setTo(winner);
		else
			next.getBottomCar().setTo(winner);
		Database.d.updateChallengeRound(next);
		Messenger.sendEvent(MT.ENTRANT_CHANGED, eid);
		
		/* Special advance to third place bracket for losers in semifinal */
		Id.Entry thirdid = rid.advanceThird();
		if (thirdid != null)
		{
			ChallengeRound.RoundEntrant loser;
			loser = (winner == r.getTopCar()) ? r.getBottomCar() : r.getTopCar();
			ChallengeRound third = getRound(thirdid);
			if (thirdid.isUpper())
				third.getTopCar().setTo(loser);
			else
				third.getBottomCar().setTo(loser);
			Database.d.updateChallengeRound(third);
			Messenger.sendEvent(MT.ENTRANT_CHANGED, thirdid);
		}

	}
	
	/**
	 * Called to (re)load the data from the database into our local storage.
	 * @param e  the event to load challenge data for
	 */
	private void loadEventData()
	{
		challenges.clear();
		rounds.clear();
		entrantcache.clear();
		
		for (Challenge c : Database.d.getChallengesForEvent())
		{
			challenges.put(c.getId(), c);
			
			HashMap <Integer, ChallengeRound> map = new HashMap <Integer, ChallengeRound>();
			rounds.put(c.getId(), map);
			
			for (ChallengeRound r : Database.d.getRoundsForChallenge(c.getId()))
				map.put(r.getRound(), r);

			/* Load all the runs and link them in the appropriate rounds. */
			for (ChallengeRun run : Database.d.getRunsForChallenge(c.getId()))
			{
				Id.Round rid = new Id.Round(run.getChallengeId(), run.getRound());
				ChallengeRound round = getRound(rid);
				round.applyRun(run);
			}
		}
		
		dialins = Database.d.loadDialins();
		
		Messenger.sendEvent(MT.MODEL_CHANGED, this);
	}

	@Override
	public void event(MT type, Object data)
	{
		switch (type)
		{
			case EVENT_CHANGED:
				loadEventData();
				break;
				
			case NEW_CHALLENGE:
				loadEventData();
				break;
				
			case CONNECT_REQUEST:
				try
				{
					FoundService service;
					if ((service = ServiceFinder.dialogFind("ProTimer")) == null)
							return;
					if (client != null)
						client.close();
					client = null;
					client = new TimerClient(new InetSocketAddress(service.host, service.port));
					new Thread(client, "ProTimerClient").start();
				}
				catch (Exception e)
				{
					log.severe("Failed to connect: " + e);
				}
				break;

			case TIMER_SERVICE_DELETE:
				log.info("Don't implement DELETE yet");
				break;

			case TIMER_SERVICE_RUN:
				setRun((Run)data);
				break;

			case MOVE_ROUND:
				Id.Round[] rnds = (Id.Round[])data;
				ChallengeRound src = getRound(rnds[0]);
				ChallengeRound dst = getRound(rnds[1]);
				System.out.println("Transfer " + src + " to " + dst);
				break;
				
			case AUTO_WIN:
				Id.Entry eid = (Id.Entry)data;
				ChallengeRound r = getRound(eid);
				RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
				
				Id.Entry nextid = eid.advancesTo();
				ChallengeRound next = getRound(nextid);
				if (nextid.isUpper())
					next.getTopCar().setTo(re);
				else
					next.getBottomCar().setTo(re);
				Database.d.updateChallengeRound(next);
				Messenger.sendEvent(MT.ENTRANT_CHANGED, nextid);
		}
	}
}
