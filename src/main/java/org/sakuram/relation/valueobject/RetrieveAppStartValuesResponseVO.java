package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RetrieveAppStartValuesResponseVO {
	List<DomainValueVO> domainValueVOList;
	String loggedInUser;
	String inUseProject;
	long inUseLanguage;
	
}
