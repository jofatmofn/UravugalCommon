package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveAttributesResponseVO {
	private long entityId;	// Applicable for savePersonAttributes, during Person create
	private List<Long> insertedAttributeValueIdList;
	
}
