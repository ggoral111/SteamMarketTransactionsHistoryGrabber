package service;

public class MarketListingsSearcherService {
	
	private static final int NUMBER_OF_THREADS;
	
	static {
		NUMBER_OF_THREADS = 16;
		// NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();;
	}
	
	public MarketListingsSearcherService() {
		
	}
	
	
}
