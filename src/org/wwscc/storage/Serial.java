/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wwscc.storage;

/**
 *
 * @author bwilson
 */
public interface Serial <T>
{
	public String encode();
	public void decode(String s);
}
