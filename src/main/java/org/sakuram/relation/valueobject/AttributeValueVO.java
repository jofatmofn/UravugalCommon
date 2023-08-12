package org.sakuram.relation.valueobject;

import java.sql.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class AttributeValueVO {
	private Long id;
	private long attributeDvId;
	private String attributeName;
	private String attributeValue;
	private String translatedValue;
	private boolean isValueApproximate;
	private Date startDate;
	private Date endDate;
	private Boolean isPrivate;
	/* Search criteria only */
	private boolean isMaybeNotRegistered;

	public String getAvValue() {
		return translatedValue == null ? attributeValue : translatedValue;
	}
	
}
