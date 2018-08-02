package model;

public class HoverItem {

	private String history_row_id, asset_id;

	public HoverItem(String history_row_id, String asset_id) {
		this.history_row_id = history_row_id;
		this.asset_id = asset_id;
	}

	public String getHistory_row_id() {
		return history_row_id;
	}

	public HoverItem setHistory_row_id(String history_row_id) {
		this.history_row_id = history_row_id;
		return this;
	}

	public String getAsset_id() {
		return asset_id;
	}

	public HoverItem setAsset_id(String asset_id) {
		this.asset_id = asset_id;
		return this;
	}
	
}
