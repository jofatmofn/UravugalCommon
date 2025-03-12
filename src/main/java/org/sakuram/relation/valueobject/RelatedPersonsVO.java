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
	private String excludePersonIdCsv;
	
	public RelatedPersonsVO(long person1Id, long person2Id, String person1ForPerson2, Long sourceId) {
		this.person1Id = person1Id;
		this.person2Id = person2Id;
		this.person1ForPerson2 = person1ForPerson2;
		this.sourceId = sourceId;
	}
}
