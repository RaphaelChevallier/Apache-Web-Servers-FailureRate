import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Collections;
import java.text.ParseException;

public class ReadApacheLogs {
	public static void main(String[] args) throws IOException {

//===================================================================================================================
// What does this program do ?
// This program reads APACHE STYLE web server log files
// Its goal is to determine for each unique URL its error ratio
// and produce a summary report sorted by date of all unique URLs in the log with their error ratio
//
// What did I learn:
// A lot about understanding and mastering REGEX in JAVA to extract precise text patterns from a source (see various functions)
// A lot about understanding and mastering DATES in JAVA to understand date matching, formatting (see various functions)
//===================================================================================================================

		String linetext ="";
		String url ="";
		String urlstring ="";
		String errorCodeToMatch = "500";														//----- Change to whatever web server code you want to detect
		String errorCode ="";
		String dateString = "";
		String convertedDate ="";
		ArrayList<String> urlsList = new ArrayList<>();    	// ----- List of unique URLs
		int urlCount =0;																						//----- number of unique URLs captured
		ArrayList<String> urlDateList = new ArrayList<>();    	// ----- List of date strings for these URLs
		Date d1, d2;
		ArrayList<Integer> urlTotalCountsList = new ArrayList<>(); // ---- List of count of occurences of each URL
		float urlTotalCount;
		ArrayList<Integer>  urlErrorCountsList = new ArrayList<>(); // ------- List of count of error occurences of each URL
		float urlErrorCount;
		float errorRatio;

/* =======================================TEST DATA ============================================================================
60.60.60.60 - - [26/Sep/2016:02:10:00 +0000] "POST /1.1/wwwwwwwwww11111/list.json?user_id=123 HTTP/1.1" 500 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
10.10.10.10 - - [26/Sep/2016:05:20:00 +0000] "GET /1.3/friendships2222/list.json?user_id=123 HTTP/1.1" 500 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
20.20.20.20 - - [27/Sep/2016:05:22:08 +0000] "GET /1.3/friendships2222/list.json?user_id=123 HTTP/1.1" 200 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
20.20.20.20 - - [27/Sep/2016:05:22:31 +0000] "POST /1.3/friendships2222/list.json?user_id=123 HTTP/1.1" 500 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
30.30.30.30 - - [27/Sep/2016:05:22:59 +0000] "POST /1.2/fakenewsomg3333/list.json?user_id=123 HTTP/1.1" 200 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
40.40.40.40 - - [27/Sep/2016:05:23:01 +0000] "POST /1.1/libertylove4444/list.json?user_id=123 HTTP/1.1" 500 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
50.50.50.50 - - [28/Sep/2016:22:45:33 +0000] "GET /1.1/libertylove4444/list.json?user_id=123 HTTP/1.1" 500 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
70.70.70.70 - - [29/Sep/2016:04:30:18 +0000] "POST /1.1/yyyyyyyyyyyyyy5555/list.json?user_id=123 HTTP/1.1" 200 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
80.80.80.80 - - [30/Sep/2016:07:55:18 +0000] "POST /1.1/qqqqqqqqqq6666/list.json?user_id=123 HTTP/1.1" 200 563 19 "Twitter-iPhone/6.63 iOS/10.0.2 (Apple;iPhone7,2;;;;;1)" 177.177.177.177
  =======================================TEST DATA ============================================================================
*/
		// -------------------------------------- START OF MAIN PROGRAM  ------------------------

		// ------ CAPTURE LOG DATA  from stdin BUT could do this from file as well
		String filename = "SampleFile_ApacheLog.txt";	//or any file you may want to try yourself
		System.out.println("Please update the ApacheFile.rtf with your own Apache web server logs if you have other logs to search performance for");
		//Scanner input = new Scanner(System.in);
		
		String input = null;
		
		FileReader filereader = new FileReader(filename);
		
		BufferedReader bufferedReader = new BufferedReader(filereader);
		

		// ----- LOOP FOR EACH LINE IN THE LOG ------------------------
		while ((input = bufferedReader.readLine()) != null){
			linetext = input;
			// ---- Extract all relevant data from each line in the Log
			urlstring = extractRegexStringFromLine(linetext);				// --- Extract URL and Error Code string
			url = extractURLFromRegexString(urlstring);							// --- Extract URL only
			errorCode = extractErrorCodeFromRegexString(urlstring);	// --- Extract Error code
			dateString = extractDateStringFromLine(linetext);				// --- Extract Date string
			convertedDate = convertDatetoGMT(dateString);						// --- Convert date string to GMT

			int index =0;
			//
			// ---- Check URL is a KNOWN one in list of urls
			if (urlsList.contains(url)){
				int indexOfURL = urlsList.indexOf(url); 																		// --- if so retrieve the position/index of this url in the list
				urlTotalCountsList.set(indexOfURL, (urlTotalCountsList.get(indexOfURL))+1); // and increment it by 1
				if (errorCode.equals(errorCodeToMatch)){ 																		// --- update the count of error for this url
					int errorCount = urlErrorCountsList.get(indexOfURL); 											// --- at this position retrieve the current count for url
					errorCount++; 																														// ------ increment it by one
					urlErrorCountsList.set(indexOfURL,errorCount);
				}
			}
			//

			else { // ---- ELSE this is a NEW URL. It needs to be inserted into the list of urls by date
				// ---- Logic to maintain sorted lists (sorted by url date) to make sure to insert a new URL values at the right index location in each list
				if (!urlDateList.isEmpty()) {
					// Essentially if the new url is not the first one in the list then . . .
					d1 = returnDateObjectFromString(convertedDate);
					d2 = returnDateObjectFromString(urlDateList.get(urlDateList.size()-1));
					// let's compare the date of the new url with last date element (the biggest)
					if(d1.compareTo(d2) > 0)  {
						index = urlsList.size(); // . . . if the new date is bigger then set the index to the very end to the lists to insert there
					}
					else {
						// let's compare the date of the new url with first date element (the smallest date)
						d2 = returnDateObjectFromString(urlDateList.get(0));
						if(d1.compareTo(d2) < 0)  {
							index = 0; // if the new date is smaller then set the index to the very beginning of the lists to insert there
						}
						else{
							// ---- The date is in the middle of the list. Need to find where to insert it to respect the chronological order
							for(index =1; index < urlDateList.size(); index++){
								d2 = returnDateObjectFromString(urlDateList.get(index));
								if(d1.compareTo(d2) <= 0)  {
									break; // We found the right index to insert at. Can break and continue
								}
							}
						}
					}
				}
				// -------- Now we know where (index) to store the data of the new url in the lists. let's do it....
				// System.out.println("** Inserting URL "+url+ " at index= "+index); 	// ---- DEBUG
				urlsList.add(index, url);									//add new URL to list of urls
				urlTotalCountsList.add(index, 1);					// Store  Total Count of of occurences for this new url- initiate to 1
				urlDateList.add(index, convertedDate);		// Store the Date of the new url in the list of dates
				if (errorCode.equals(errorCodeToMatch)){	// Check the error code for this new url
					urlErrorCountsList.add(index,1);				// Set it to 1 if this is an error ocurence of this url
				}
				else {
					urlErrorCountsList.add(index,0);				// else set it to 0 if this is a regular occurence of this url
				}
			} //--------END OF STORING this URL in an array of URLs, store its count of occurences in an array and store its count of errors in an array
		}

		// -- END OF LOOP OVER ALL LINES IN LOG ---
		//
		// -- TIME TO PRINT RESULTS
		System.out.println("- - - - - - - - - - - - PRINT RESULTS - - - - - - - - - -");
		// System.out.println("urlsList.size()       = " + urlsList.size()); 			// ---- DEBUG
		// System.out.println("urlsList              = " + urlsList);							// ---- DEBUG
		// System.out.println("urlDateList           = " + urlDateList);						// ---- DEBUG
		// System.out.println("urlTotalCountsList    = " + urlTotalCountsList);		// ---- DEBUG
		// System.out.println("urlErrorCountsList    = " + urlErrorCountsList);		// ---- DEBUG
		for (int i=0; i < urlsList.size() - 1; i++) {// while there are URLS  in the list of URLs
			url = urlsList.get(i);
			urlErrorCount = urlErrorCountsList.get(i);
			urlTotalCount = urlTotalCountsList.get(i);
			convertedDate =urlDateList.get(i);
			errorRatio = ((urlTotalCount-urlErrorCount)/urlTotalCount)*100;
			System.out.printf("%s %s %.2f\n",convertedDate, url, errorRatio);  // ---*** WHAT WAS REQUESTED **** ----
		}
		// - - - - - - - - - - - - END OF PROGRAM - - - - - - - - - - - - -
	}


	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// FUNCTIONS
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String extractRegexStringFromLine(String linetext){
		String urlstring;
		String regex = "(\"(GET|POST)([a-zA-Z0-9?=_\\s\\.\\/-]+)\")\\s[0-9]{3}";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(linetext);
		if (m.find()){ // ----- There is a URI in the line of text ---------
			urlstring = m.group();
		}
		else {return "***There is no URL on this line of data !!";}
		return urlstring;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	public static String extractErrorCodeFromRegexString(String urlstring){
		return urlstring.substring(urlstring.length()-3);
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	public static String extractURLFromRegexString(String urlstring){
		String url="";
		if (urlstring.contains("GET ")){
			url = urlstring.substring(5,urlstring.length()-5); 	// ------- This removes the first part of the string with GET . . . AND the end starting from ? mark sign
			url = url.substring(0,url.indexOf("?"));
		}
		else {
			if (urlstring.contains("POST ")){ 									// ------- This removes the first part of the string with POST . . . AND the end starting from ? mark sign
			url = urlstring.substring(6,urlstring.length()-5);
			url = url.substring(0,url.indexOf("?"));
			}
		}
		return url;
	}

	// - - - - - - - - -
	public static String extractDateStringFromLine(String linetext){
		String datestring;
		String regex = "\\[([\\w:\\/]+\\s[+\\-]\\d{4})\\]";    //---- This is the date format of the Apache logs ------
		Pattern p = Pattern.compile(regex);		//---- Prepare REGEX matching
		Matcher m = p.matcher(linetext);    	//---- Run REGX matching
		if (m.find()){ // ----- Is there a URI in the line of text ? ---------
			datestring = m.group();
		}
		else {
			//System.err.println("Bad log entry NO DATE IN IT!:");
			return "******** DONE *******";
		}
		datestring = datestring.substring(1, datestring.length()-1); // ---- removeing the brackets around the date
		return datestring;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	public static String convertDatetoGMT(String datestring){
		Date date = new Date();
		String dateFormatted ="";
		DateFormat dformatOrigin = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ssZ");
		DateFormat dformatFinal    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		// datestring = "27/Oct/2000:09:27:09 -0400";  //----- TO USE FOR TEST ONLY ----- DEBUG
		try{
			date = dformatOrigin.parse(datestring);
			dateFormatted = dformatFinal.format(date);
		}
		catch ( Exception ex){
			//System.out.println(ex);
		}
		return dateFormatted;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	public static Date returnDateObjectFromString(String datestring){
		Date date = new Date();
		String dateFormatted ="";
		DateFormat dformatFinal    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		try{
			date = dformatFinal.parse(datestring);
		}
		catch ( Exception ex){
			//System.out.println(ex);
		}
		return date;
	}
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
