package org.sakuram.relation.valueobject;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class NodeAttributesVO {
	String label;
	double size;
	String color;
	double x;
	double y;
}
