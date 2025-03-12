package org.sakuram.relation.valueobject;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveOtherRelationRequestVO {
	private long otherRelationTypeDvId;
	private long person1Id;
	private long otherRelationVia1DvId;
	private long person2Id;
	private long otherRelationVia2DvId;
	private Long sourceId;
}
