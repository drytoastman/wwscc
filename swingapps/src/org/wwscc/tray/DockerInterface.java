package org.wwscc.tray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wwscc.util.Prefs;

/**
 * Thread to keep pinging our services to check their stlatus.  It pauses for 3 seconds but can
 * be woken by anyone calling notify on the class object.
 */
public class DockerInterface
{
	private static final Logger log = Logger.getLogger(DockerInterface.class.getName());
	private static String[] compose = { "docker-compose", "-p", "nwrsc", "-f", "docker-compose.yaml" };
	private static String[] machine = { "docker-machine" };
	private static File base = new File(Prefs.getDocRoot());
	private static Map<String,String> dockerenv = new HashMap<String, String>();
			
	public static void machineenv()
	{
		try (Scanner scan = new Scanner(collectit(build(base, machine, "env", "--shell", "cmd")))) 
		{
			while (scan.hasNext())
			{
				String set = scan.next();
				String var = scan.next();
				if (set.equals("SET")) {
					String p[] = var.split("=");
					dockerenv.put(p[0], p[1]);
				} else {
					scan.nextLine();
				}
			}
		}		
		catch (NoSuchElementException nse)
		{
		}
		
		System.out.println(dockerenv);
	}
	
	public static boolean machinepresent()
	{
		return runit(build(base, machine, "-h")) == 0;
	}
	
	public static boolean machinecreated()
	{
		try (Scanner scan = new Scanner(collectit(build(base, machine, "ls")))) {
			scan.nextLine();
			return (scan.hasNextLine());
		}
	}
	
	public static boolean createmachine()
	{
		return runit(build(base, machine, "create", "-d", "virtualbox", "default")) == 0;
	}
	
	public static boolean machinerunning()
	{
		return runit(build(base, machine, "ip")) == 0;		
	}
	
	public static boolean startmachine()
	{
		return runit(build(base, machine, "start")) == 0;
	}

	public static boolean up()
	{
		return runit(build(base, compose, "up", "-d")) == 0;		
	}

	public static boolean down()
	{
		return runit(build(base, compose, "down")) == 0;		
	}
	
	public static boolean[] ps()
	{
		boolean ret[] = new boolean[] { false, false };
		try (Scanner scan = new Scanner(collectit(build(base, compose, "ps")))) 
		{
			scan.nextLine();
			scan.nextLine();
			while (scan.hasNextLine())
			{
				String name = scan.next();
				boolean up  = scan.nextLine().contains("Up");
				if (name.contains("nwrsc_web")) {
					ret[0] = up;
				} else if (name.contains("nwrsc_db")) {
					ret[1] = up;
				}
			}
		}
		catch (NoSuchElementException nse)
		{
		}
		return ret;		
	}

	private static ProcessBuilder build(File root, String[] cmd, String ... additional)
	{
		List<String> cmdlist = new ArrayList<String>();
		for (String s : cmd)
			cmdlist.add(s);
		for (String s : additional)
			cmdlist.add(s);
		ProcessBuilder p = new ProcessBuilder(cmdlist);
        p.directory(root);
        Map <String,String> env = p.environment();
        env.putAll(dockerenv);
        return p;
	}
	
	private static String collectit(ProcessBuilder in)
	{
		try {
            Process p = in.start();
            int ret = p.waitFor();
            log.log(Level.FINE, "{0} returns {1}", new Object [] { in.command().toString(), ret });
            
            InputStream is = p.getInputStream();
            byte buf[] =  new byte[8192];
            is.read(buf);
            is.close();
            p.destroy();
            return new String(buf);
		} catch (InterruptedException | IOException ie) {
			log.log(Level.WARNING, "Exec failed " + ie, ie);
		}
		return "";
	}
	
	private static int runit(ProcessBuilder in)
	{
		try {
            Process p = in.start();
            int ret = p.waitFor();
            log.log(Level.FINE, "{0} returns {1}", new Object [] { in.command().toString(), ret });
            return ret;
		} catch (InterruptedException | IOException ie) {
			log.log(Level.WARNING, "Exec failed " + ie, ie);
		}
		return -1;
	}
}
