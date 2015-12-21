package utility;

import java.util.Comparator;
import java.util.Map;

public class ComparatorForTreeMap implements Comparator{
	
	Map<String, Integer> map;

	public ComparatorForTreeMap(Map<String, Integer> map) {
	    this.map = map;
	}

	public int compare(Object o1, Object o2) {
		int returnValue = ((Integer) map.get(o2)).compareTo((Integer) map.get(o1));
		String key1 = new String((String) o1);
		String key2 = new String((String) o2);
		if (returnValue == 0) {
			return key1.compareTo(key2);
		} else return returnValue;
		
	}
}

