/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.storage;

import org.json.simple.parser.ParseException;

/**
 *
 * @author bwilson
 */
public interface Serial
{
	public String encode();
	public void decode(String s) throws ParseException;
}
