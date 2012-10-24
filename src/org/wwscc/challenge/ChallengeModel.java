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
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wwscc.dialogs.SimpleFinderDialog;
import org.wwscc.storage.Challenge;
import org.wwscc.storage.ChallengeRound;
import org.wwscc.storage.ChallengeRound.RoundEntrant;
import org.wwscc.storage.ChallengeRun;
import org.wwscc.storage.Database;
import org.wwscc.storage.Entrant;
import org.wwscc.storage.LeftRightDialin;
import org.wwscc.storage.Run;
import org.wwscc.timercomm.TimerClient;
import org.wwscc.util.MT;
import org.wwscc.util.MessageListener;
import org.wwscc.util.Messenger;

/**
 * Represents the data storage for the active challenge.
 */
public class ChallengeModel implements MessageListener
{
	private static final Logger log = Logger.getLogger(ChallengeModel.class.getCanonicalName());

	Map<Integer, Map<Integer, ChallengeRound>> rounds;
	Map<Integer, Challenge> challenges;
	Map<Integer, Entrant> entrantcache;
	LinkedList<Id.Run> leftTargets;
	LinkedList<Id.Run> rightTargets;
	TimerClient client;
		
	/**
	 * Create new model
	 */
	public ChallengeModel()
	{
		entrantcache = new HashMap<Integer,Entrant>();
		challenges = new HashMap<Integer, Challenge>();
		rounds = new HashMap<Integer, Map<Integer,ChallengeRound>>();
		leftTargets = new LinkedList<Id.Run>();
		rightTargets = new LinkedList<Id.Run>();
		client = null;

		Messenger.register(MT.ACTIVE_CHANGE_REQUEST, this);
		Messenger.register(MT.CONNECT_REQUEST, this);
		Messenger.register(MT.EVENT_CHANGED, this);
		Messenger.register(MT.NEW_CHALLENGE, this);
		Messenger.register(MT.CHALLENGE_EDIT_REQUEST, this);
		Messenger.register(MT.TIMER_SERVICE_RUN, this);
		Messenger.register(MT.TIMER_SERVICE_DELETE, this);
		Messenger.register(MT.AUTO_WIN, this);
		Messenger.register(MT.MOVE_ROUND, this);
	}
	
	
	/**
	 * Called when there is a request to change the focus for incoming runs
	 * @param request the request being made
	 */
	protected void makeActive(ActivationRequest request)
	{
		Id.Run rid = request.runToChange;
		ChallengeRound r = getRound(rid);
		boolean topleft = (rid.isUpper()&& rid.isLeft()) || (rid.isLower() && rid.isRight());
		
		if (request.sendDials)
		{
			if (client == null)
			{
				log.severe("Unabled to send dialins, not connected");
				return;
			}
			
			LeftRightDialin msg = new LeftRightDialin();
			if (topleft)
			{
				msg.left = r.getTopCar().getDial();
				msg.right = r.getBottomCar().getDial();
			}
			else	
			{
				msg.left = r.getBottomCar().getDial();
				msg.right = r.getTopCar().getDial();
			}
			client.sendDial(msg);
		}
		
		if (request.makeActive)
		{
			switch (leftTargets.size())
			{
				default:
					leftTargets.clear();  // reseting and starting like fresh
					rightTargets.clear();
					// fallthrough on purpose
				case 0:
				case 1:
					if (topleft)
					{
						leftTargets.add(rid.makeUpperLeft());
						leftTargets.add(rid.makeLowerLeft());
						rightTargets.add(rid.makeLowerRight());
						rightTargets.add(rid.makeUpperRight());
					}
					else
					{
						leftTargets.add(rid.makeLowerLeft());
						leftTargets.add(rid.makeUpperLeft());
						rightTargets.add(rid.makeUpperRight());
						rightTargets.add(rid.makeLowerRight());
					}
					break;
			}
		}
		else
		{
			if (rid.isLeft())
				leftTargets.remove(rid);
			else
				rightTargets.remove(rid);
		}

		Messenger.sendEvent(MT.ACTIVE_CHANGE, null);
	}
	
