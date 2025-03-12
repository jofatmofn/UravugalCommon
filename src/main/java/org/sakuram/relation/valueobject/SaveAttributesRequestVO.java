package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveAttributesRequestVO {
	private long entityId;
	private List<AttributeValueVO> attributeValueVOList;
	private long creatorId;
	private Long sourceId;
	private byte[] photo;

	public SaveAttributesRequestVO(long entityId, List<AttributeValueVO> attributeValueVOList, Long sourceId) {
		this.entityId = entityId;
		this.attributeValueVOList = attributeValueVOList;
		this.sourceId = sourceId;
	}
}
