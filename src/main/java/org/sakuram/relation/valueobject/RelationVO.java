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

	private String key;
	private String source;
	private String target;
	private EdgeAttributesVO attributes;
	@JsonIgnore private String relationLabel;
	@JsonIgnore private Map<Long, String> attributeMap;
	@JsonIgnore private Map<Long, String> attributeTranslatedMap;
	@JsonIgnore private boolean toSwap;

	public RelationVO() {
		attributes = new EdgeAttributesVO();
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
		if (attributes.getLabel() != null) {
			return;
		}
		attributes.setLabel(Constants.RELATION_LABEL_TEMPLATE.replaceAll("@@person1@@", source).replaceAll("@@person2@@", target));
		for (Map.Entry<Long, String> attributeEntry : attributeTranslatedMap.entrySet()) {
			attributes.setLabel(attributes.getLabel().replaceAll("@@" + attributeEntry.getKey() + "@@", attributeEntry.getValue()));
		}
		// Beware: Because of the ids 34, 35, 36, 61, 62, the pattern \d\d is used below
		attributes.setLabel((toIncludeRelationId ? "<" + key + ">" : "") + attributes.getLabel().replaceAll("@@\\d\\d@@", "").replaceAll("\\(\\)", ""));
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
		sb.append(attributes.getLabel());
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
