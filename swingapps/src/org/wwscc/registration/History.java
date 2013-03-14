package org.wwscc.registration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.wwscc.util.CSVParser;

public class History extends JFrame implements ActionListener
{
	private static final Logger log = Logger.getLogger(History.class.getCanonicalName());

	public History()
	{
		super("History");
		
		JMenu file = new JMenu("File");
		file.add(createItem("Open Local CSV"));
		file.add(createItem("Download CSV"));
		
		JMenuBar bar = new JMenuBar();
		bar.add(file);
		setJMenuBar(bar);
		
		pack();
		setVisible(true);
	}
	
	private JMenuItem createItem(String title)
	{
		JMenuItem item = new JMenuItem(title);
		item.addActionListener(this);
		return item;
	}

	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("Open Local CSV"))
		{
		}
		else if (cmd.equals("Download CSV"))
		{
		}
	}
	
	/**
	 * Retrieve the history report from the main host
	 * @return a map of lower case name to map of key/values
	 * @throws URISyntaxException 
	 * @throws UnsupportedEncodingException 
	 */
	public void getHistory(String host, int isttotal, int istavg, String series) throws IOException, URISyntaxException
	{
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		param.add(new BasicNameValuePair("isttotal", ""+isttotal));
		param.add(new BasicNameValuePair("istavg", ""+istavg));
		param.add(new BasicNameValuePair("pcseries", series));
		param.add(new BasicNameValuePair("pcyearmax", "4"));
		param.add(new BasicNameValuePair("pcsinceyear", ""+(Calendar.getInstance().get(Calendar.YEAR) - 2)));
		param.add(new BasicNameValuePair("pcchamp", "on"));		
		//param.add(new BasicNameValuePair("exclusionsonly", "10"));
		param.add(new BasicNameValuePair("colyears", "on"));
		param.add(new BasicNameValuePair("colseries", "on"));
		param.add(new BasicNameValuePair("colisttotal", "on"));
		param.add(new BasicNameValuePair("colistavg", "on"));
		param.add(new BasicNameValuePair("colpcchamp", "on"));
		param.add(new BasicNameValuePair("colpcevents", "on"));
		param.add(new BasicNameValuePair("colistqualify", "on"));
		param.add(new BasicNameValuePair("colpcqualify", "on"));
		param.add(new BasicNameValuePair("selection", "CSV"));

		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(httpclient.getParams(), "Scorekeeper/2.0");
		
        HttpPost request = new HttpPost(new URI("http", host, "/history/report", null));
        request.setEntity(new UrlEncodedFormEntity(param));
        FileOutputStream out = new FileOutputStream("sdfsdg");
        httpclient.execute(request).getEntity().writeTo(out);
	}
        
	
	/**
	 * Read in the history data from a csv file
	 * @param file the csv file to read from
	 * @return a map of lower case name to map of key/values
	 * @throws IOException
	 */
	public Map<String, Map<String,String>> readFile(File file) throws IOException
	{
        BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        Map<String, Map<String,String>> ret = new HashMap<String, Map<String,String>>();
        CSVParser parser = new CSVParser();
        
        try {
	    	String[] titles = parser.parseLine(buffer.readLine());
	        while (true) {
	        	// last first years series isttotal istavg pcchamp pcevents istqualify pcqualify
	        	String[] parts = parser.parseLine(buffer.readLine());
	        	if (parts == null) break;
	        	Map<String, String> object = new HashMap<String,String>();
	        	for (int ii = 0; ii < titles.length; ii++) {
	        		object.put(titles[ii], parts[ii]);
	        	}
	        	ret.put(object.remove("first") + " " + object.remove("last"), object);
	        }
        } finally {
        	buffer.close();
        }

        return ret;
	}	

	
	public static void main(String[] args)
	{
		try {
			new History();
		} catch (Throwable e) {
			log.log(Level.SEVERE, "History main failure: " + e, e);
		}
	}
}
