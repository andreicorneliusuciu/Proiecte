package constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.ls.LSInput;

public final class Constants {
	public static String eol = System.getProperty("line.separator");
	public static final String DELIMITERS = new String(";:/?~\\.,><~`[]{}()!@#$%^&-+'=*\"| \t\n\0"+eol); 
	public static final int PROBABLY_THE_LARGEST_OFFSET_POSSIBLE = 35;
	public static final String[] STOP_WORDS_EN = {"a", "an", "and", "are", "as", "at", "be", "but",
	                                            "by", "for", "if", "in", "into", "is", "it", "no",
	                                            "not", "of", "on", "or", "such", "that", "the",
	                                            "their", "then", "there", "they", "this", "to",
	                                            "was", "will", "with"};
	
	public static final String[] STOP_WORDS_RO = {"acea", "aceasta", "această", "aceea", "aceea", "aceea",
			"aceea", "aceea", "acei", "aceia", "acel" , "acela", "acele", "acelea", "acest", "acesta", 
			"aceste", "acestea", "aceşti", "aceştia", "acolo", 
			"acum", "ai", "aia", "aibă", "aici", "al", "ăla", "ale", "alea", "ălea", "altceva", "altcineva", 
			"am", "ar", "are", "aş", "aşadar", "asemenea", "asta", "ăsta", 
			"astăzi", "astea", "ăstea", "ăştia", "asupra", "aţi", "au", "avea", "avem", "aveţi", "azi", 
			"bine", "bucur", "bună", "ca", "că", "căci", "când", "care", "cărei", 
			"căror", "cărui", "cât", "câte", "câţi", "către", "câtva", "ce", "cel", "ceva",
			"chiar", "cînd", "cine", "cineva", "cît", "cîte", "cîţi", "cîtva", "contra", "cu", 
			"cum", "cumva", "curând", "curînd", "da", "dă", "dacă", "dar", "datorită", "de", "deci", 
			"deja", "deoarece", "departe", "deşi", "din", "dinaintea", "dintr", "dintre", "drept", "după",
			"ea", "ei", "el", "ele", 
			"eram", "este", "eşti", "eu", "face", "fără", "fi", "fie", "fiecare", "fii", "fim", "fiţi",
			"iar", "ieri", "îi", "îl", "îmi", "împotriva", "în", "înainte", "înaintea", "încât", "încît", "încotro", 
			"între", "întrucât", "îţi", "la", "lângă", "le", "li", "lîngă", "lor", "lui", "mă", "mâine", "mea",
			"mei", "mele", "mereu", "meu", "mi", "mine", "mult", "multă", "mulţi", "ne", "nicăieri", 
			"nici", "nimeni", "nişte", "noastră", "noastre", "noi", "noştri", "nostru", "nu", "ori", "oricând",
			"oricare", "oricât", "orice", "oricînd", "oricine", "oricît", "oricum", "oriunde", "până", "pe",
			"pentru", "peste", "pînă", 
			"poate", "pot", "prea", "prima", "primul", "prin", "printr", "sa", "să", "săi", "sale", "sau", "său",
			"se", "şi", "sînt", "sîntem", "sînteţi", "spre", "sub", "sunt", "suntem", "sunteţi", "ta", 
			"tăi", "tale", "tău", "te", "ţi", "ţie", "tine", "toată", "toate", "tot", "toţi", "totuşi", "tu", "un", 
			"una", "unde", "unei", "undeva", "unele", "uneori", "unor", "vă", "vi", "voastră", 
			"voastre", "voi", "voştri", "vostru", "vouă", "vreo", "vreun"	
	};

	public static final List<String> STOP_WORDS_ENGLISH = Arrays.asList(Constants.STOP_WORDS_EN);
	public static final List<String> STOP_WORDS_ROMANIAN = Arrays.asList(Constants.STOP_WORDS_RO);
}
