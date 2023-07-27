package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RetrieveRelationAttributesResponseVO {
	Long person1GenderDVId;
	Long person2GenderDVId;
	List<AttributeValueVO> attributeValueVOList;
	Long person1Id;
	String relationGroup;
	
}
