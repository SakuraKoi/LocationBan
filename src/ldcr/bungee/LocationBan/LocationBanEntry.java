package ldcr.bungee.LocationBan;

public class LocationBanEntry {
	private String keyword;
	private String reason;
	private String executor;
	public LocationBanEntry(final String keyword, final String reason, final String executor) {
		setKeyword(keyword);
		setReason(reason);
		setExecutor(executor);
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(final String keyword) {
		this.keyword = keyword;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(final String reason) {
		this.reason = reason;
	}
	public String getExecutor() {
		return executor;
	}
	public void setExecutor(final String executor) {
		this.executor = executor;
	}

	@Override
	public int hashCode() {
		return keyword.toLowerCase().hashCode();
	}
	@Override
	public boolean equals(final Object other) {
		if (other==null) return false;
		if (other instanceof String) return keyword.equalsIgnoreCase(other.toString());
		if (other instanceof LocationBanEntry) return keyword.equalsIgnoreCase(((LocationBanEntry)other).keyword);
		return false;
	}
}
