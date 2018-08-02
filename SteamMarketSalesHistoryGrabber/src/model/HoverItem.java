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
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		
		if(obj == null || getClass() != obj.getClass()) {
			return false;
		}
		
		HoverItem hi = (HoverItem) obj;
		
		return this.history_row_id.equals(hi.getHistory_row_id()) && this.asset_id.equals(hi.getAsset_id());
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + (this.history_row_id == null ? 0 : this.history_row_id.hashCode());
		hash = 31 * hash + (this.asset_id == null ? 0 : this.asset_id.hashCode());
		
		return hash;
	}
	
}
