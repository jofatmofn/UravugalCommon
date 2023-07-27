package org.sakuram.relation.valueobject;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RelatedPersonsVO {
	private long person1Id;
	private long person2Id;
	private String person1ForPerson2;
	private long creatorId;
	private Long sourceId;
	private String excludeRelationIdCsv;
	
}
