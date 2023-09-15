package org.sakuram.relation.valueobject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class PersonVO implements Comparable<PersonVO>{
	
	private String id;
	private String label;
	@JsonIgnore private String personLabel;
	@JsonIgnore private String firstName;
	@JsonIgnore private String gender;
	private double size;
	private String color;
	private double x;
	private double y;
	@JsonIgnore private boolean hasContributed;
	
	public void determineLabel() {
		label =  "(" + (id == null ? "" : id) + "/" + (gender == null ? "" : gender) + ")" + (firstName == null ? "" : firstName) +
				(firstName == null || personLabel  == null ? "" : "/") + (personLabel == null ? "" : personLabel);
	}

	public void determineGender(String genderDvValue) {
		gender = genderDvValue.substring(0,1);	// TODO: Incorrect logic (In some languages, duplicates can be there; In some languages, a single character could be made up of multiple unicodes)
	}

	public int compareTo(PersonVO personVO) {
		return (this.getY() < personVO.getY() ? -1 : this.getY() > personVO.getY() ? 1 : this.getX() < personVO.getX() ? -1 : this.getX() == personVO.getX() ? 0 : 1);
	}
}
