package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class PersonSearchCriteriaVO {
	boolean isLenient;
	List<AttributeValueVO> attributeValueVOList;
}
