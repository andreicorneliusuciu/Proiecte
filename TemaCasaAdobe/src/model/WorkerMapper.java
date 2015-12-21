package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.SnowballStemmer;

import constants.Constants;

public class WorkerMapper implements Runnable {

	private File inputFile;
	private long offset;
	private int charCount;
	/*Don't need the File in this stage. The word and its occurences are enough*/
	private HashMap<String, Integer> localInvertedIndexForThread;
	private ArrayList<HashMap<String, Integer>> partialInvertedIndicesForAFile;
	private String lang = "english";
	
	public WorkerMapper(File inputFile, long offset, int charCount,
			ArrayList<HashMap<String, Integer>> partialInvertedIndicesForAFile) {
		this.inputFile = inputFile;
		this.offset = offset;
		this.charCount = charCount;
		this.partialInvertedIndicesForAFile = partialInvertedIndicesForAFile;
	}

	
	/* The worker has a file, a offset and an amount to read from the file,
	 * starting from the specified offset. It doesn't read the first word if it
	 * doesn't start at the specified offset, but rather skips it. If ( offset +
	 * bytesRead ) hits in the middle of a word, it will continue reading until
	 * the first delimiter is met.
	 */
	@Override
	public void run() {
		
		try {
			InputStreamReader reader = new InputStreamReader(new FileInputStream(inputFile), "UTF8");
			
			char[] buffer = new char[charCount + Constants.PROBABLY_THE_LARGEST_OFFSET_POSSIBLE + 2];

			int startAt = 0;
			int endAt = charCount - 1;
			
			/* Read the required sequence, and mark the start of the parsable sequence*/
			if(offset == 0) {
				/* If this thread reads from the beginning of the file, it starts with offset 0*/
				reader.read(buffer, 0, charCount + Constants.PROBABLY_THE_LARGEST_OFFSET_POSSIBLE);
			} else {
				/* Otherwise, starting from a (offset-1), read until a delimiter is found*/
				offset--;
				charCount++;
				reader.skip(offset);
				reader.read(buffer, 0, charCount + Constants.PROBABLY_THE_LARGEST_OFFSET_POSSIBLE);
				while(!Constants.DELIMITERS.contains( buffer[startAt] + "" ))
					startAt++;
				
				/* Then skip the delimiter*/
				startAt++;
			}

			reader.close();
			
			/* Mark the end of the parsable sequence*/
			while(!Constants.DELIMITERS.contains( buffer[endAt] + "" ))
				endAt++; // It would get pretty nasty if this overflows...
			
			/* At this point, I have the parsable sequence in buffer[ startAt : endAt ]*/
			StringTokenizer st = new StringTokenizer(
					new String(buffer).substring(startAt, endAt + 1), Constants.DELIMITERS);
			
			localInvertedIndexForThread = new HashMap<String, Integer>();
			
			/*The stemmer I used : http://snowball.tartarus.org/download.php*/
			Class stemClass = Class.forName("org.tartarus.snowball.ext." +
					lang + "Stemmer");
			SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();

			while(st.hasMoreElements()) {
				String word = st.nextToken().toLowerCase();
				
				if(lang.equals("english") && Constants.STOP_WORDS_ENGLISH.contains(word)) {
					continue;
				}
				
				if(lang.equals("romanian") && Constants.STOP_WORDS_ROMANIAN.contains(word)) {
					continue;
				}
	
				stemmer.setCurrent(word);
				stemmer.stem();
				String stemmedWord = stemmer.getCurrent();  
					
				if(localInvertedIndexForThread.containsKey(stemmedWord)) {
					localInvertedIndexForThread.put(stemmedWord, localInvertedIndexForThread.get(stemmedWord) + 1);
				} else {
					localInvertedIndexForThread.put(stemmedWord, 1);
				}
			}
			
			/*Add the partial HashMap computed by this thread to the list of partial HashMaps computed for a file*/
			synchronized (partialInvertedIndicesForAFile) {
				partialInvertedIndicesForAFile.add(localInvertedIndexForThread);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
