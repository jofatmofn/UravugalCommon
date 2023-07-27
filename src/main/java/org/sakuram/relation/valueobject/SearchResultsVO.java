package org.sakuram.relation.valueobject;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SearchResultsVO {
	int countInDb;
	List<List<String>> resultsList;
	
}
