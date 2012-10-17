/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wwscc.protimer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;
import org.wwscc.util.TimeTextField;

/**
 * Panel to make sure of the simulator
 */
public class SimulatorPanel extends JFrame
{
	Simulator sim;
	InternalListener lis;
	TimeTextField time;
	TimeTextField dial;
	
	public SimulatorPanel()
	{
		super("Simulator");
		JPanel main = new JPanel(new MigLayout(""));
		main.setOpaque(true);
		main.setBackground(Color.WHITE);
		setContentPane(main);

		sim = new Simulator();
		lis = new InternalListener();
		time = new TimeTextField("0.000", 6);
		dial = new TimeTextField("0.000", 6);
		
		main.add(new JLabel("Time"), "split 4");
		main.add(time, "");
		main.add(new JLabel("Dial"), "");
		main.add(dial, "wrap");
		
		main.add(new JSeparator(), "wrap");
		
		main.add(button("tree"), "wrap");
		main.add(button("reaction left"), "split 2");
		main.add(button("reaction right"), "wrap");
		main.add(button("sixty left"), "split 2");
		main.add(button("sixty right"), "wrap");
		main.add(button("finish left"), "split 2");
		main.add(button("finish right"), "wrap");
		
		pack();
		setVisible(true);
	}
	
	private JButton button(String s)
	{
		JButton but = new JButton(s);
		but.addActionListener(lis);
		return but;
	}
	
	class InternalListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent ae) {
			String s = ae.getActionCommand();
			if (s.equals("tree"))
				sim.tree();
			else if (s.equals("reaction left"))
				sim.reaction(true, time.getTime(), ColorTime.NORMAL);
			else if (s.equals("reaction right"))
				sim.reaction(false, time.getTime(), ColorTime.NORMAL);
			else if (s.equals("sixty left"))
				sim.sixty(true, time.getTime(), ColorTime.NORMAL);
			else if (s.equals("sixty right"))
				sim.sixty(false, time.getTime(), ColorTime.NORMAL);
			else if (s.equals("finish left"))
				sim.finish(true, time.getTime(), dial.getTime(), ColorTime.NORMAL);
			else if (s.equals("finish right"))
				sim.finish(false, time.getTime(), dial.getTime(), ColorTime.NORMAL);
		}
	}
}
