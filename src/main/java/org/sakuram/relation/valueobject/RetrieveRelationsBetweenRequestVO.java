package org.sakuram.relation.valueobject;

import java.util.List;

public class RetrieveRelationsBetweenRequestVO {
	private long end1PersonId;
	private List<Long> end2PersonIdsList;
	
	public long getEnd1PersonId() {
		return end1PersonId;
	}
	
	public void setEnd1PersonId(long end1PersonId) {
		this.end1PersonId = end1PersonId;
	}
	
	public List<Long> getEnd2PersonIdsList() {
		return end2PersonIdsList;
	}
	
	public void setEnd2PersonIdsList(List<Long> end2PersonIdsList) {
		this.end2PersonIdsList = end2PersonIdsList;
	}
	
}
