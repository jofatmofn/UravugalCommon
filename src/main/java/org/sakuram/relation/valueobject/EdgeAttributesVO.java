package org.sakuram.relation.valueobject;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class EdgeAttributesVO {
	private String label;
	private double size;
	private String color;
	private String type;
}
