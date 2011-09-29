/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class of string functions.
 * 
 * @author James Moger
 * 
 */
public class StringUtils {

	public static final String MD5_TYPE = "MD5:";

	/**
	 * Returns true if the string is null or empty.
	 * 
	 * @param value
	 * @return true if string is null or empty
	 */
	public static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Replaces carriage returns and line feeds with html line breaks.
	 * 
	 * @param string
	 * @return plain text with html line breaks
	 */
	public static String breakLinesForHtml(String string) {
		return string.replace("\r\n", "<br/>").replace("\r", "<br/>").replace("\n", "<br/>");
	}

	/**
	 * Prepare text for html presentation. Replace sensitive characters with
	 * html entities.
	 * 
	 * @param inStr
	 * @param changeSpace
	 * @return plain text escaped for html
	 */
	public static String escapeForHtml(String inStr, boolean changeSpace) {
		StringBuffer retStr = new StringBuffer();
		int i = 0;
		while (i < inStr.length()) {
			if (inStr.charAt(i) == '&') {
				retStr.append("&amp;");
			} else if (inStr.charAt(i) == '<') {
				retStr.append("&lt;");
			} else if (inStr.charAt(i) == '>') {
				retStr.append("&gt;");
			} else if (inStr.charAt(i) == '\"') {
				retStr.append("&quot;");
			} else if (changeSpace && inStr.charAt(i) == ' ') {
				retStr.append("&nbsp;");
			} else if (changeSpace && inStr.charAt(i) == '\t') {
				retStr.append(" &nbsp; &nbsp;");
			} else {
				retStr.append(inStr.charAt(i));
			}
			i++;
		}
		return retStr.toString();
	}

