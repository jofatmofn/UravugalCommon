package org.sakuram.relation.valueobject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PersonVO implements Comparable<PersonVO>{
	
	private String key;
	private NodeAttributesVO attributes;
	@JsonIgnore private String personLabel;
	@JsonIgnore private String firstName;
	@JsonIgnore private String gender;
	@JsonIgnore private boolean hasContributed;
	
	public PersonVO() {
		attributes = new NodeAttributesVO();
	}
	
	public void determineLabel() {
		attributes.setLabel("(" + (key == null ? "" : key) + "/" + (gender == null ? "" : gender) + ")" + (firstName == null ? "" : firstName) +
				(firstName == null || personLabel  == null ? "" : "/") + (personLabel == null ? "" : personLabel));
	}

	public void determineGender(String genderDvValue) {
		gender = genderDvValue.substring(0,1);	// TODO: Incorrect logic (In some languages, duplicates can be there; In some languages, a single character could be made up of multiple unicodes)
	}

	public int compareTo(PersonVO personVO) {
		return (attributes.getY() < personVO.getAttributes().getY() ? -1 : attributes.getY() > personVO.getAttributes().getY() ? 1 : attributes.getX() < personVO.getAttributes().getX() ? -1 : attributes.getX() == personVO.getAttributes().getX() ? 0 : 1);
	}
}
