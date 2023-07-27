package org.sakuram.relation.valueobject;

import java.util.List;

public class SaveAttributesResponseVO {
	private long entityId;	// Applicable for savePersonAttributes, during Person create
	private List<Long> insertedAttributeValueIdList;
	
	public long getEntityId() {
		return entityId;
	}
	
	public void setEntityId(long entityId) {
		this.entityId = entityId;
	}
	
	public List<Long> getInsertedAttributeValueIdList() {
		return insertedAttributeValueIdList;
	}
	
	public void setInsertedAttributeValueIdList(List<Long> insertedAttributeValueIdList) {
		this.insertedAttributeValueIdList = insertedAttributeValueIdList;
	}
	
}
