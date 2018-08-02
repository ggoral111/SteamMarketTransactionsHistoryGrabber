package model;

public class MarketListing {

	private String history_row_id, item_name, item_game_listing_name, item_price, item_action_type, item_whoactedwith_profile_name, item_whoactedwith_profile_url, item_date, asset_id, inspect_ingame;
	
	public MarketListing() {
		
	}

	public MarketListing(String history_row_id, String item_name, String item_game_listing_name, String item_price, String item_action_type, String item_whoactedwith_profile_name, String item_whoactedwith_profile_url, String item_date) {
		this.history_row_id = history_row_id;
		this.item_name = item_name;
		this.item_game_listing_name = item_game_listing_name;
		this.item_price = item_price;
		this.item_action_type = item_action_type;
		this.item_whoactedwith_profile_name = item_whoactedwith_profile_name;
		this.item_whoactedwith_profile_url = item_whoactedwith_profile_url;		
		this.item_date = item_date;
	}

	public String getItem_name() {
		return item_name;
	}

	public MarketListing setItem_name(String item_name) {
		this.item_name = item_name;
		return this;
	}

	public String getItem_game_listing_name() {
		return item_game_listing_name;
	}

	public MarketListing setItem_game_listing_name(String item_game_listing_name) {
		this.item_game_listing_name = item_game_listing_name;
		return this;
	}

	public String getItem_price() {
		return item_price;
	}

	public MarketListing setItem_price(String item_price) {
		this.item_price = item_price;
		return this;
	}

	public String getItem_action_type() {
		return item_action_type;
	}

	public MarketListing setItem_action_type(String item_action_type) {
		this.item_action_type = item_action_type;
		return this;
	}
	
	public String getItem_whoactedwith_profile_name() {
		return item_whoactedwith_profile_name;
	}

	public MarketListing setItem_whoactedwith_profile_name(String item_whoactedwith_profile_name) {
		this.item_whoactedwith_profile_name = item_whoactedwith_profile_name;
		return this;
	}

	public String getItem_whoactedwith_profile_url() {
		return item_whoactedwith_profile_url;
	}

	public MarketListing setItem_whoactedwith_profile_url(String item_whoactedwith_profile_url) {
		this.item_whoactedwith_profile_url = item_whoactedwith_profile_url;
		return this;
	}

	public String getItem_date() {
		return item_date;
	}

	public MarketListing setItem_date(String item_date) {
		this.item_date = item_date;
		return this;
	}
	
	public String getHistory_row_id() {
		return history_row_id;
	}

	public MarketListing setHistory_row_id(String history_row_id) {
		this.history_row_id = history_row_id;
		return this;
	}

	public String getAsset_id() {
		return asset_id;
	}

	public MarketListing setAsset_id(String asset_id) {
		this.asset_id = asset_id;
		return this;
	}

	public String getInspect_ingame() {
		return inspect_ingame;
	}

	public MarketListing setInspect_ingame(String inspect_ingame) {
		this.inspect_ingame = inspect_ingame;
		return this;
	}

	@Override
	public String toString() {
		return this.item_name + " | " + this.item_game_listing_name + " | " + this.item_price + " | " + this.item_action_type + " | " + this.item_whoactedwith_profile_name + " | " + this.item_whoactedwith_profile_url + " | " + this.item_date;	
	}
	
}