	/**
	 * Get the timer receiving state for a run in the tree
	 * @param rid the run identifier (round, top/bottom, left/right)
	 * @return an index for where it is in the queue, -1 for not in the queue
	 */
	public int getState(Id.Run rid)
	{
		if (rid.isLeft())
			return leftTargets.indexOf(rid);
		else
			return rightTargets.indexOf(rid);		
	}

	/**
	 * @param rid the run identifier (round, top/bottom, left/right)
	 * @return the associated run from the model
	 */
	public ChallengeRun getRun(Id.Run rid)
	{
		ChallengeRound r = getRound(rid);
		RoundEntrant re = (rid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		return (rid.isLeft()) ? re.getLeft() : re.getRight();
	}


	/**
	 * Find the associated run, set a new time value, update round, let everyone know
	 * @param rid the run in question
	 * @param time the new time value
	 */
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


	/**
	 * Find the associated run, set a new cones value, update round, let everyone know
	 * @param rid the run in question
	 * @param cones the new cones value
	 */
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

	/**
	 * Find the associated run, set a new gates value, update round, let everyone know
	 * @param rid the run in question
	 * @param gates the new gates value
	 */
	public void setGates(Id.Run rid, int gates)
	{
		ChallengeRun r = getRun(rid);
		if (r != null)
		{
			r.setGates(gates);
			Database.d.updateRun(r);
		}
		
		checkForWinner(rid);
		Messenger.sendEvent(MT.RUN_CHANGED, r);
	}


	/**
	 * Find the associated run, set a new status value, update round, let everyone know
	 * @param rid the run in question
	 * @param status the new status value
	 */
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
	
	/**
	 * Set an entrant in the tree
	 * @param eid the pointer to a round and side (upper/lower)
	 * @param e the entrant to place there
	 * @param dialin the dialin to associated with this round placement
	 */
	public void setEntrant(Id.Entry eid, Entrant e, double dialin)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		if (e != null)
		{
			re.setCar(e.getCarId());
			re.setDial(dialin);
		}
		else
		{
			re.setCar(-1);
			re.setDial(0.0);
		}
		Database.d.updateChallengeRound(r);
	}
	
	/**
	 * @param eid the round and side (upper/lower) identifier
	 * @return the current entrant at a particular location in the tree
	 */
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
	
