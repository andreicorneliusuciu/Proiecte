package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tartarus.snowball.SnowballStemmer;

import model.WorkerMapper;
import model.WorkerReducer;
import utility.ComparatorForTreeMap;

public class MainController {
	
	private static File[] filesInFolder;
	private static int availableThreadsNumber;
	private static int fragmentSize;
	private static ConcurrentHashMap<String, HashMap<String, Integer>> invertedIndex = new ConcurrentHashMap<>();
	

	public static void main(String[] args) throws IOException {
		
		File folder = null;
		String pathToDocuments = null;
		String lang = "english";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter file path where documents are located:");
        while(true) {
        	pathToDocuments = br.readLine();
        	folder = new File(pathToDocuments);
        	if(folder.exists())
        		break;
        	else System.out.println("Please, add a valid path");
        }
        /*Index just the .txt files*/
        File[] files = folder.listFiles();
        ArrayList<File> f = new ArrayList<>();
        for(int i = 0; i < files.length; i++) {
        	if(files[i].getName().endsWith(".txt")) {
        		f.add(files[i]);
        	}
        }
        
        filesInFolder = new File[f.size()];
        for(int i = 0; i < f.size(); i++) {
        	filesInFolder[i] = f.get(i);
        }
        int numberOfFiles = filesInFolder.length;
        
        /**
         * MAP
         */
        
        availableThreadsNumber = Runtime.getRuntime().availableProcessors();
        
        ExecutorService map = Executors.newFixedThreadPool(availableThreadsNumber);
        
		ArrayList<ArrayList<HashMap<String, Integer>>> partialResultsList = 
				new ArrayList<>();
		
		/*For every file, every worker thread will parse fragmentSize characters untill the end of the file
		 * fragmentSize is computed dynamically for every file. If I have a file with length less than 5000
		 * characters, I will read the entire file with one thread. Else every worker thread will read 5000
		 * characters untill the end of the file is reached.*/
		for(int fileIndex = 0; fileIndex < numberOfFiles; fileIndex++) {
			
			System.out.println("Indexing " + filesInFolder[fileIndex].getName());
			
			if(filesInFolder[fileIndex].length() >= 5000) {
				fragmentSize = 5000;
			} else fragmentSize = (int) filesInFolder[fileIndex].length();

			partialResultsList.add(new ArrayList<HashMap<String, Integer>>());
			long fileSize = filesInFolder[fileIndex].length();
			
			/*Make sure that I do not have an empty file*/
			if(fragmentSize != 0) {
				long fragments = fileSize / fragmentSize;
				for(int i = 0 ; i < fragments ; i++) {
					/*File to be parsed, offset where the thread start reading, the amount to read and the result*/
					map.execute(new WorkerMapper(filesInFolder[fileIndex], fragmentSize * i,
						fragmentSize, partialResultsList.get(fileIndex)));
				}
			}
		}
		
		map.shutdown();
		while(!map.isTerminated());
		
		
		/**
		 * REDUCE
		 */
		
		ExecutorService reduce = Executors.newFixedThreadPool(availableThreadsNumber);
		/*Every reduce thread will parse a partial inverted indices list at a time in parallel for a file
		 * and put the result into the global inverted index, the one in which the search will take place. */
		for(int fileIndex = 0; fileIndex < numberOfFiles; fileIndex++) {
			reduce.execute(new WorkerReducer(filesInFolder[fileIndex], 
					partialResultsList.get(fileIndex), invertedIndex));
		}
		
		reduce.shutdown();
		while(!reduce.isTerminated());
		
		/**
		 * SORT THE INVERTED INDEX BY OCCURENCES IN DOCS
		 */
		
		System.out.println("Indexing process finished");
		System.out.println();
		
		Class stemClass = null;
		SnowballStemmer stemmer = null;
			
		try {
			stemClass = Class.forName("org.tartarus.snowball.ext." +
					lang + "Stemmer");
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
			
		try {
			stemmer = (SnowballStemmer) stemClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
        while(true) {
        	System.out.println();
        	System.out.println("Enter search query:");
        	String s = br.readLine();
        	String[] searchQueryBeforeStem = s.split("\\s+");
        	
        	ArrayList<HashMap<String, Integer>> searchResult = new ArrayList<>();
        	HashMap<String, Integer> finalSearchResult = new HashMap<>();
        	HashMap<String, Integer> firstResult = new HashMap<>();
        	boolean flag = true;
        	int min = Integer.MAX_VALUE;
        	String finalStemmedWord = null;

        	/*Stem the word/words that we want use for search*/
        	for(int i = 0; i < searchQueryBeforeStem.length; i++) {

	        	stemmer.setCurrent(searchQueryBeforeStem[i].toLowerCase());
	    		stemmer.stem();
	    		finalStemmedWord = stemmer.getCurrent();

    			/*Here I get a list of inverted indices for every word in the query. Then I will
    			 * combine the results in the list in order to find the document(s) that contains
    			 * all the words in the query*/
    			if(invertedIndex.get(finalStemmedWord) != null) {
    				searchResult.add(invertedIndex.get(finalStemmedWord));  
    			} 
        	}
        	
        	if(searchResult.size() != searchQueryBeforeStem.length) {
        		/*If one word from the query is not foud, the query is not present in any file*/
        		System.out.println("Query not found in any file");
        		/*Go for a new input*/
				continue;
        	} else firstResult = searchResult.get(0);
        	
        	/*Only one word to search for, I do not have to combine anything. I will print the result*/
        	if(searchResult.size() == 1) {
        		finalSearchResult = firstResult;
        	} else {
	        	/*Check if all partial inverted indices resulted after the query(multiple words), have
	        	 * the same document(s)*/
        		Set<String> keys = firstResult.keySet();
        		for(int i = 1; i < searchResult.size(); i++) {
        			/*If any two random partial inverted indices does not match, break beacuse the
        			 * query can not be found*/
        			if(flag == false) { 
        				break;
        			}
        			
        			flag = false;
        			
        			for(String key: keys) {
        				if(searchResult.get(i).containsKey(key)) {
        					min = Math.min(searchResult.get(i).get(key), firstResult.get(key));
        					finalSearchResult.put(key, min);
        					flag = true;
        				}
        			}
        		}
        	}
        		
            if(flag) {
            	/*ComparatorForTreeMap is used to store the results in the correct order(the 
            	 * compare criteria is first after the number occurences in a file[as requested
            	 * in the homework] and then alphabetically)*/
            	ComparatorForTreeMap tm = new ComparatorForTreeMap(finalSearchResult);
                Map<String,Integer> finalMap = new TreeMap<String, Integer>(tm);
                finalMap.putAll(finalSearchResult);
                
                /*Print the result of the search*/
                System.out.println("Your query matches the following documents, presented in a ranked order:");
                System.out.println();
                
                for(String key: finalMap.keySet()) {
                	System.out.println(pathToDocuments + "\\" + key + " - " + finalMap.get(key) + " occ");
                }
        	} else System.out.println("Query not found in any file");
        }
	}
}
