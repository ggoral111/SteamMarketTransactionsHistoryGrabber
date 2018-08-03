package model;

public class ConvertedResponse {

	private String convertedResponse;
	private int transactionsListSize;
	
	public ConvertedResponse(String convertedResponse, int transactionsListSize) {
		this.convertedResponse = convertedResponse;
		this.transactionsListSize = transactionsListSize;
	}

	public String getConvertedResponse() {
		return convertedResponse;
	}

	public int getTransactionsListSize() {
		return transactionsListSize;
	}
	
}
