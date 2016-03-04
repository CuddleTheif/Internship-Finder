import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Main {
	
	// Regex patterns for finding links and keywords
	private static Pattern linkPattern = Pattern.compile("<a .*?href=\"(.*?)\".*?>(.*?)</a>", Pattern.DOTALL | Pattern.MULTILINE),
							careerPattern = Pattern.compile("[^a-z]((career)|(job)|(position)|(team))(?:[^a-z]|s[^a-z])", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
							internPattern = Pattern.compile("[^a-z](((intern)|(internship))|(co(-|)op)|(student))(?:[^a-z]|s[^a-z])", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	// List used to store any links that through errors
	private static List<String> errorLinks = new LinkedList<>();
	
	// List used to store links for checking for duplicates
	private static List<String>	visitedLinks = new LinkedList<>();
	
	public static void main(String[] args) {

		// Keep getting websites to search until exit
		while(true){
			
			// Get the current website to search
				String website = JOptionPane.showInputDialog(null, "Enter Website to get links from to search for jobs", "Website Please", JOptionPane.QUESTION_MESSAGE);
			
			// If no website was given quit
				if(website==null)
					System.exit(0);
				
			// Ask for name of output files and if output should be links in HTML
				String file = JOptionPane.showInputDialog(null, "Output File Name:", "File", JOptionPane.QUESTION_MESSAGE);
				int format = JOptionPane.showConfirmDialog(null, "Format output for HTML?", "Format", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			
			// Search the given site for jobs and check if was succesful
				List<String> websites = getJobSites(website);
				if(websites==null){
					JOptionPane.showMessageDialog(null, "Error with given URL!");
					continue;
				}
			
			// Write found job sites to file
				writeListToFile(websites, file+" Jobs", format==JOptionPane.YES_OPTION);
			
			// Write any sites that threw an error to file
				writeListToFile(errorLinks, file+" Errors", format==JOptionPane.YES_OPTION);
			
			// Let the user know the process is done
				JOptionPane.showMessageDialog(null, "Done");
		}
	}
	
	/**
	 * Writes the given list to the given file name using HTML format or not depending on given value
	 * 
	 * @param list     List to write to file
	 * @param file     File to write to
	 * @param format   If should use HTML format
	 */
	private static void writeListToFile(List<String> list, String file, boolean format){
		
		// Create list for checking duplicates
			List<String> dupList = new LinkedList<>();
		
		// Write the list to file
			BufferedWriter fileWriter = null;
			try {
				// Create writer to file=
				    fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(file+" Jobs.txt"))));
				
				// Write all items in the list to the file
					for(String item : list)
					{
						// Check for duplicates
						if(!dupList.contains(item)){
							
							// Format for HTML if chosen and write to file
								if(format)
									fileWriter.write("<a href=\""+item+"\">"+item+"</a>");
								else
									fileWriter.write(item);
								fileWriter.newLine();
								dupList.add(item);
						}
					}
			} catch (Exception e) {
				// Print error to console (Shouldn't happen?)
					e.printStackTrace();
			} finally {
				// Make sure writer closes if opened
					if(fileWriter!=null){
						try {
							fileWriter.close();
						} catch (IOException e) {
							// Print error to console (Shouldn't happen?)
								e.printStackTrace();
						}
					}
			}
		
	}
	
	/**
	 * Gets all the pages that are linked to by the given website and returns the sites that reference internship keywords
	 * 
	 * @param website   The website to get pages to search
	 * @return			The pages that are refrenced by the given website that have inten keywords
	 */
	private static List<String> getJobSites(String website){
		
		// Get the website and matcher (If website not found return null)
			List<String> websites = new LinkedList<String>();
			String html = getHtml(website);
			if(html==null)
				return null;
			Matcher htmlMatcher = linkPattern.matcher(html);
		
		// Get the number of links
			int numLinks = 0;
			while (htmlMatcher.find())
				numLinks++;
			htmlMatcher.reset();

		//create the loading bar and JPanel for it
			final JPanel tempPanel = new JPanel();
			JProgressBar progressBar = new JProgressBar(0, numLinks);
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
			tempPanel.add(progressBar);
		
		//Open a message dialog in a thread with the loading bar that if closed, will stop the program.
			new Thread(new Runnable(){
			    public void run() {
			    	JOptionPane.showMessageDialog(null, tempPanel, "Loading", JOptionPane.PLAIN_MESSAGE);
			    	System.exit(0);
			    }
			}).start();

		// Search each site and search for pages with intern keywords
			while (htmlMatcher.find()){
				websites.addAll(getInternSites(htmlMatcher.group(1)));
				visitedLinks.clear();
				progressBar.setValue(progressBar.getValue()+1);
			}
		
		// Return all pages found with intern keywords
			return websites;
		
	}
	
	private static List<String> getInternSites(String website){
		
		// Get the website and matcher (If website not found return empty list)
			List<String> jobs = new LinkedList<String>();
			visitedLinks.add(website);
			String html = getHtml(website);
			if(html==null)
				return jobs;
			Matcher htmlMatcher = linkPattern.matcher(html);
		
		// Check links for career keywords to search more pages and search those
			while (htmlMatcher.find())
				if(careerPattern.matcher(htmlMatcher.group(2)).find() &&!visitedLinks.contains(htmlMatcher.group(1))){
					jobs.addAll(getInternSites(htmlMatcher.group(1)));
				}
		
		// Check this page for intern keywords and add it if so and then return the whole list
			if(internPattern.matcher(html).find())
				jobs.add(website);
				
		// Return all current sites found with intern keywords
			return jobs;
		
	}
	
	/**
	 * Gets the HTML of the given website as a String
	 * 
	 * @param url    The URL of the website to get the HTML of
	 * @return       The HTML of the website
	 */
	private static String getHtml(String url){
		
		// Get the html of the page
			String html;
			Scanner scanner = null;
			try {
				// Open connection to URL and trick it to thinking Java is firefox
					URLConnection connection =  new URL(url).openConnection();
					connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
			  
				// Create a scanner on the connection and get the html from that
				    scanner = new Scanner(connection.getInputStream());
					scanner.useDelimiter("\\Z");
					html = scanner.next();
			}catch ( Exception ex ) {
				// If the errored link is actually a link (i.e. starts with http) add it to the error list
					if(url.startsWith("http"))
						errorLinks.add(url);
					
				// Return null since couldn't get the html
				    return null;
			} finally {
				
				// Close the scanner just in case it is open
				if(scanner!=null)
					scanner.close();
				
			}
		
		
		// replace special characters and return the html
			html = html.replaceAll("&ndash;", "-");
			html = html.replaceAll("&mdash;", "—");
			html = html.replaceAll("&iexcl;", "¡");
			html = html.replaceAll("&iquest;", "¿");
			html = html.replaceAll("&quot;", "\"");
			html = html.replaceAll("&ldquo;", "“");
			html = html.replaceAll("&rdquo;", "”");
			html = html.replaceAll("&lsquo;", "‘");
			html = html.replaceAll("&rsquo;", "’");
			html = html.replaceAll("&raquo;", "«");
			html = html.replaceAll("&rdquo;", "»");
			html = html.replaceAll("&nbsp;", " ");
			html = html.replaceAll("&amp;", "&");
			html = html.replaceAll("&cent;", "¢");
			html = html.replaceAll("&copy;", "©");
			html = html.replaceAll("&divide;", "÷");
			html = html.replaceAll("&gt;", ">");
			html = html.replaceAll("&lt;", "<");
			html = html.replaceAll("&micro;", "µ");
			html = html.replaceAll("&middot;", "·");
			html = html.replaceAll("&para;", "¶");
			html = html.replaceAll("&plusmn;", "±");
			html = html.replaceAll("&euro;", "€");
			html = html.replaceAll("&pound;", "£");
			html = html.replaceAll("&reg;", "®");
			html = html.replaceAll("&sect;", "§");
			html = html.replaceAll("&trade;", "™");
			html = html.replaceAll("&yen;", "¥");
			html = html.replaceAll("&times;", "×");
			html = html.replaceAll("&hellip;", "…");
			html = html.replaceAll("â€œ", "“");
			html = html.replaceAll("â€", "”");
			return html;
	}

}
