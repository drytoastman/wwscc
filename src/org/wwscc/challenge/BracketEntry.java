/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.challenge;

import org.wwscc.storage.Entrant;

/**
 *
 * @author bwilson
 */
public class BracketEntry {
	public Entrant entrant;
	public double dialin;

	BracketEntry(Entrant entrant, double dialin) {
		this.entrant = entrant;
		this.dialin = dialin;
	}
	
}
