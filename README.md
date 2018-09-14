# Apache-Web-Servers-FailureRate UPDATED

What does this program do ?
  This program reads APACHE STYLE web server log files
  Its goal is to determine for each unique URL included in log data, its error ratio.
  This is done by aggregating over all instances of a particular URL, the proportion of those instances which resulted in       error - have an error code
  and produce a summary report sorted by date of all unique URLs in the log with their error ratio

What did I learn from this experience:

  1- Learned a ton about processing text data such as JSON , Text files, Log files for manipulating data.
  
  2- Learned a ton about mastering REGEX in JAVA
    I must say I totally warmed up to it. Applying its power was revealing. I did learn how to leverage Regex for complex text     matching and analysis.
    With regex I was to extract and compare precise text patterns in each record in the log file to conduct my analysis (see       various functions in the code)
    
  3- Learned a ton about mastering DATES in JAVA
     This sounds mondane, but it is extremely critical for data manipulation.
     This project forced me to experience with advanced DATE manipulation functions to match, analyze, compare and format          data as required by this project  (see various DATE functions)

Feel free to try it with the sample log file provided!
