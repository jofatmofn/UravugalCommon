package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RetrievePersonAttributesResponseVO {
	byte[] photo;
	String label;
	List<AttributeValueVO> attributeValueVOList;

}
