package org.sakuram.relation.valueobject;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DomainValueVO {
	private long id;	
	private String category;	
	private String value;
	private String relationGroup;
	private String attributeInUi;
	private String repetitionType;
	private String attributeDomain;
	private Boolean isInputMandatory;
	private String validationJsRegEx;
	private String languageCode;
	private String privacyRestrictionType;
	private Boolean isScriptConvertible;
}
