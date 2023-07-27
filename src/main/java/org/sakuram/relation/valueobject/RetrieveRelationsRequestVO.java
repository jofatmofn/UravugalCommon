package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RetrieveRelationsRequestVO {
	private long startPersonId;
	private Short maxDepth;
	private List<String> requiredRelationsList;

}
