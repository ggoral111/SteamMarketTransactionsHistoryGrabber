package model;

public interface ConsoleLineSeparator {

	default String produceSeparator(String separatorCharacter, int separatorLength) {
		StringBuilder sb = new StringBuilder(separatorCharacter.length() * separatorLength);
		
		for(int i = 0; i < separatorLength; i++) {
			sb.append(separatorCharacter);
		}
		
		return sb.toString();
	}
}