	/**
	 * Decode html entities back into plain text characters.
	 * 
	 * @param inStr
	 * @return returns plain text from html
	 */
	public static String decodeFromHtml(String inStr) {
		return inStr.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&quot;", "\"").replace("&nbsp;", " ");
	}

	/**
	 * Encodes a url parameter by escaping troublesome characters.
	 * 
	 * @param inStr
	 * @return properly escaped url
	 */
	public static String encodeURL(String inStr) {
		StringBuffer retStr = new StringBuffer();
		int i = 0;
		while (i < inStr.length()) {
			if (inStr.charAt(i) == '/') {
				retStr.append("%2F");
			} else if (inStr.charAt(i) == ' ') {
				retStr.append("%20");
			} else {
				retStr.append(inStr.charAt(i));
			}
			i++;
		}
		return retStr.toString();
	}

	/**
	 * Flatten the list of strings into a single string with a space separator.
	 * 
	 * @param values
	 * @return flattened list
	 */
	public static String flattenStrings(List<String> values) {
		return flattenStrings(values, " ");
	}

	/**
	 * Flatten the list of strings into a single string with the specified
	 * separator.
	 * 
	 * @param values
	 * @param separator
	 * @return flattened list
	 */
	public static String flattenStrings(List<String> values, String separator) {
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			sb.append(value).append(separator);
		}
		return sb.toString().trim();
	}

	/**
	 * Returns a string trimmed to a maximum length with trailing ellipses. If
	 * the string length is shorter than the max, the original string is
	 * returned.
	 * 
	 * @param value
	 * @param max
	 * @return trimmed string
	 */
	public static String trimString(String value, int max) {
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max - 3) + "...";
	}

	/**
	 * Returns a trimmed shortlog message.
	 * 
	 * @param string
	 * @return trimmed shortlog message
	 */
	public static String trimShortLog(String string) {
		return trimString(string, 60);
	}

	/**
	 * Left pad a string with the specified character, if the string length is
	 * less than the specified length.
	 * 
	 * @param input
	 * @param length
	 * @param pad
	 * @return left-padded string
	 */
	public static String leftPad(String input, int length, char pad) {
		if (input.length() < length) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, len = length - input.length(); i < len; i++) {
				sb.append(pad);
			}
			sb.append(input);
			return sb.toString();
		}
		return input;
	}

	/**
	 * Right pad a string with the specified character, if the string length is
	 * less then the specified length.
	 * 
	 * @param input
	 * @param length
	 * @param pad
	 * @return right-padded string
	 */
	public static String rightPad(String input, int length, char pad) {
		if (input.length() < length) {
			StringBuilder sb = new StringBuilder();
			sb.append(input);
			for (int i = 0, len = length - input.length(); i < len; i++) {
				sb.append(pad);
			}
			return sb.toString();
		}
		return input;
	}

	/**
	 * Calculates the SHA1 of the string.
	 * 
	 * @param text
	 * @return sha1 of the string
	 */
	public static String getSHA1(String text) {
		try {
			byte[] bytes = text.getBytes("iso-8859-1");
			return getSHA1(bytes);
		} catch (UnsupportedEncodingException u) {
			throw new RuntimeException(u);
		}
	}

	/**
	 * Calculates the SHA1 of the byte array.
	 * 
	 * @param bytes
	 * @return sha1 of the byte array
	 */
	public static String getSHA1(byte[] bytes) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(bytes, 0, bytes.length);
			byte[] digest = md.digest();
			return toHex(digest);
		} catch (NoSuchAlgorithmException t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * Calculates the MD5 of the string.
	 * 
	 * @param string
	 * @return md5 of the string
	 */
	public static String getMD5(String string) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(string.getBytes("iso-8859-1"));
			byte[] digest = md.digest();
			return toHex(digest);
		} catch (UnsupportedEncodingException u) {
			throw new RuntimeException(u);
		} catch (NoSuchAlgorithmException t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * Returns the hex representation of the byte array.
	 * 
	 * @param bytes
	 * @return byte array as hex string
	 */
	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			if (((int) bytes[i] & 0xff) < 0x10) {
				sb.append('0');
			}
			sb.append(Long.toString((int) bytes[i] & 0xff, 16));
		}
		return sb.toString();
	}

	/**
	 * Returns the root path of the specified path. Returns a blank string if
	 * there is no root path.
	 * 
	 * @param path
	 * @return root path or blank
	 */
	public static String getRootPath(String path) {
		if (path.indexOf('/') > -1) {
			return path.substring(0, path.lastIndexOf('/'));
		}
		return "";
	}

	/**
	 * Returns the path remainder after subtracting the basePath from the
	 * fullPath.
	 * 
	 * @param basePath
	 * @param fullPath
	 * @return the relative path
	 */
	public static String getRelativePath(String basePath, String fullPath) {
		String relativePath = fullPath.substring(basePath.length()).replace('\\', '/');
		if (relativePath.charAt(0) == '/') {
			relativePath = relativePath.substring(1);
		}
		return relativePath;
	}

	/**
	 * Splits the space-separated string into a list of strings.
	 * 
	 * @param value
	 * @return list of strings
	 */
	public static List<String> getStringsFromValue(String value) {
		return getStringsFromValue(value, " ");
	}

	/**
	 * Splits the string into a list of string by the specified separator.
	 * 
	 * @param value
	 * @param separator
	 * @return list of strings
	 */
	public static List<String> getStringsFromValue(String value, String separator) {
		List<String> strings = new ArrayList<String>();
		try {
			String[] chunks = value.split(separator);
			for (String chunk : chunks) {
				chunk = chunk.trim();
				if (chunk.length() > 0) {
					strings.add(chunk);
				}
			}
		} catch (PatternSyntaxException e) {
			throw new RuntimeException(e);
		}
		return strings;
	}

	/**
	 * Validates that a name is composed of letters, digits, or limited other
	 * characters.
	 * 
	 * @param name
	 * @return the first invalid character found or null if string is acceptable
	 */
	public static Character findInvalidCharacter(String name) {
		char[] validChars = { '/', '.', '_', '-' };
		for (char c : name.toCharArray()) {
			if (!Character.isLetterOrDigit(c)) {
				boolean ok = false;
				for (char vc : validChars) {
					ok |= c == vc;
				}
				if (!ok) {
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Simple fuzzy string comparison. This is a case-insensitive check. A
	 * single wildcard * value is supported.
	 * 
	 * @param value
	 * @param pattern
	 * @return true if the value matches the pattern
	 */
	public static boolean fuzzyMatch(String value, String pattern) {
		if (value.equalsIgnoreCase(pattern)) {
			return true;
		}
		if (pattern.contains("*")) {
			boolean prefixMatches = false;
			boolean suffixMatches = false;

			int wildcard = pattern.indexOf('*');
			String prefix = pattern.substring(0, wildcard).toLowerCase();
			prefixMatches = value.toLowerCase().startsWith(prefix);

			if (pattern.length() > (wildcard + 1)) {
				String suffix = pattern.substring(wildcard + 1).toLowerCase();
				suffixMatches = value.toLowerCase().endsWith(suffix);
				return prefixMatches && suffixMatches;
			}
			return prefixMatches || suffixMatches;
		}
		return false;
	}
}