	/**
	 * @param eid the round and side (upper/lower) identifier
	 * @return gets he dialin used for the specified location in the tree
	 */
	public Double getDial(Id.Entry eid)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		return re.getDial();
	}

	/**
	 * Set a new dial value for an entrant in the tree
	 * @param eid the round and side (upper/lower) identifier
	 * @param newDial the new dialin to set for this round
	 */
	public void overrideDial(Id.Entry eid, double newDial)
	{
		ChallengeRound r = getRound(eid);
		RoundEntrant re = (eid.isUpper()) ? r.getTopCar() : r.getBottomCar();
		re.setDial(newDial);
		re.setNewDial(newDial);
		Database.d.updateChallengeRound(r);
	}
	
	/**
	 * @param rid a round identifier in the challenge bracket
	 * @return the round information for the requested round
	 */
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
	 * Remove all run information associated with a round in case the data is bad
	 * @param rid the round identifier
	 */
	public void resetRound(Id.Round rid)
	{
		ChallengeRound r = getRound(rid);
		
		RoundEntrant top = r.getTopCar();
		RoundEntrant bottom = r.getBottomCar();
		
		if (top != null)
		{
			if (top.getLeft() != null)
				Database.d.deleteRun(top.getLeft().getId());
			if (top.getRight() != null)
				Database.d.deleteRun(top.getRight().getId());
			top.reset();
		}
		
		if (bottom != null)
		{
			if (bottom.getLeft() != null)
				Database.d.deleteRun(bottom.getLeft().getId());
			if (bottom.getRight() != null)
				Database.d.deleteRun(bottom.getRight().getId());
			bottom.reset();
		}

		Database.d.updateChallengeRound(r);
	}

	/**
	 * Called to set Run data received from the timer.  Uses the current
	 * active run information to determine where to apply it
	 * @param run the Run data received from the timer
	 */
	private void setRun(Run run)
	{
		/* Get the active left or right run */
		Id.Run rid = (run.course() == Run.LEFT) ? leftTargets.peekFirst() : rightTargets.peekFirst();
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

		Double raw = cr.getRaw();
		if (raw.isNaN() || (raw == 0))
		{
			// updated reaction or sixty from timer
		}
		else
		{  // pull out the active pointer, check for winner and let everyone know the active has changed
			checkForWinner((run.course() == Run.LEFT) ? leftTargets.pollFirst() : rightTargets.pollFirst());
			Messenger.sendEvent(MT.ACTIVE_CHANGE, null);
		}
		
		Messenger.sendEvent(MT.RUN_CHANGED, cr);
	}

	private void checkForWinner(Id.Round rid)
	{
		ChallengeRound round = getRound(rid);
		ChallengeRun topLeft = round.getTopCar().getLeft();
		ChallengeRun topRight = round.getTopCar().getRight();
		ChallengeRun bottomLeft = round.getBottomCar().getLeft();
		ChallengeRun bottomRight = round.getBottomCar().getRight();
		ChallengeRound.RoundEntrant winner = null;
		
		switch (round.getState())
		{
			case HALFNORMAL:
				if (topLeft.statusLevel() == bottomRight.statusLevel()) // both OK, both DNF or both RL
					break;
				else if (topLeft.statusLevel() < bottomRight.statusLevel())
					winner = round.getTopCar();
				else
					winner = round.getBottomCar();
				break;
				
			case HALFINVERSE:
				if (topRight.statusLevel() == bottomLeft.statusLevel()) // both OK, both DNF or both RL
					break;
				else if (topRight.statusLevel() < bottomLeft.statusLevel())
					winner = round.getBottomCar();
				else
					winner = round.getTopCar();
				break;
				
			case DONE:
				round.getTopCar().setResultByNet(topLeft.getNet() + topRight.getNet());
				round.getBottomCar().setResultByNet(bottomLeft.getNet() + bottomRight.getNet());
				int topLevel = topLeft.statusLevel() + topRight.statusLevel();
				int botLevel = bottomLeft.statusLevel() + bottomRight.statusLevel();
				
				if ((topLevel > 1) && (botLevel > 1))
					break; // no winner
				else if (topLevel > 0)
					winner = round.getBottomCar();
				else if (botLevel > 0)
					winner = round.getTopCar();
				else if (round.getTopCar().getResult() < round.getBottomCar().getResult())
					winner = round.getTopCar();
				else if (round.getBottomCar().getResult() < round.getTopCar().getResult())
					winner = round.getBottomCar();
				//else no winner due to tie
				break;
		}
		
		Database.d.updateChallengeRound(round);
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
			loser = (winner == round.getTopCar()) ? round.getBottomCar() : round.getTopCar();
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
					InetSocketAddress newAddr;
					SimpleFinderDialog dialog = new SimpleFinderDialog("ProTimer");
					dialog.doDialog("Find Pro Timers", null);
					if ((newAddr = dialog.getResult()) != null)
					{
						if (client != null)
							client.stop();
						client = null;
						client = new TimerClient(newAddr);
						client.start();
					}
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Failed to connect: {0}", e.getMessage());
				}
				break;

			case TIMER_SERVICE_DELETE:
				log.info("Don't implement DELETE yet");
				break;

			case TIMER_SERVICE_RUN:
				setRun((Run)data);
				break;

			case ACTIVE_CHANGE_REQUEST:
				makeActive((ActivationRequest)data);
				break;
				
			case MOVE_ROUND:
				Id.Round[] rnds = (Id.Round[])data;
				ChallengeRound src = getRound(rnds[0]);
				ChallengeRound dst = getRound(rnds[1]);
				log.log(Level.FINE, "Transfer {0} to {1}", new Object[]{src, dst});
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
