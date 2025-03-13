package org.sakuram.relation.util;

import org.sakuram.relation.bean.DomainValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class DomainValueFlags {
	private String attributeDomain, repetitionType, validationJsRegEx, languageCode, relationGroup, privacyRestrictionType, scriptConversionType, attributeInUi;
	private Boolean isInputMandatory, isScriptConvertible;
	private Integer searchResultColInd;
	
	public DomainValueFlags(DomainValue domainValue) {
		setDomainValue(domainValue);
	}
	
	public void setDomainValue(DomainValue domainValue) {
    	String flagsArr[];
    	
		attributeDomain = null;
		repetitionType = null;
		validationJsRegEx = null;
		languageCode = null;
		relationGroup = null;
		attributeInUi = null;
		isInputMandatory = null;
		isScriptConvertible = null;
		privacyRestrictionType = null;
		scriptConversionType = null;
		searchResultColInd = null;
		
		if (domainValue.getFlagsCsv() != null && !domainValue.getFlagsCsv().equals("")) {
			flagsArr = domainValue.getFlagsCsv().split(Constants.CSV_SEPARATOR);
		}
		else {
			flagsArr = new String[0];
		}
		if (domainValue.getCategory().equals(Constants.CATEGORY_RELATION_NAME) || domainValue.getCategory().equals(Constants.CATEGORY_RELATION_SUB_TYPE)) {
			if (flagsArr.length > Constants.FLAG_POSITION_RELATION_GROUP) {
				relationGroup = flagsArr[Constants.FLAG_POSITION_RELATION_GROUP];
			}
		} else if (domainValue.getCategory().equals(Constants.CATEGORY_PERSON_ATTRIBUTE) || domainValue.getCategory().equals(Constants.CATEGORY_RELATION_ATTRIBUTE) ||
				domainValue.getCategory().equals(Constants.CATEGORY_ADDITIONAL_PERSON_ATTRIBUTE)) {
			if (flagsArr.length > Constants.FLAG_POSITION_INPUT_AS_ATTRIBUTE) {
				attributeInUi = flagsArr[Constants.FLAG_POSITION_INPUT_AS_ATTRIBUTE];
			}
			if (flagsArr.length > Constants.FLAG_POSITION_REPETITION) {
				repetitionType = flagsArr[Constants.FLAG_POSITION_REPETITION];
			}
			if (flagsArr.length > Constants.FLAG_POSITION_DOMAIN) {
				attributeDomain = flagsArr[Constants.FLAG_POSITION_DOMAIN];
			}
			if (flagsArr.length > Constants.FLAG_POSITION_INPUT_MANDATORY) {
				isInputMandatory = Boolean.valueOf(flagsArr[Constants.FLAG_POSITION_INPUT_MANDATORY]);
			}
			if (flagsArr.length > Constants.FLAG_POSITION_VALIDATION_JS_REG_EX) {
				validationJsRegEx = flagsArr[Constants.FLAG_POSITION_VALIDATION_JS_REG_EX];
			}
			if (flagsArr.length > Constants.FLAG_POSITION_PRIVACY_RESTRICTION) {
				privacyRestrictionType = flagsArr[Constants.FLAG_POSITION_PRIVACY_RESTRICTION];
			}
			if (domainValue.getCategory().equals(Constants.CATEGORY_RELATION_ATTRIBUTE) && flagsArr.length > Constants.FLAG_POSITION_REL_ATTR_APPLICABLE_REL_GROUP) {
				relationGroup = flagsArr[Constants.FLAG_POSITION_REL_ATTR_APPLICABLE_REL_GROUP];
			}
			isScriptConvertible = false;
			if (flagsArr.length > Constants.FLAG_POSITION_SCRIPT_CONVERSION) {
				scriptConversionType = flagsArr[Constants.FLAG_POSITION_SCRIPT_CONVERSION];
				if (!scriptConversionType.equals("")) {
					isScriptConvertible = true;
				}
			}
			if (domainValue.getCategory().equals(Constants.CATEGORY_ADDITIONAL_PERSON_ATTRIBUTE) && flagsArr.length > Constants.FLAG_POSITION_SEARCH_RESULT_COLUMN_INDEX) {
				searchResultColInd = Integer.valueOf(flagsArr[Constants.FLAG_POSITION_SEARCH_RESULT_COLUMN_INDEX]);
			}
		} else if (domainValue.getCategory().equals(Constants.CATEGORY_LANGUAGE)) {
			if (flagsArr.length > Constants.FLAG_POSITION_ISO_LANGUAGE_CODE) {
				languageCode = flagsArr[Constants.FLAG_POSITION_ISO_LANGUAGE_CODE];
			}
		}
		
	}
	
}
