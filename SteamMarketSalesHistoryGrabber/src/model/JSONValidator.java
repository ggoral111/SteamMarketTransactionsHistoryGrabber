package model;

/**
 * Simple interface which contains methods signatures of validating {@link java.lang.String} type variable in JSON format.
 * 
 * @author Jakub Podg�rski
 *
 */
public interface JSONValidator {
	
	/**
	 * This method formulates the check of String value obtained from web source which should contains 'success' object and be proper string of characters in JSON format.
	 * 
	 * @param jsonString the string of characters to check.
	 * @return true if given string of characters contains 'success' object set to true, false otherwise.
	 */
	boolean isValidJSON(String jsonString);
	
	/**
	 * This method formulates the check of String value which should be one of the admissible types in JSON format (JSONArray, JSONObject).
	 * 
	 * @param jsonString the string of characters to check.
	 * @return true if given string of characters is one of the JSON types, false otherwise.
	 */
	boolean isJSON(String jsonString);
}
