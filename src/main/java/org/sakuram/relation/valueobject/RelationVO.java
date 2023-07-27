package org.sakuram.relation.valueobject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sakuram.relation.util.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RelationVO {

	private String id;
	private String source;
	private String target;
	private String label;
	private double size;
	private String type;
	@JsonIgnore private String relationLabel;
	@JsonIgnore private Map<Long, String> attributeMap;
	@JsonIgnore private Map<Long, String> attributeTranslatedMap;
	@JsonIgnore private boolean toSwap;

	public RelationVO() {
		attributeMap = new HashMap<Long, String>();
		attributeTranslatedMap = new HashMap<Long, String>();
	}
	
	public void determineSource(String source) {
		if (toSwap) {
			this.target = source;
		} else {
			this.source = source;
		}
	}

	public void determineTarget(String target) {
		if (toSwap) {
			this.source = target;
		} else {
			this.target = target;
		}
	}

	public void determineLabel(boolean toIncludeRelationId) {
		if (label != null) {
			return;
		}
		label = Constants.RELATION_LABEL_TEMPLATE;
		for (Map.Entry<Long, String> attributeEntry : attributeTranslatedMap.entrySet()) {
			label = label.replaceAll("@@" + attributeEntry.getKey() + "@@", attributeEntry.getValue());
		}
		// Beware: Because of the ids 34, 35, 36, 61, 62, the pattern \d\d is used below
		label = (toIncludeRelationId ? "<" + id + ">" : "") + label.replaceAll("@@\\d\\d@@", "").replaceAll("\\(\\)", "");
	}

	public String getAttribute(long attributeDvId) {
		if(attributeMap.containsKey(attributeDvId)) {
			return attributeMap.get(attributeDvId);
		} else {
			return "";
		}
	}

	public void setAttribute(long attributeDvId, String attributeValue, String translatedValue) {
		if (toSwap) {
			if (attributeDvId == Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2) {
				attributeMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, attributeValue);
				attributeTranslatedMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, translatedValue);
			} else if (attributeDvId == Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1) {
				attributeMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, attributeValue);
				attributeTranslatedMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, translatedValue);
			} else if (attributeDvId == Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2) {
				attributeMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1, attributeValue);
				attributeTranslatedMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1, translatedValue);
			} else if (attributeDvId == Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1) {
				attributeMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2, attributeValue);
				attributeTranslatedMap.put(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2, translatedValue);
			} else {
				attributeMap.put(attributeDvId, attributeValue);
				attributeTranslatedMap.put(attributeDvId, translatedValue);
			}
		} else {
			attributeMap.put(attributeDvId, attributeValue);
			attributeTranslatedMap.put(attributeDvId, translatedValue);
		}
	}

	public String toString() {
		StringBuffer sb;
		sb = new StringBuffer(1000);
		sb.append("Source: ");
		sb.append(source);
		sb.append("\n");
		sb.append("Target: ");
		sb.append(target);
		sb.append("\n");
		sb.append("Label: ");
		sb.append(label);
		sb.append("\n");
		for (Entry<Long, String> attributeEntry : attributeMap.entrySet()) {
			sb.append(attributeEntry.getKey());
			sb.append(": ");
			sb.append(attributeEntry.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}
}
