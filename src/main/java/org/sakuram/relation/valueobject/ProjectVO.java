package org.sakuram.relation.valueobject;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProjectVO {
	private Long tenantId;
	private String projectName;
	private boolean isAppReadOnly;
	private long personCount;
}
