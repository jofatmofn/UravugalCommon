package org.sakuram.relation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.sakuram.relation.bean.AttributeValue;
import org.sakuram.relation.bean.DomainValue;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.repository.DomainValueRepository;
import org.sakuram.relation.repository.RelationRepository;
import org.sakuram.relation.util.AppException;
import org.sakuram.relation.util.Constants;
import org.sakuram.relation.util.DomainValueFlags;
import org.sakuram.relation.valueobject.PersonVO;
import org.sakuram.relation.valueobject.RelationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceParts {

	@Autowired
	RelationRepository relationRepository;
	@Autowired
	DomainValueRepository domainValueRepository;
	
	public PersonVO addToPersonVOMap(Map<Long, PersonVO> personVOMap, Person person) {
    	PersonVO personVO;
    	DomainValue attributeValueDv;
    	
		personVO = new PersonVO();
		personVOMap.put(person.getId(), personVO);
		
		personVO.setKey(String.valueOf(person.getId()));
		personVO.getAttributes().setSize(5.0);
		personVO.getAttributes().setColor(Constants.DEFAULT_NODE_COLOR);
		
		for (AttributeValue attributeValue : person.getAttributeValueList()) {
    		if ((attributeValue.getStartDate() == null || attributeValue.getStartDate().toLocalDate().isBefore(LocalDate.now())) &&
    				(attributeValue.getEndDate() == null || attributeValue.getEndDate().toLocalDate().isAfter(LocalDate.now()))) {
    			if (attributeValue.getAttribute().getId() == Constants.PERSON_ATTRIBUTE_DV_ID_LABEL) {
		    		personVO.setPersonLabel(attributeValue.getAvValue());
		    		// TODO: What if domainValueFlags.getAttributeDomain() not empty
    			} else if (attributeValue.getAttribute().getId() == Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME) {
		    		personVO.setFirstName(attributeValue.getAvValue());
    			} else if (attributeValue.getAttribute().getId() == Constants.PERSON_ATTRIBUTE_DV_ID_GENDER) {
    				attributeValueDv = domainValueRepository.findById(Long.valueOf(attributeValue.getAttributeValue()))
            				.orElseThrow(() -> new AppException("Invalid Attribute Value Dv Id " + attributeValue.getAttributeValue(), null));
		    		personVO.determineGender(attributeValueDv.getDvValue());
    			}
    		}
		}
		
		personVO.determineLabel();
		return personVO;
	}
	
	public RelatedPerson1VO addToRelationVOList(List<RelationVO> relationVOList, Relation relation, Person startPerson, boolean toIncludeRelationId) {
		RelationVO relationVO;
    	DomainValueFlags domainValueFlags;
    	DomainValue attributeDv;
    	String otherPersonId;
    	RelatedPerson1VO relatedPerson1VO;
    	long relationAttributeDVIdOtherForStart, relationSeqNoAttributeDVIdOtherForStart;
    	
		relationVO = new RelationVO();
		relationVOList.add(relationVO);
    	domainValueFlags = new DomainValueFlags();
    	relatedPerson1VO = new RelatedPerson1VO();
		
		relationVO.setKey(String.valueOf(relation.getId()));
		relationVO.getAttributes().setSize(2.0);
		relationVO.getAttributes().setType(Constants.EDGE_TYPE_DIRECT_RELATION);
		if (relation.getPerson1().equals(startPerson)) {
			relatedPerson1VO.person = relation.getPerson2();
			otherPersonId = String.valueOf(relation.getPerson2().getId());
			relationVO.setToSwap(false);
			relationAttributeDVIdOtherForStart = Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1;
			relationSeqNoAttributeDVIdOtherForStart = Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1;
		}
		else if (relation.getPerson2().equals(startPerson)){
			relatedPerson1VO.person = relation.getPerson1();
			otherPersonId = String.valueOf(relation.getPerson1().getId());
			relationVO.setToSwap(true);
			relationAttributeDVIdOtherForStart = Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2;
			relationSeqNoAttributeDVIdOtherForStart = Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2;
		}
		else {
			otherPersonId = null;
			relationVO.setToSwap(false);
			relationAttributeDVIdOtherForStart = -1;
			relationSeqNoAttributeDVIdOtherForStart = -1;
		}
		relationVO.determineSource(String.valueOf(relation.getPerson1().getId()));
		relationVO.determineTarget(String.valueOf(relation.getPerson2().getId()));
		for (AttributeValue attributeValue : relation.getAttributeValueList()) {
    		if (isCurrentValidAttributeValue(attributeValue)) {
        		domainValueFlags.setDomainValue(attributeValue.getAttribute());
        		if (domainValueFlags.getAttributeDomain().equals("")) {
        			relationVO.setAttribute(attributeValue.getAttribute().getId(), attributeValue.getAttributeValue(), attributeValue.getAvValue());
        		}
        		else {
            		attributeDv = domainValueRepository.findById(Long.valueOf(attributeValue.getAttributeValue()))
            				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + attributeValue.getAttributeValue(), null));
        			relationVO.setAttribute(attributeValue.getAttribute().getId(), attributeDv.getValue(), attributeDv.getDvValue());
        		}
    			if (otherPersonId != null && attributeValue.getAttribute().getId() == relationAttributeDVIdOtherForStart) {
    				relatedPerson1VO.relationDvId = attributeValue.getAttributeValue();
    			}
    			else if (otherPersonId != null && attributeValue.getAttribute().getId() == relationSeqNoAttributeDVIdOtherForStart) {
    				relatedPerson1VO.seqNo = Float.parseFloat(attributeValue.getAttributeValue());
    			}
    		}
		}
		relationVO.determineLabel(toIncludeRelationId);
		return relatedPerson1VO;
	}
	
    public boolean isCurrentValidAttributeValue(AttributeValue attributeValue) {
		if ((attributeValue.getStartDate() == null || attributeValue.getStartDate().toLocalDate().isBefore(LocalDate.now())) &&
				(attributeValue.getEndDate() == null || attributeValue.getEndDate().toLocalDate().isAfter(LocalDate.now()))) {
			return true;
		}
		else {
			return false;
		}
    }
    
    protected class RelatedPerson1VO  implements Comparable<RelatedPerson1VO> {
    	Person person;
    	Relation relation;
    	String relationDvId;
    	double seqNo;
    	public int compareTo(RelatedPerson1VO relatedPerson1VO) {	// Assuming the list contains same person
    		if ((this.relationDvId.equals(Constants.RELATION_NAME_SON) || this.relationDvId.equals(Constants.RELATION_NAME_DAUGHTER)) &&
    				(relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_SON) || relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_DAUGHTER))) {
    			return this.seqNo < relatedPerson1VO.seqNo ? -1 : (this.seqNo > relatedPerson1VO.seqNo ? 1 : 0);
    		}
    		return (Integer.parseInt(this.relationDvId) < Integer.parseInt(relatedPerson1VO.relationDvId) ? -1 : (Integer.parseInt(this.relationDvId) > Integer.parseInt(relatedPerson1VO.relationDvId) ? 1 : 
    			(this.seqNo < relatedPerson1VO.seqNo ? -1 : (this.seqNo > relatedPerson1VO.seqNo ? 1 : 0))));
    	}
    }

}
