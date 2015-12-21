package model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerReducer implements Runnable {

	private File inputFile;
	/*word=occurences*/
	private ArrayList<HashMap<String, Integer>> partialInvertedIndicesForAFile;
	/*word=<file, occurences>*/
	private ConcurrentHashMap<String, HashMap<String, Integer>> invertedIndex;
	
	
	public WorkerReducer(File inputFile,
			ArrayList<HashMap<String, Integer>> partialInvertedIndicesForAFile,
			ConcurrentHashMap<String, HashMap<String, Integer>> invertedIndex) {
		
		this.inputFile = inputFile;
		this.partialInvertedIndicesForAFile = partialInvertedIndicesForAFile;
		this.invertedIndex = invertedIndex;
	}

	@Override
	public void run() {
		
		HashMap<String, Integer> resultInvertedIndexForAFile = new HashMap<>();
		
		/*Now I merge all the partial Inverted Indices for a file into one invertedIndex for a file.
		 * The resulted inverted index will be added to the global inverted index.
		 * Also this is done in parallel. Every thread will merge all the partial inverted indices for a 
		 * file and will put the final result in the global inverted index only once to reduce overhead*/
		
		for(HashMap<String, Integer> hm: this.partialInvertedIndicesForAFile) {
			for(String key: hm.keySet()) {
				if(resultInvertedIndexForAFile.containsKey(key)) {
					resultInvertedIndexForAFile.put(key, resultInvertedIndexForAFile.get(key) + hm.get(key));
				} else {
					resultInvertedIndexForAFile.put(key, hm.get(key));
				}
			}
		}
		
		/*Now I add the partial inverted index resulted for File that the executor service assigned to
		 * this current thread to the global inverted index*/
		
		for(String key : resultInvertedIndexForAFile.keySet()){

			if(invertedIndex.containsKey(key)) {
				invertedIndex.get(key).put(this.inputFile.getName(), resultInvertedIndexForAFile.get(key));
			} else {
				HashMap<String, Integer> aux = new HashMap<>();
				aux.put(this.inputFile.getName(), resultInvertedIndexForAFile.get(key));
				invertedIndex.put(key, aux);
			}			
		}		
	}
}
