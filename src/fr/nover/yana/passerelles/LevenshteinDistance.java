package fr.nover.yana.passerelles;

import android.annotation.SuppressLint;

public class LevenshteinDistance { 
	
	public static double similarity(String s1, String s2) { 
		if (s1.length() < s2.length()) { 
			// s1 should always be bigger 
			String swap = s1; 
			s1 = s2; 
			s2 = swap; } 
		int bigLen = s1.length(); 
		if (bigLen == 0) { 
			return 1.0; /* both strings are zero length */ } 
		return (bigLen - computeEditDistance(s1, s2)) / (double) bigLen; }
	
	@SuppressLint("DefaultLocale")
	public static int computeEditDistance(String s1, String s2) { 
		s1 = s1.toLowerCase(); 
		s2 = s2.toLowerCase(); 
		int[] costs = new int[s2.length() + 1]; 
		for (int i = 0; i <= s1.length(); i++) { 
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) { 
				if (i == 0) costs[j] = j; 
				else { 
					if (j > 0) { 
						int newValue = costs[j - 1]; 
						if (s1.charAt(i - 1) != s2.charAt(j - 1)) newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1; 
						costs[j - 1] = lastValue; lastValue = newValue; } } } 
			if (i > 0) costs[s2.length()] = lastValue; } 
		return costs[s2.length()]; } 
	
	public static void printDistance(String s1, String s2) { 
		System.out.println(s1 + "-->" + s2 + ": " + computeEditDistance(s1, s2) + " ("+similarity(s1, s2)+")"); } 
	
	public static void main(String[] args) { 
		printDistance("", ""); 
		printDistance("1234567890", "1"); 
		printDistance("1234567890", "12"); 
		printDistance("1234567890", "123"); 
		printDistance("1234567890", "1234"); 
		printDistance("1234567890", "12345"); 
		printDistance("1234567890", "123456"); 
		printDistance("1234567890", "1234567"); 
		printDistance("1234567890", "12345678"); 
		printDistance("1234567890", "123456789"); 
		printDistance("1234567890", "1234567890"); 
		printDistance("1234567890", "1234567980"); 
		printDistance("47/2010", "472010"); 
		printDistance("47/2010", "472011"); 
		printDistance("47/2010", "AB.CDEF"); 
		printDistance("47/2010", "4B.CDEFG"); 
		printDistance("47/2010", "AB.CDEFG"); 
		printDistance("The quick fox jumped", "The fox jumped"); 
		printDistance("The quick fox jumped", "The fox"); 
		printDistance("The quick fox jumped", "The quick fox jumped off the balcany"); 
		printDistance("kitten", "sitting"); 
		printDistance("rosettacode", "raisethysword"); 
		printDistance(new StringBuilder("rosettacode").reverse().toString(), new StringBuilder("raisethysword").reverse().toString()); 
		for (int i = 1; i < args.length; i += 2) { printDistance(args[i - 1], args[i]); } 
	}
}