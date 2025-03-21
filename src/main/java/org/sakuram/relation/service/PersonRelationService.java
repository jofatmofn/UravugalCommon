package org.sakuram.relation.service;

import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.javatuples.Pair;
import org.javatuples.Octet;
import org.javatuples.Quintet;
import org.sakuram.relation.bean.AttributeValue;
import org.sakuram.relation.bean.DomainValue;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.bean.Tenant;
import org.sakuram.relation.bean.Translation;
import org.sakuram.relation.repository.AttributeValueRepository;
import org.sakuram.relation.repository.DomainValueRepository;
import org.sakuram.relation.repository.PersonRepository;
import org.sakuram.relation.repository.RelationRepository;
import org.sakuram.relation.repository.TenantRepository;
import org.sakuram.relation.repository.TranslationRepository;
import org.sakuram.relation.service.ServiceParts.RelatedPerson1VO;
import org.sakuram.relation.util.AppException;
import org.sakuram.relation.util.Constants;
import org.sakuram.relation.util.DomainValueFlags;
import org.sakuram.relation.util.SecurityContext;
import org.sakuram.relation.util.UtilFuncs;
import org.sakuram.relation.valueobject.AttributeValueVO;
import org.sakuram.relation.valueobject.DomainValueVO;
import org.sakuram.relation.valueobject.RetrieveRelationsRequestVO;
import org.sakuram.relation.valueobject.GraphVO;
import org.sakuram.relation.valueobject.PersonSearchCriteriaVO;
import org.sakuram.relation.valueobject.PersonVO;
import org.sakuram.relation.valueobject.SaveAttributesRequestVO;
import org.sakuram.relation.valueobject.SaveAttributesResponseVO;
import org.sakuram.relation.valueobject.SaveOtherRelationRequestVO;
import org.sakuram.relation.valueobject.SearchResultsVO;
import org.sakuram.relation.valueobject.RelatedPersonsVO;
import org.sakuram.relation.valueobject.RelationVO;
import org.sakuram.relation.valueobject.RetrieveAppStartValuesResponseVO;
import org.sakuram.relation.valueobject.RetrievePersonAttributesResponseVO;
import org.sakuram.relation.valueobject.RetrieveRelationAttributesResponseVO;
import org.sakuram.relation.valueobject.RetrieveRelationsBetweenRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonRelationService {
	
	@Autowired
	PersonRepository personRepository;
	@Autowired
	RelationRepository relationRepository;
	@Autowired
	DomainValueRepository domainValueRepository;
	@Autowired
	AttributeValueRepository attributeValueRepository;
	@Autowired
	TenantRepository tenantRepository;
	@Autowired
	TranslationRepository translationRepository;
	
	@Autowired
	ServiceParts serviceParts;
	@Autowired
	ProjectUserService projectUserService;
	
	int maxLevel;
	
	public GraphVO retrieveRelations(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
    	Person startPerson;
    	List<Relation> participatingRelationList;
    	Set<Person> relatedPersonSet;
    	int ind;
    	List<RelatedPerson3VO> fatherRelatedPerson3VOList, motherRelatedPerson3VOList, spouseRelatedPerson3VOList, childRelatedPerson3VOList, siblingRelatedPerson3VOList;
    	DomainValue person2ForPerson1Dv, sequenceOfPerson2ForPerson1Dv;
    	AttributeValue attributeValue;
    	List<Person> siblingList;
    	List<Long> siblingIdList;
    	GraphVO retrieveRelationsResponseVO;
    	Map<Long, PersonVO> personVOMap;
    	List<Relation> relationList;
    	List<RelationVO> relationVOList;
    	RelatedPerson1VO relatedPerson1VO;
    	PersonVO personVO;

    	sequenceOfPerson2ForPerson1Dv = domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1)
				.orElseThrow(() -> new AppException("Domain Value missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1, null));
    	person2ForPerson1Dv = domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)
				.orElseThrow(() -> new AppException("Domain Value missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, null));
		
    	relatedPersonSet = new HashSet<Person>();
    	startPerson = personRepository.findByIdAndTenant(retrieveRelationsRequestVO.getStartPersonId(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + retrieveRelationsRequestVO.getStartPersonId(), null));
		relatedPersonSet.add(startPerson);
    	participatingRelationList = relationRepository.findByPerson1(startPerson);
    	for (Relation relation : participatingRelationList) {
    		relatedPersonSet.add(relation.getPerson2());
    	}
    	siblingIdList = new ArrayList<Long>();
    	participatingRelationList = relationRepository.findByPerson2(startPerson);
    	for (Relation relation : participatingRelationList) {
    		relatedPersonSet.add(relation.getPerson1());
	    	attributeValue = fetchAttribute(null, relation, person2ForPerson1Dv)
					.orElseThrow(() -> new AppException("Relation without PERSON2_FOR_PERSON1 attribute", null));
	    	if (attributeValue.getAttributeValue().equals(Constants.RELATION_NAME_SON) || attributeValue.getAttributeValue().equals(Constants.RELATION_NAME_DAUGHTER)) {
	    		siblingList = personRepository.findKids(relation.getPerson1().getId(), SecurityContext.getCurrentTenantId());
	    		relatedPersonSet.addAll(siblingList);
	    		for (Person person : siblingList) {
	    			siblingIdList.add(person.getId());
	    		}
	    	}
    	}
    	
    	retrieveRelationsResponseVO = new GraphVO();
    	personVOMap = new HashMap<Long, PersonVO>();
    	relationVOList = new ArrayList<RelationVO>();
    	retrieveRelationsResponseVO.setEdges(relationVOList);
    	relationList = new ArrayList<Relation>();
    	
    	for (Person person : relatedPersonSet) {
    		personVO = serviceParts.addToPersonVOMap(personVOMap, person);
    		if (person.equals(startPerson)) {
        		personVO.getAttributes().setX(1);
        		personVO.getAttributes().setY(6);
    		}
    	}
    	
    	fatherRelatedPerson3VOList = new ArrayList<RelatedPerson3VO>();
    	motherRelatedPerson3VOList = new ArrayList<RelatedPerson3VO>();
    	spouseRelatedPerson3VOList = new ArrayList<RelatedPerson3VO>();
    	childRelatedPerson3VOList = new ArrayList<RelatedPerson3VO>();
    	siblingRelatedPerson3VOList = new ArrayList<RelatedPerson3VO>();
    	relationList = relationRepository.findByPerson1InAndPerson2In(relatedPersonSet, relatedPersonSet);
    	for (Relation relation : relationList) {
    		relatedPerson1VO = serviceParts.addToRelationVOList(relationVOList, relation, startPerson, false);
    		if (relatedPerson1VO.person == null) {	// (1) Siblings (2) Husband-Wife relation between parents
				if (siblingIdList.contains(relation.getPerson2().getId()) && !siblingRelatedPerson3VOList.contains(new RelatedPerson3VO(relation.getPerson2().getId(), 0))) { // Sibling && Not in list already
	        		personVO = personVOMap.get(relation.getPerson2().getId());
	        		System.out.println("Sibling ==> " + relation.getPerson2().getId());
	        		attributeValue = fetchAttribute(null, relation, sequenceOfPerson2ForPerson1Dv).orElse(null);
					siblingRelatedPerson3VOList.add(new RelatedPerson3VO(relation.getPerson2().getId(), attributeValue == null ? 0 : Double.valueOf(attributeValue.getAttributeValue())));
			    	personVO.getAttributes().setY(9);
				}
    		} else {
        		personVO = personVOMap.get(relatedPerson1VO.person.getId());
	    		if (relatedPerson1VO.relationDvId == null) {
	        		personVO.getAttributes().setX(Math.random() * 10);
	        		personVO.getAttributes().setY(Math.random() * 10);
	    		}
	    		else {
					switch(relatedPerson1VO.relationDvId) {
					case Constants.RELATION_NAME_FATHER:
						fatherRelatedPerson3VOList.add(new RelatedPerson3VO(relatedPerson1VO.person.getId(), relatedPerson1VO.seqNo));
						personVO.getAttributes().setX(2.5);
			    		break;
					case Constants.RELATION_NAME_MOTHER:
						motherRelatedPerson3VOList.add(new RelatedPerson3VO(relatedPerson1VO.person.getId(), relatedPerson1VO.seqNo));
						personVO.getAttributes().setX(8.5);
			    		break;
					case Constants.RELATION_NAME_HUSBAND:
					case Constants.RELATION_NAME_WIFE:
						spouseRelatedPerson3VOList.add(new RelatedPerson3VO(relatedPerson1VO.person.getId(), relatedPerson1VO.seqNo));
						personVO.getAttributes().setX(10);
			    		break;
					case Constants.RELATION_NAME_SON:
					case Constants.RELATION_NAME_DAUGHTER:
						childRelatedPerson3VOList.add(new RelatedPerson3VO(relatedPerson1VO.person.getId(), relatedPerson1VO.seqNo));
				    	personVO.getAttributes().setY(0);
			    		break;
					}
	    		}
    		}
    	}

    	Collections.sort(fatherRelatedPerson3VOList);
    	ind = 12;
    	for (RelatedPerson3VO relatedPerson3VO : fatherRelatedPerson3VOList) {
    		ind -= 1;
    		personVOMap.get(relatedPerson3VO.personId).getAttributes().setY(ind);
    	}
    	Collections.sort(motherRelatedPerson3VOList);
    	ind = 12;
    	for (RelatedPerson3VO relatedPerson3VO : motherRelatedPerson3VOList) {
    		ind -= 1;
    		personVOMap.get(relatedPerson3VO.personId).getAttributes().setY(ind);
    	}
    	Collections.sort(spouseRelatedPerson3VOList);
    	ind = 7;
    	for (RelatedPerson3VO relatedPerson3VO : spouseRelatedPerson3VOList) {
    		ind -= 1;
    		personVOMap.get(relatedPerson3VO.personId).getAttributes().setY(ind);
    	}
    	Collections.sort(childRelatedPerson3VOList);
    	ind = 0;
    	for (RelatedPerson3VO relatedPerson3VO : childRelatedPerson3VOList) {
    		ind += 2;
    		personVOMap.get(relatedPerson3VO.personId).getAttributes().setX(ind);
    	}
    	Collections.sort(siblingRelatedPerson3VOList);
    	ind = 5;
    	for (RelatedPerson3VO relatedPerson3VO : siblingRelatedPerson3VOList) {
    		ind += 2;
    		personVOMap.get(relatedPerson3VO.personId).getAttributes().setX(ind);
    	}
    	
    	retrieveRelationsResponseVO.setNodes(new ArrayList<PersonVO>(personVOMap.values()));
    	return retrieveRelationsResponseVO;
    }
	
	public List<RelationVO> retrieveRelationsBetween(RetrieveRelationsBetweenRequestVO retrieveRelationsBetweenRequestVO) {
    	Person end1Person;
    	List<Relation> relationList;
    	List<RelationVO> relationVOList;
    	
    	end1Person = personRepository.findByIdAndTenant(retrieveRelationsBetweenRequestVO.getEnd1PersonId(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + retrieveRelationsBetweenRequestVO.getEnd1PersonId(), null));
    	relationList = relationRepository.findByPerson1(end1Person);
    	relationList.addAll(relationRepository.findByPerson2(end1Person));
    	
    	relationVOList = new ArrayList<RelationVO>();
    	for (Relation relation : relationList) {
    		if (relation.getPerson1().getId() == retrieveRelationsBetweenRequestVO.getEnd1PersonId() && retrieveRelationsBetweenRequestVO.getEnd2PersonIdsList().contains(relation.getPerson2().getId()) ||
    				relation.getPerson2().getId() == retrieveRelationsBetweenRequestVO.getEnd1PersonId() && retrieveRelationsBetweenRequestVO.getEnd2PersonIdsList().contains(relation.getPerson1().getId())) {
    			serviceParts.addToRelationVOList(relationVOList, relation, null, false);
    		}
    	}
    	return relationVOList;
    }
	
	public GraphVO retrieveTree(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
    	Person startPerson, currentPerson;
    	Set<Long> relatedPersonIdSet, relatedRelationIdSet;
    	List<RelatedPerson2VO> relatedPerson2VOList;
    	RelatedPerson2VO relatedPerson2VO, parentRelatedPerson2VO;
    	int readInd, level, currentLevel, currInd, startInd, endInd;
    	double sequence, sequenceAdjustment;
    	List<Double> seqAtLevel;
    	List<Person> excludeSpouseList;
    	boolean isFirstKid, isShifted;
    	GraphVO retrieveRelationsResponseVO;
    	Map<Long, PersonVO> personVOMap;
    	PersonVO relatedPersonVO, currentPersonVO, parentPersonVO;
    	List<PersonVO> personVOList;
    	List<RelationVO> relationVOList;
    	
    	retrieveRelationsResponseVO = new GraphVO();
    	personVOMap = new HashMap<Long, PersonVO>();
    	relationVOList = new ArrayList<RelationVO>();
    	retrieveRelationsResponseVO.setEdges(relationVOList);
    	relatedPersonIdSet = new HashSet<Long>();
    	relatedRelationIdSet = new HashSet<Long>();
    	excludeSpouseList = new ArrayList<Person>();
		relatedPerson2VOList = new ArrayList<RelatedPerson2VO>();
    	startPerson = personRepository.findByIdAndTenant(retrieveRelationsRequestVO.getStartPersonId(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + retrieveRelationsRequestVO.getStartPersonId(), null));
		relatedPersonIdSet.add(startPerson.getId());
		currentPersonVO = serviceParts.addToPersonVOMap(personVOMap, startPerson);
		currentPersonVO.getAttributes().setX(0);
		currentPersonVO.getAttributes().setY(0);
		if (retrieveRelationsRequestVO.getRequiredRelationsList() == null || retrieveRelationsRequestVO.getRequiredRelationsList().isEmpty()) {
			retrieveRelationsRequestVO.setRequiredRelationsList(Arrays.asList(Constants.RELATION_NAME_HUSBAND, Constants.RELATION_NAME_WIFE, Constants.RELATION_NAME_SON, Constants.RELATION_NAME_DAUGHTER));
		}
		
		relatedPerson2VO =  new RelatedPerson2VO();
		relatedPerson2VOList.add(relatedPerson2VO);
		relatedPerson2VO.person = startPerson;
		relatedPerson2VO.level = 0;
		seqAtLevel = new ArrayList<Double>();
		seqAtLevel.add(0D);
		readInd = 0;
		/*		relatedPerson2VOList - Built (appended) as it is traversed (Concurrent modification). Will not be in any order w.r.t X and Y.
		 * 		current - Item from relatedPerson2VOList, currently being processed
		 * 		list of relatedPerson1VO - Relatives of current person (in the order of relation type and sequence no.
		 * 		relatedPerson2VO - New person (relatedPerson1VO) added to relatedPerson2VOList
		 * 		relatedPersonVO - New person (relatedPerson1VO) added to personVOMap
		 */
		while (true) {
			currentPerson = relatedPerson2VOList.get(readInd).person;
			currentLevel = relatedPerson2VOList.get(readInd).level;
			LogManager.getLogger().debug("Current person: " + currentPerson.getId() + " at level " + currentLevel + " at index " + readInd);
			if (currentLevel == retrieveRelationsRequestVO.getMaxDepth()) {
				break;
			}
			if (relatedPerson2VOList.get(readInd).isSpouse) {
				excludeSpouseList.add(currentPerson);
			}
			else {
				currentPersonVO = personVOMap.get(currentPerson.getId());
				isFirstKid = true;
	    		currentPersonVO.setHasContributed(false);
		    	for (RelatedPerson1VO relatedPerson1VO : retrieveRelatives(currentPerson, retrieveRelationsRequestVO.getRequiredRelationsList())) {
		    		currentPersonVO.setHasContributed(true);
					if (relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_HUSBAND) ||
							relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_WIFE) ||
							currentLevel < retrieveRelationsRequestVO.getMaxDepth() - 1) {
						if (relatedPersonIdSet.add(relatedPerson1VO.person.getId())) {
							sequence = 0;
							relatedPerson2VO =  new RelatedPerson2VO();
							relatedPerson2VO.person = relatedPerson1VO.person;
							relatedPerson2VO.parentInd = readInd;
				    		relatedPersonVO = serviceParts.addToPersonVOMap(personVOMap, relatedPerson1VO.person);
							LogManager.getLogger().debug("Added person: " + relatedPersonVO.getAttributes().getLabel());
				    		if (relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_HUSBAND) || relatedPerson1VO.relationDvId.equals(Constants.RELATION_NAME_WIFE)) {
				    			relatedPerson2VO.level = currentLevel;
				    			relatedPerson2VO.isSpouse = true;
			    				sequence = currentPersonVO.getAttributes().getX() + relatedPerson1VO.seqNo;
			    				currInd = (int) (readInd + relatedPerson1VO.seqNo);
			    				LogManager.getLogger().debug(" at index " + currInd);
			    				if (currInd >= relatedPerson2VOList.size()) {
				    				if (relatedPerson2VOList.size() == Constants.EXPORT_TREE_MAX_PERSONS && Constants.EXPORT_TREE_MAX_PERSONS != 0) {
				    					throw new AppException("You are not authorised to export such a huge data.", null);
				    				}
									relatedPerson2VOList.add(relatedPerson2VO);
			    				} else {
									relatedPerson2VOList.add(currInd, relatedPerson2VO);
			    					shiftX(personVOMap, relatedPerson2VOList, currInd + 1, 1);
			    				}
								LogManager.getLogger().debug("currentPersonVO.getX(): " + currentPersonVO.getAttributes().getX() + ". relatedPerson1VO.seqNo: " + relatedPerson1VO.seqNo);
								LogManager.getLogger().debug("relatedPerson2VO.level: " + relatedPerson2VO.level + ". Initial sequence: " + sequence);
								relatedPerson2VO.sequence = sequence;
								relatedPersonVO.getAttributes().setX(sequence);
								relatedPersonVO.getAttributes().setY(currentPersonVO.getAttributes().getY());
								if (sequence > seqAtLevel.get(currentLevel)) {
					    			seqAtLevel.set(currentLevel, sequence);
								}
				    		}
				    		else if (currentLevel < retrieveRelationsRequestVO.getMaxDepth() - 1) {
								relatedPerson2VOList.add(relatedPerson2VO);
				    			level = currentLevel + 1;
				    			relatedPerson2VO.level = level;
				    			relatedPerson2VO.isSpouse = false;
				    			if (seqAtLevel.size() > level) {
				    				sequence = seqAtLevel.get(level);
					    			sequence++;
				    			}
				    			else {
				    				seqAtLevel.add(0D);
				    				sequence = 0D;
				    			}
				    			relatedPerson2VO.isFirstKid = false;
				    			if (isFirstKid) {
					    			relatedPerson2VO.isFirstKid = true;
				    				if (currentPersonVO.getAttributes().getX() >= sequence) {	// Position of child
				    					sequence = currentPersonVO.getAttributes().getX();
				    				}
				    				isFirstKid = false;
				    			}
				    			// UtilFuncs.listSet(relatedPerson2VOList, sequence, relatedPerson2VO, null);
								LogManager.getLogger().debug("sequence: " + sequence);
				    			seqAtLevel.set(level, sequence);
								relatedPerson2VO.sequence = sequence;
								relatedPersonVO.getAttributes().setX(sequence);
								relatedPersonVO.getAttributes().setY(-level);
				    		}
				    		LogManager.getLogger().debug("Added person: " + relatedPerson2VO.person.getId() + " at level " + relatedPerson2VO.level);

						}
						else LogManager.getLogger().debug("Skipped (due to duplicate) person: " + relatedPerson1VO.person.getId());
						if (relatedRelationIdSet.add(relatedPerson1VO.relation.getId())) {
							serviceParts.addToRelationVOList(relationVOList, relatedPerson1VO.relation, currentPerson, false);
						}
					}
					else LogManager.getLogger().debug("Skipped (due to higher depth) person: " + relatedPerson1VO.person.getId());
				}
			}
	    	readInd++;
	    	if (readInd == relatedPerson2VOList.size()) {
	    		break;
	    	}
		}
		
		// Align parents to first kid
		isShifted = true;
		while(isShifted) {	// Multiple iterations, there should be a better performing solution
			isShifted = false;
			currInd = readInd - 1;
			for (int ind = seqAtLevel.size() - 1; ind > -1; ind--) {	// Levels Descending, Sequence Ascending
				endInd = currInd;
				while (currInd > -1 && relatedPerson2VOList.get(currInd).level == ind) currInd--;
				startInd = currInd + 1;
				for (int ind2 = startInd; ind2 <= endInd; ind2++)
				{
					relatedPerson2VO = relatedPerson2VOList.get(ind2);
					currentPersonVO = personVOMap.get(relatedPerson2VO.person.getId());
					currentPersonVO.getAttributes().setX(relatedPerson2VO.sequence);
					if (relatedPerson2VO.isFirstKid) {
						parentRelatedPerson2VO = relatedPerson2VOList.get(relatedPerson2VO.parentInd);
						LogManager.getLogger().debug("Current Person: " + currentPersonVO.getKey() + " : " + currentPersonVO.getAttributes().getX() + " : " + currentPersonVO.getAttributes().getY());
						parentPersonVO = personVOMap.get(parentRelatedPerson2VO.person.getId());
						LogManager.getLogger().debug("Parent Person: " + parentPersonVO.getKey() + " : " + parentPersonVO.getAttributes().getX() + " : " + parentPersonVO.getAttributes().getY());
						sequenceAdjustment = relatedPerson2VO.sequence - parentRelatedPerson2VO.sequence;
						if (sequenceAdjustment < 0) {
							shiftX(personVOMap, relatedPerson2VOList, ind2, (float) (0 - sequenceAdjustment));	// Shift Child
							isShifted = true;
						} else if (sequenceAdjustment > 0) {
							shiftX(personVOMap, relatedPerson2VOList, relatedPerson2VO.parentInd, (float) (sequenceAdjustment));	// Shift Parent
						}
					}
				}
			}
		}
		
		for (Person spouse : excludeSpouseList) {
	    	for (RelatedPerson1VO relatedPerson1VO : retrieveRelatives(spouse, retrieveRelationsRequestVO.getRequiredRelationsList())) {
				if (relatedPersonIdSet.contains(relatedPerson1VO.person.getId()) && relatedRelationIdSet.add(relatedPerson1VO.relation.getId())) {
					serviceParts.addToRelationVOList(relationVOList, relatedPerson1VO.relation, spouse, false);
				}
	    	}
		}
    	
		personVOList = new ArrayList<PersonVO>(personVOMap.values());
		for (PersonVO personVO : personVOList) {
			LogManager.getLogger().debug(personVO.getKey() + "/"  + personVO.getFirstName() + "/" + personVO.getAttributes().getY() + "/" + personVO.getAttributes().getX());
		}
    	retrieveRelationsResponseVO.setNodes(personVOList);
    	return retrieveRelationsResponseVO;
    }
	
	private void shiftX(Map<Long, PersonVO> personVOMap, List<RelatedPerson2VO> relatedPerson2VOList, int toShiftInd, double shiftBy) {
		int ind;
		RelatedPerson2VO relatedPerson2VO, firstRelatedPerson2VO;
		PersonVO personVO;
		double sequence;
		
		firstRelatedPerson2VO = relatedPerson2VOList.get(toShiftInd);
		ind = toShiftInd;
		while (ind < relatedPerson2VOList.size()) {
			relatedPerson2VO = relatedPerson2VOList.get(ind);
			personVO = personVOMap.get(relatedPerson2VO.person.getId());
			if (relatedPerson2VO.level != firstRelatedPerson2VO.level) break;
			LogManager.getLogger().debug("Shift: " + personVO.getKey() + ", Level: " + personVO.getAttributes().getY() + ", Sequence: " + relatedPerson2VO.sequence + ", Shift: " + shiftBy);
			
			sequence = relatedPerson2VO.sequence;
			if (ind == toShiftInd) {
				sequence = relatedPerson2VO.sequence + shiftBy;
			} else if (relatedPerson2VOList.get(ind - 1).sequence >= relatedPerson2VO.sequence) {
				sequence = relatedPerson2VOList.get(ind - 1).sequence + 1;
			}
			LogManager.getLogger().debug("New sequence: " + sequence);
			relatedPerson2VO.sequence = sequence;
			personVO.getAttributes().setX(sequence);
			ind++;
		}
	}
	
	public List<List<Object>> exportFullTree(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
		List<List<Object>> treeCsvContents;
		GraphVO treeGraphVO;
		List<Long> forPersonsList;
		List<RelatedPerson1VO> relatedPerson1VOList;
		Person person;
		List<String> requiredRelationTypesList;
		int mainPersonInd, matchSpouseInd;
		PersonVO mainPersonVO, matchSpouseVO;
		long retrievePersonId;
		
		treeCsvContents = new ArrayList<List<Object>>();
		treeGraphVO = retrieveRoots(retrieveRelationsRequestVO);
		forPersonsList = new ArrayList<Long>();
		requiredRelationTypesList = Constants.RELATION_NAME_LIST_SPOUSE;
		mainPersonInd = 0;
		while (mainPersonInd < treeGraphVO.getNodes().size()) {
			mainPersonVO = treeGraphVO.getNodes().get(mainPersonInd);
			LogManager.getLogger().debug("Main Person: " + mainPersonVO.getAttributes().getLabel());
			retrievePersonId = Long.parseLong(mainPersonVO.getKey());
			person = personRepository.findByIdAndTenant(retrievePersonId, SecurityContext.getCurrentTenant())
					.orElseThrow(() -> new AppException("Invalid Person Id ", null));
	    	
			relatedPerson1VOList = retrieveRelatives(person, requiredRelationTypesList);
			
			matchSpouseVO = null;
			matchSpouseInd = mainPersonInd + 1;
			match:
			while (matchSpouseInd < treeGraphVO.getNodes().size()) {
				matchSpouseVO = treeGraphVO.getNodes().get(matchSpouseInd);
				for (RelatedPerson1VO relatedPerson1VO : relatedPerson1VOList) {
					if (Long.parseLong(matchSpouseVO.getKey()) == relatedPerson1VO.person.getId()) {
						break match;
					}
				}
				matchSpouseInd++;
			}
			if (matchSpouseInd < treeGraphVO.getNodes().size()) {
				LogManager.getLogger().debug("Matching Spouse: " + matchSpouseVO.getAttributes().getLabel());
				if (!mainPersonVO.isHasContributed() && !matchSpouseVO.isHasContributed()) {
					retrievePersonId = Long.parseLong(matchSpouseVO.getKey());
					person = personRepository.findByIdAndTenant(retrievePersonId, SecurityContext.getCurrentTenant())
							.orElseThrow(() -> new AppException("Invalid Person Id ", null));
					if (relatedPerson1VOList.size() > retrieveRelatives(person, requiredRelationTypesList).size()) {
						LogManager.getLogger().debug("Picked Main");
						forPersonsList.add(Long.parseLong(mainPersonVO.getKey()));
					} else {
						LogManager.getLogger().debug("Picked Spouse");
						forPersonsList.add(Long.parseLong(matchSpouseVO.getKey()));
					}
				}
				treeGraphVO.getNodes().remove(matchSpouseVO);
			} else if (!mainPersonVO.isHasContributed()) {
				LogManager.getLogger().debug("No matched, hence Main");
				forPersonsList.add(Long.parseLong(mainPersonVO.getKey()));
			}
			mainPersonInd++;
		}
		
		retrieveRelationsRequestVO.setRequiredRelationsList(null);
		for (long personId : forPersonsList) {
				retrieveRelationsRequestVO.setStartPersonId(personId);
				treeCsvContents.addAll(exportTree(retrieveRelationsRequestVO));
				if (treeCsvContents.size() > Constants.EXPORT_TREE_MAX_ROWS && Constants.EXPORT_TREE_MAX_ROWS != 0) {
					throw new AppException("You are not authorised to export such a huge data.", null);
				}
				treeCsvContents.add(new ArrayList<Object>());
				treeCsvContents.add(new ArrayList<Object>());
		}
		return treeCsvContents;
	}
	
	public List<List<Object>> exportTree(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
		List<List<Object>> treeCsvContents;
		GraphVO treeGraphVO;
		Map<String, PersonVO> personsMap;
		List<Object> treeCsvRow;
		
		treeCsvContents = new ArrayList<List<Object>>();
		retrieveRelationsRequestVO.setMaxDepth(Constants.EXPORT_TREE_MAX_DEPTH);
		treeGraphVO = retrieveTree(retrieveRelationsRequestVO);
		
		personsMap = new HashMap<String, PersonVO>();
		for(PersonVO node : treeGraphVO.getNodes()) {
			personsMap.put(node.getKey(), node);
		}

		maxLevel = 0;
		exportWriteTree(String.valueOf(retrieveRelationsRequestVO.getStartPersonId()), 0, personsMap, treeGraphVO.getEdges(), treeCsvContents);
		
		treeCsvRow = new ArrayList<Object>((maxLevel + 1) * 2);
		treeCsvContents.add(0, treeCsvRow);
		for(int ind2 = 0; ind2 <= maxLevel; ind2++) {	// TODO: Multi-language support for the following column titles
			treeCsvRow.add("Level " + (ind2 + 1));
			treeCsvRow.add("Level " + (ind2 + 1) + " Spouse");
		}
		
		LogManager.getLogger().debug("Exported rows: " + treeCsvContents.size());
		return treeCsvContents;
	}
	
	public GraphVO displayTree(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
		List<List<Object>> treeCsvContents;
		GraphVO treeGraphVO;
		Map<String, PersonVO> personsMap;
		
		treeCsvContents = new ArrayList<List<Object>>();
		retrieveRelationsRequestVO.setMaxDepth(Constants.EXPORT_TREE_MAX_DEPTH);
		treeGraphVO = retrieveTree(retrieveRelationsRequestVO);
		
		// Rest of the logic is a round-about way of setting X and Y :(
		personsMap = new HashMap<String, PersonVO>();
		for(PersonVO node : treeGraphVO.getNodes()) {
			personsMap.put(node.getKey(), node);
		}

		maxLevel = 0;
		exportWriteTree(String.valueOf(retrieveRelationsRequestVO.getStartPersonId()), 0, personsMap, treeGraphVO.getEdges(), treeCsvContents);
		
		return treeGraphVO;
	}
	
	private void exportWriteTree(String personId, int level, Map<String, PersonVO> personsMap, List<RelationVO> relationsList, List<List<Object>> treeCsvContents) {
		List<String> spousesList, kidsList;
		boolean isFirstSpouse;
		Relation relation;
		DomainValue domainValue;
		AttributeValue attributeValue;
		
		if (level > maxLevel) {
			maxLevel = level;
		}
		// Person
		exportWriteRow(personId, level * 2, false, personsMap, treeCsvContents, true);
		
		// Kids (Only one parent specified)
		kidsList = getKids(personId, null, relationsList);
		for(String kidId : kidsList) {
			exportWriteTree(kidId, level + 1, personsMap, relationsList, treeCsvContents);
		}
		
		// Spouse
		spousesList = getSpouses(personId, relationsList);
		isFirstSpouse = (kidsList.size() == 0 ? true : false);
		for(String spouseId : spousesList) {
			kidsList = getKids(personId, spouseId, relationsList);
			
			attributeValue = null;
			if (kidsList.size() == 0) {
				if ((relation = relationRepository.findRelationGivenPersons(Long.parseLong(personId), Long.parseLong(spouseId), SecurityContext.getCurrentTenantId())) == null) {
					throw new AppException(personId + " and " + spouseId + " are not related.", null);
				}
				domainValue = domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_YYYY_OF_RELATION_END)
						.orElseThrow(() -> new AppException("Domain Value missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_YYYY_OF_RELATION_END, null));
		    	attributeValue = fetchAttribute(null, relation, domainValue)
						.orElse(null);
			}
			// attributeValue of null implies Husband-Wife relation is still there OR they have kid(s)
			exportWriteRow(spouseId, level * 2 + 1, isFirstSpouse, personsMap, treeCsvContents, attributeValue == null);
			isFirstSpouse = false;
			
			// Kids (Both parents are specified)
			for(String kidId : kidsList) {
				exportWriteTree(kidId, level + 1, personsMap, relationsList, treeCsvContents);
			}
		}
	}
	
	private void exportWriteRow(String personId, int index, boolean toReuseLastRow, Map<String, PersonVO> personsMap, List<List<Object>> treeCsvContents, boolean toWrite) {
		List<Object> treeCsvRow;
		PersonVO personVO;
		
		if (toReuseLastRow) {
			treeCsvRow = treeCsvContents.get(treeCsvContents.size() - 1);
		} else {
			treeCsvRow = new ArrayList<Object>(index + 1);
			treeCsvContents.add(treeCsvRow);
		}
		if(personsMap.containsKey(personId)) {
			personVO = personsMap.get(personId);
			if (toWrite) {
				UtilFuncs.listSet(treeCsvRow, index, personVO.getAttributes().getLabel(), null);
			}
			personVO.getAttributes().setY(-treeCsvContents.indexOf(treeCsvRow));
			personVO.getAttributes().setX(index);
			LogManager.getLogger().debug(personVO.getKey() + ":"  + personVO.getFirstName() + ":" + personVO.getAttributes().getY() + ":" + personVO.getAttributes().getX());
		} else {
			throw new AppException("Application in inconsistent state?! PersonId: " + personId, null);
			// UtilFuncs.listSet(treeCsvRow, index, personId, null);
		}
	}
	
	private List<String> getSpouses(String personId, List<RelationVO> relationsList) {
		List<String> spousesList;
		String spouseId, person2ForPerson1RelId;
		float sequenceNo, randSequenceNo;
		
		spousesList = new ArrayList<String>();
		randSequenceNo = 1;
		for(RelationVO relationVO : relationsList) {
			person2ForPerson1RelId = Constants.RELATION_NAME_TO_ID_MAP.get(relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1));
			if (person2ForPerson1RelId == Constants.RELATION_NAME_HUSBAND || person2ForPerson1RelId == Constants.RELATION_NAME_WIFE) {
				if (relationVO.getSource().equals(personId)) {
					sequenceNo = relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1).equals("") ? randSequenceNo++ : Float.valueOf(relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1));
					spouseId = relationVO.getTarget();
				} else if (relationVO.getTarget().equals(personId)) {
					sequenceNo = relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2).equals("") ? randSequenceNo++ : Float.valueOf(relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2));
					spouseId = relationVO.getSource();
				} else {
					continue;
				}
				UtilFuncs.listSet(spousesList, sequenceNo, spouseId, null);
			}
		}
		LogManager.getLogger().debug(personId + "'s spouses: " +  spousesList.size());
		return spousesList.size() == 0 ? spousesList : spousesList.subList(1, spousesList.size());
	}
	
	private List<String> getKids(String parent1Id, String parent2Id, List<RelationVO> relationsList) {
		Map<String, Float> parent1SatisfiedKidsMap;
		List<String> parent2SatisfiedKidsList;
		List<Pair<String, Float>> kidsList;
		String kidId, person2ForPerson1RelId;
		float sequenceNo, randSequenceNo;
		
		parent1SatisfiedKidsMap = new HashMap<String, Float>();
		parent2SatisfiedKidsList = new ArrayList<String>();
		kidsList = new ArrayList<Pair<String, Float>>();
		randSequenceNo = 1;
		for(RelationVO relationVO : relationsList) {
			person2ForPerson1RelId = Constants.RELATION_NAME_TO_ID_MAP.get(relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1));
			if(relationVO.getSource().equals(parent1Id) && (person2ForPerson1RelId == Constants.RELATION_NAME_SON || person2ForPerson1RelId == Constants.RELATION_NAME_DAUGHTER)) {
				kidId = relationVO.getTarget();
				sequenceNo = relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1).equals("") ? randSequenceNo++ : Float.valueOf(relationVO.getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1));
				if (parent2SatisfiedKidsList.contains(kidId) && parent2Id != null) {
					kidsList.add(Pair.with(kidId, sequenceNo));
				}
				parent1SatisfiedKidsMap.put(kidId, sequenceNo);
			} else if((relationVO.getSource().equals(parent2Id) || parent2Id == null) && (person2ForPerson1RelId == Constants.RELATION_NAME_SON || person2ForPerson1RelId == Constants.RELATION_NAME_DAUGHTER)) {
				kidId = relationVO.getTarget();
				if (parent1SatisfiedKidsMap.containsKey(kidId) && parent2Id != null) {
					sequenceNo = parent1SatisfiedKidsMap.get(kidId);
					kidsList.add(Pair.with(kidId, sequenceNo));
				}
				parent2SatisfiedKidsList.add(kidId);
			}
		}
		if (parent2Id == null) {
			for (Map.Entry<String, Float>  parent1SatisfiedKidsMapEntry : parent1SatisfiedKidsMap.entrySet()) {
				if (!parent2SatisfiedKidsList.contains(parent1SatisfiedKidsMapEntry.getKey())) {
					kidsList.add(Pair.with(parent1SatisfiedKidsMapEntry.getKey(), parent1SatisfiedKidsMapEntry.getValue()));
				}
			}
		}
		Collections.sort(kidsList, (m1, m2) -> m1.getValue1().compareTo(m2.getValue1()));
		LogManager.getLogger().debug(parent1Id + "-" + parent2Id + "'s kids: " +  kidsList.size());
		return kidsList.stream().map(Pair<String, Float>::getValue0).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public GraphVO retrieveRoots(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
    	retrieveRelationsRequestVO.setMaxDepth(Constants.EXPORT_TREE_MAX_DEPTH);
    	retrieveRelationsRequestVO.setRequiredRelationsList(Constants.RELATION_NAME_LIST_PARENT);
		return retrieveTree(retrieveRelationsRequestVO);
	}
	
	public GraphVO retrieveParceners(RetrieveRelationsRequestVO retrieveRelationsRequestVO) {
		Person startPerson;
		List<RelatedPerson1VO> relatedPerson1VOList;
		RetrieveRelationsRequestVO retrieveRelationsRequestVO2;
		short depth;
		
    	startPerson = personRepository.findByIdAndTenant(retrieveRelationsRequestVO.getStartPersonId(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + retrieveRelationsRequestVO.getStartPersonId(), null));
    	depth = 1;
    	while((relatedPerson1VOList = retrieveRelatives(startPerson, Arrays.asList(Constants.RELATION_NAME_FATHER))).size() == 1) {
    		startPerson = relatedPerson1VOList.get(0).person;
    		depth++;
    	}
    	retrieveRelationsRequestVO2 = new RetrieveRelationsRequestVO();
    	retrieveRelationsRequestVO2.setStartPersonId(startPerson.getId());
    	retrieveRelationsRequestVO2.setMaxDepth(depth);
    	retrieveRelationsRequestVO2.setRequiredRelationsList(Arrays.asList(Constants.RELATION_NAME_SON));
		return retrieveTree(retrieveRelationsRequestVO2);
	}
	
	public RetrieveAppStartValuesResponseVO retrieveAppStartValues(Long tenantId) {
		RetrieveAppStartValuesResponseVO retrieveAppStartValuesResponseVO;
		Tenant tenant;
		
		retrieveAppStartValuesResponseVO = new RetrieveAppStartValuesResponseVO();
		retrieveAppStartValuesResponseVO.setDomainValueVOList(retrieveDomainValues());
		if (tenantId != null) {
    		tenant = tenantRepository.findById(tenantId)
    				.orElseThrow(() -> new AppException("Invalid Tenant Id " + tenantId, null));
    		retrieveAppStartValuesResponseVO.setInUseProject(tenant.getProjectId());
		}

		return retrieveAppStartValuesResponseVO;
	}
	
    private List<DomainValueVO> retrieveDomainValues() {
    	DomainValueVO domainValueVO;
    	List<DomainValueVO> domainValueVOList;
    	List<DomainValue> domainValueList;
    	DomainValueFlags domainValueFlags;
    	
    	domainValueFlags = new DomainValueFlags();
    	domainValueList = domainValueRepository.findAllByOrderByCategoryAscValueAsc();
    	domainValueVOList = new ArrayList<DomainValueVO>(domainValueList.size());
    	
    	for (DomainValue domainValue : domainValueList) {
    		domainValueVO = new DomainValueVO();
    		domainValueVOList.add(domainValueVO);
    		
    		domainValueVO.setId(domainValue.getId());
    		domainValueVO.setCategory(domainValue.getCategory());
    		domainValueVO.setValue(domainValue.getDvValue());
    		
    		domainValueFlags.setDomainValue(domainValue);
    		domainValueVO.setRelationGroup(domainValueFlags.getRelationGroup());
    		domainValueVO.setAttributeInUi(domainValueFlags.getAttributeInUi());
    		domainValueVO.setRepetitionType(domainValueFlags.getRepetitionType());
    		domainValueVO.setAttributeDomain(domainValueFlags.getAttributeDomain());
    		domainValueVO.setIsInputMandatory(domainValueFlags.getIsInputMandatory());
    		domainValueVO.setValidationJsRegEx(domainValueFlags.getValidationJsRegEx());
    		domainValueVO.setLanguageCode(domainValueFlags.getLanguageCode());
    		domainValueVO.setPrivacyRestrictionType(domainValueFlags.getPrivacyRestrictionType());
    		domainValueVO.setIsScriptConvertible(domainValueFlags.getIsScriptConvertible());
    	}
    	
    	return domainValueVOList;
    }
    
    public RetrievePersonAttributesResponseVO retrievePersonAttributes(long entityId) {
    	Person person;
    	List<AttributeValue> attributeValueList;
    	RetrievePersonAttributesResponseVO retrievePersonAttributesResponseVO;
		String gender, firstName, personLabel, label;
		DomainValue attributeValueDv;
    	
    	retrievePersonAttributesResponseVO = new RetrievePersonAttributesResponseVO();
		person = personRepository.findByIdAndTenant(entityId, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + entityId, null));
		retrievePersonAttributesResponseVO.setPhoto(person.getPhoto());
		retrievePersonAttributesResponseVO.setManageAccess(isAuthorisedToManage(person));
		attributeValueList = person.getAttributeValueList();
		retrievePersonAttributesResponseVO.setAttributeValueVOList(attributeValuesEntityToVo(attributeValueList, !isAuthorisedToManage(person)));
		
		// Determine Label
		gender = null;
		firstName = null;
		personLabel = null;
		for (AttributeValueVO aVVo : retrievePersonAttributesResponseVO.getAttributeValueVOList()) {
			if(aVVo.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_GENDER) {
				attributeValueDv = domainValueRepository.findById(Long.valueOf(aVVo.getAttributeValue()))
        				.orElseThrow(() -> new AppException("Invalid Attribute Value Dv Id " + aVVo.getAttributeValue(), null));
				gender = attributeValueDv.getDvValue().substring(0,1);	// TODO: Incorrect logic (In some languages, duplicates can be there; In some languages, a single character could be made up of multiple unicodes)
			} else if(aVVo.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME) {
				firstName = aVVo.getAvValue();
			} else if(aVVo.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_LABEL) {
				personLabel = aVVo.getAvValue();
			}
		}
		// The following expression is to be maintained in sync with that in PersonVO.determineLabel
		label =  "(" + entityId + "/" + (gender == null ? "" : gender) + ")" + (firstName == null ? "" : firstName) +
				(firstName == null || personLabel  == null ? "" : "/") + (personLabel == null ? "" : personLabel);
		retrievePersonAttributesResponseVO.setLabel(label);
		
		return retrievePersonAttributesResponseVO;
    }
    
    private boolean isAuthorisedToManage(Person person) {
    	DomainValue domainValue;
    	Optional<AttributeValue> attributeValueOpt;
    	
    	if (SecurityContext.getCurrentUser() == null) {
    		return false;
    	}
    	
    	if (!projectUserService.isAppReadOnly(SecurityContext.getCurrentTenant(), SecurityContext.getCurrentUser())) {
    		return true;	// Is a creator or collaborator and hence has manage access to the entire tenant
    	}
    	
    	domainValue = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_MANAGE_USER)
				.orElseThrow(() -> new AppException("Invalid Attribute Value Dv Id " + Constants.PERSON_ATTRIBUTE_DV_ID_MANAGE_USER, null));
    	attributeValueOpt = fetchAttribute(person, null, domainValue);
    	if (attributeValueOpt.isPresent() && Long.parseLong(attributeValueOpt.get().getAttributeValue()) == SecurityContext.getCurrentUserId()) {
			return true;	// The user has manage access to the person
		} else {
			return false;
		}
    	
    }
    
    private Boolean isPrivateAttributeValue(AttributeValue attributeValue, String privacyRestrictionType) {
    	switch(privacyRestrictionType) {
    	case Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_PUBLIC_ONLY:
    		return false;
    	case Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_PRIVATE_ONLY:
    		return true;
    	default: // Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE:
    		return attributeValue.getIsPrivate();
    	}
    }
    
    public RetrieveRelationAttributesResponseVO retrieveRelationAttributes(long entityId) {
    	Relation relation;
    	List<AttributeValue> attributeValueList;
    	RetrieveRelationAttributesResponseVO retrieveRelationAttributesResponseVO;
    	DomainValue domainValue;
    	AttributeValue attributeValue;
    	DomainValueFlags domainValueFlags;
    	String errorMessage;
    	
		relation = relationRepository.findByIdAndTenant(entityId, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Relation Id " + entityId, null));
		attributeValueList = relation.getAttributeValueList();
		
    	retrieveRelationAttributesResponseVO = new RetrieveRelationAttributesResponseVO();
    	retrieveRelationAttributesResponseVO.setPerson1Id(relation.getPerson1().getId());
    	
		domainValue =  domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)
				.orElseThrow(() -> new AppException("Domain Value missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, null));
    	attributeValue = fetchAttribute(null, relation, domainValue)
				.orElseThrow(() -> new AppException("Person 1 for Person 2 missing for relation " + relation.getId(), null));
    	errorMessage = "Invalid (Attribute Value) Dv Id " + attributeValue.getAttributeValue();
		domainValue = domainValueRepository.findById(Long.valueOf(attributeValue.getAttributeValue()))
				.orElseThrow(() -> new AppException(errorMessage, null));
    	domainValueFlags = new DomainValueFlags(domainValue);
    	retrieveRelationAttributesResponseVO.setRelationGroup(domainValueFlags.getRelationGroup());
    	
		domainValue =  domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER)
				.orElseThrow(() -> new AppException("Domain Value missing: " + Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, null));
    	attributeValue = fetchAttribute(relation.getPerson1(), null, domainValue)
				.orElseThrow(() -> new AppException("Gender missing for person " + relation.getPerson1().getId(), null));
    	retrieveRelationAttributesResponseVO.setPerson1GenderDVId(Long.valueOf(attributeValue.getAttributeValue()));
    	
    	attributeValue = fetchAttribute(relation.getPerson2(), null, domainValue)
				.orElseThrow(() -> new AppException("Gender missing for person " + relation.getPerson2().getId(), null));
    	retrieveRelationAttributesResponseVO.setPerson2GenderDVId(Long.valueOf(attributeValue.getAttributeValue()));
    	
    	retrieveRelationAttributesResponseVO.setAttributeValueVOList(attributeValuesEntityToVo(attributeValueList, false));
    	return retrieveRelationAttributesResponseVO;
    }
    
    private List<AttributeValueVO> attributeValuesEntityToVo(List<AttributeValue> attributeValueList, boolean isPublicOnly) {
    	List<AttributeValueVO> attributeValueVOList;
    	AttributeValueVO attributeValueVO;
    	DomainValueFlags domainValueFlags;
    	
    	domainValueFlags = new DomainValueFlags();
    	
    	attributeValueVOList = new ArrayList<AttributeValueVO>();
    	for(AttributeValue attributeValue : attributeValueList) {
    		
    		domainValueFlags.setDomainValue(attributeValue.getAttribute());
    		if (!domainValueFlags.getAttributeInUi().equals(Constants.FLAG_ATTRIBUTE_UI_INTERNAL) && (!isPrivateAttributeValue(attributeValue, domainValueFlags.getPrivacyRestrictionType()) || !isPublicOnly)) {
        		attributeValueVO = new AttributeValueVO();
        		attributeValueVOList.add(attributeValueVO);
        		attributeValueVO.setId(attributeValue.getId());
        		attributeValueVO.setAttributeDvId(attributeValue.getAttribute().getId());
        		attributeValueVO.setAttributeName(attributeValue.getAttribute().getDvValue());
        		attributeValueVO.setAttributeValue(attributeValue.getAttributeValue());
        		attributeValueVO.setTranslatedValue(attributeValue.getTranslation() == null ? null : attributeValue.getTranslation().getValue());
        		attributeValueVO.setValueApproximate(attributeValue.isValueApproximate());
        		attributeValueVO.setStartDate(attributeValue.getStartDate());
        		attributeValueVO.setEndDate(attributeValue.getEndDate());
				if (domainValueFlags.getPrivacyRestrictionType().equals(Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE)) {
					attributeValueVO.setIsPrivate(attributeValue.getIsPrivate());
				}
    		}
    	}
    	return attributeValueVOList;
    }
    
    public SaveAttributesResponseVO savePersonAttributes(SaveAttributesRequestVO saveAttributesRequestVO) {
    	Person person, source;
    	SaveAttributesResponseVO saveAttributesResponseVO;
    	
    	source = null;
    	if (saveAttributesRequestVO.getSourceId() != null) {
    		source = personRepository.findByIdAndTenant(saveAttributesRequestVO.getSourceId(), SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Source " + saveAttributesRequestVO.getSourceId(), null));
    	}
    	if (saveAttributesRequestVO.getEntityId() == Constants.NEW_ENTITY_ID) {
    		person = new Person(source);
    		person = personRepository.save(person);
    	}
    	else {
    		person = personRepository.findByIdAndTenant(saveAttributesRequestVO.getEntityId(), SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Person Id " + saveAttributesRequestVO.getEntityId(), null));
        	if (!isAuthorisedToManage(person)) {
        		throw new AppException("You are not authorised to modify this person", null);
        	}
    	}
    	if (saveAttributesRequestVO.getPhoto() != null) {	// When no fresh upload (browse & open), don't update existing photo with null
    		person.setPhoto(saveAttributesRequestVO.getPhoto());
    	}
    	
		saveAttributesResponseVO = new SaveAttributesResponseVO();
		saveAttributesResponseVO.setEntityId(person.getId());
		saveAttributesResponseVO.setInsertedAttributeValueIdList(saveAttributeValue(saveAttributesRequestVO.getAttributeValueVOList(), person, null, source));
		updateAttributeListInParentEntity(person, null);
    	return saveAttributesResponseVO;
    }
    
    public SaveAttributesResponseVO saveRelationAttributes(SaveAttributesRequestVO saveAttributesRequestVO) {
    	Relation relation = null;
    	SaveAttributesResponseVO saveAttributesResponseVO;
    	AttributeValue person1ForPerson2Av;
    	DomainValue domainValue;
    	DomainValueFlags domainValueFlags;
    	String relationGroup;
    	Person source;
    	
    	source = null;
    	if (saveAttributesRequestVO.getSourceId() != null) {
    		source = personRepository.findByIdAndTenant(saveAttributesRequestVO.getSourceId(), SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Source " + saveAttributesRequestVO.getSourceId(), null));
    	}
		relation = relationRepository.findByIdAndTenant(saveAttributesRequestVO.getEntityId(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Relation Id " + saveAttributesRequestVO.getEntityId(), null));
		domainValue =  domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, null));
		person1ForPerson2Av = fetchAttribute(null, relation, domainValue)
				.orElseThrow(() -> new AppException("Mandatory Attribute missing for relation " + saveAttributesRequestVO.getEntityId(), null));
		domainValue = domainValueRepository.findById(Long.valueOf(person1ForPerson2Av.getAttributeValue()))
				.orElseThrow(() -> new AppException("Invalid Attribute Value Dv Id " + person1ForPerson2Av.getAttributeValue(), null));
    	domainValueFlags = new DomainValueFlags(domainValue);
    	relationGroup = domainValueFlags.getRelationGroup();
    	for(AttributeValueVO attributeValueVO : saveAttributesRequestVO.getAttributeValueVOList()) {
    		if (attributeValueVO.getId() == null) {
    			throw new AppException("System error: Attribute with null id", null);
    		}
    		domainValue = domainValueRepository.findById(attributeValueVO.getAttributeDvId())
    				.orElseThrow(() -> new AppException("Invalid Attribute " + attributeValueVO.getAttributeDvId(), null));
    		domainValueFlags.setDomainValue(domainValue);
        	if (domainValueFlags.getRelationGroup() != null && !domainValueFlags.getRelationGroup().equals(relationGroup)) {
        		throw new AppException("Attribute " + domainValue.getValue() + " cannot be used for " + relationGroup + " relations.", null);
        	}
    	}
    	
		saveAttributesResponseVO = new SaveAttributesResponseVO();
		saveAttributesResponseVO.setEntityId(saveAttributesRequestVO.getEntityId());
		saveAttributesResponseVO.setInsertedAttributeValueIdList(saveAttributeValue(saveAttributesRequestVO.getAttributeValueVOList(), null, relation, source));
		updateAttributeListInParentEntity(null, relation);
    	return saveAttributesResponseVO;
    }

    private void updateAttributeListInParentEntity(Person person, Relation relation) {
    	// This method is required when the Child list in the Parent entity is used, after there are changes to the Child list.
    	// The Database is fine, but the Java Bean has become out of sync with the DB
    	
    	if (person != null) {
    		person.setAttributeValueList(attributeValueRepository.findByPerson(person));
    	} else { // relation != null
    		relation.setAttributeValueList(attributeValueRepository.findByRelation(relation));
    	}
    }
    
    private List<Long> saveAttributeValue(List<AttributeValueVO> attributeValueVOList, Person person, Relation relation, Person source) {
    	AttributeValue attributeValue, insertedAttributeValue, preModifyAttributeValue;
    	List<Long> incomingAttributeValueWithIdList, insertedAttributeValueIdList;
    	List<AttributeValue> toDeleteAttributeValueList;
    	DomainValueFlags domainValueFlags;
    	Translation translation;
    	String translatedValue;
    	DomainValue attributeDv;
    	
		toDeleteAttributeValueList = (person != null ? person.getAttributeValueList() : relation.getAttributeValueList());
		LogManager.getLogger().debug("1. Before update, no. of attributes in DB: " + (toDeleteAttributeValueList == null ? 0 : toDeleteAttributeValueList.size()));
    	incomingAttributeValueWithIdList = new ArrayList<Long>();
    	insertedAttributeValueIdList = new ArrayList<Long>();
    	domainValueFlags = new DomainValueFlags();
    	
    	for(AttributeValueVO attributeValueVO : attributeValueVOList) {
    		if (attributeValueVO.getId() == null) {
    			throw new AppException("System error: Attribute with null id", null);
    		}
    		attributeDv = domainValueRepository.findById(attributeValueVO.getAttributeDvId())
    				.orElseThrow(() -> new AppException("Invalid Attribute " + attributeValueVO.getAttributeDvId(), null));
    		domainValueFlags.setDomainValue(attributeDv);
			if (domainValueFlags.getPrivacyRestrictionType().equals(Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE) &&
					attributeValueVO.getIsPrivate() == null) {
				attributeValueVO.setIsPrivate(false);
			}
    		if (attributeValueVO.getId() < 1) {	// Insert New AV
    			insertedAttributeValue = insertAttributeValue(attributeValueVO, person, relation, source);
    			insertedAttributeValueIdList.add(insertedAttributeValue.getId());
				if (attributeValueVO.getTranslatedValue() != null) {	// Insert New Translation
					translation = new Translation(insertedAttributeValue, null, attributeValueVO.getTranslatedValue());
					translationRepository.save(translation);
				}
    		}
    		else {	// Modify Existing AV
    			incomingAttributeValueWithIdList.add(attributeValueVO.getId());
    			attributeValue = attributeValueRepository.findByIdAndTenant(attributeValueVO.getId(), SecurityContext.getCurrentTenant())
    					.orElseThrow(() -> new AppException("Invalid Attribute Value Id " + attributeValueVO.getId(), null));
        		if (attributeValueVO.getAttributeValue() == null || attributeValueVO.getAttributeValue().equals("") ||
        				domainValueFlags.getIsScriptConvertible() && !SecurityContext.getCurrentLanguageDvId().equals(Constants.DEFAULT_LANGUAGE_DV_ID) &&
        				(attributeValueVO.getTranslatedValue() == null || attributeValueVO.getTranslatedValue().equals(""))) {
        			throw new AppException("Attribute value (and its translation, if applicable) cannot be null", null);
        		}
    			if (attributeValueVO.getAttributeDvId() != attributeValue.getAttribute().getId()) {
    				throw new AppException("Invalid input from client.", null);
    			}
    			
    			if (!Objects.equals(attributeValueVO.getAttributeValue(), attributeValue.getAttributeValue()) ||
    					!Objects.equals(attributeValueVO.isValueApproximate(), attributeValue.isValueApproximate()) ||
    					!Objects.equals(attributeValueVO.getIsPrivate(), isPrivateAttributeValue(attributeValue, domainValueFlags.getPrivacyRestrictionType())) ||
    					!UtilFuncs.dateEquals(attributeValueVO.getStartDate(), attributeValue.getStartDate()) ||
    					!UtilFuncs.dateEquals(attributeValueVO.getEndDate(), attributeValue.getEndDate())) {	// Modify Default-Lang
    				preModifyAttributeValue = new AttributeValue(attributeValue);
    				preModifyAttributeValue.setOverwrittenBy(attributeValue);
    				attributeValueRepository.save(preModifyAttributeValue);
    				attributeValue.setAttributeValue(attributeValueVO.getAttributeValue());
    				attributeValue.setValueApproximate(attributeValueVO.isValueApproximate());
    				attributeValue.setStartDate(attributeValueVO.getStartDate());
    				attributeValue.setEndDate(attributeValueVO.getEndDate());
    				if (domainValueFlags.getPrivacyRestrictionType().equals(Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE)) {
    					attributeValue.setIsPrivate(attributeValueVO.getIsPrivate());
    				}
    				attributeValue.setSource(source);
    				attributeValueRepository.save(attributeValue);
    			}
    			translatedValue = attributeValue.getTranslation() == null ? null : attributeValue.getTranslation().getValue();
    			if (!Objects.equals(attributeValueVO.getTranslatedValue(), translatedValue)) {
    				if (attributeValueVO.getTranslatedValue() == null) {	// Delete Translation
    					throw new AppException("Translated Value cannot be removed", null);
    				} else if (translatedValue == null) {	// Insert New Translation
    					translation = new Translation(attributeValue, null, attributeValueVO.getTranslatedValue());
    					translationRepository.save(translation);
    				} else {	// Modify Existing Translation
    					attributeValue.getTranslation().setValue(attributeValueVO.getTranslatedValue());
    				}
    			}
    		}
    	}
    	
    	LogManager.getLogger().debug("2. Before update, no. of attributes in DB: " + (toDeleteAttributeValueList == null ? 0 : toDeleteAttributeValueList.size()));
		if (toDeleteAttributeValueList != null) {	// Delete AV
	    	for(AttributeValue toDeleteAttributeValue : toDeleteAttributeValueList) {
	    		domainValueFlags.setDomainValue(toDeleteAttributeValue.getAttribute());
	    		if (!domainValueFlags.getAttributeInUi().equals(Constants.FLAG_ATTRIBUTE_UI_INTERNAL) && !incomingAttributeValueWithIdList.contains(toDeleteAttributeValue.getId())) {
	    			toDeleteAttributeValue.setDeleter(SecurityContext.getCurrentUser());
	    			toDeleteAttributeValue.setDeletedAt(new Timestamp(System.currentTimeMillis()));
					attributeValueRepository.save(toDeleteAttributeValue);
					 // Soft delete of parent (AttributeValue); Children (Translation) left untouched
	    		}
	    	}
		}
		
		return insertedAttributeValueIdList;
    }

    private AttributeValue insertAttributeValue(AttributeValueVO attributeValueVO, Person person, Relation relation, Person source) {
    	AttributeValue attributeValue;
    	DomainValue attributeDv;
    	DomainValueFlags domainValueFlags;
    	
    	attributeValue = new AttributeValue(source);
		attributeDv = domainValueRepository.findById(attributeValueVO.getAttributeDvId())
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + attributeValueVO.getAttributeDvId(), null));
    	domainValueFlags = new DomainValueFlags(attributeDv);
		attributeValue.setAttribute(attributeDv);
		attributeValue.setAttributeValue(attributeValueVO.getAttributeValue());
		attributeValue.setPerson(person);
		attributeValue.setRelation(relation);
		attributeValue.setValueApproximate(attributeValueVO.isValueApproximate());
		attributeValue.setStartDate(attributeValueVO.getStartDate());
		attributeValue.setEndDate(attributeValueVO.getEndDate());
		if (domainValueFlags.getPrivacyRestrictionType().equals(Constants.FLAG_ATTRIBUTE_PRIVACY_RESTRICTION_INDIVIDUAL_CHOICE)) {
			attributeValue.setIsPrivate(attributeValueVO.getIsPrivate());
		}
		return attributeValueRepository.save(attributeValue);
    }

    public SearchResultsVO searchPerson(PersonSearchCriteriaVO personSearchCriteriaVO) {
    	List<AttributeValueVO> attributeValueVOList;
    	StringBuilder querySB;
    	List<Person> personList;
    	int searchResultsCount, attributesCount;
    	SearchResultsVO searchResultsVO;
    	Map<Long, Integer> attributeVsColumnMap;
    	List<List<String>> searchResultsList;
    	List<String> personAttributesList;
    	DomainValueFlags domainValueFlags;
    	String attrVal, parentNamesSsv, spouseNamesSsv, childNamesSsv, ntrmdtQuery;
    	DomainValue domainValue;
    	boolean isPublicOnly;
    	List<DomainValue> domainValueList;
    	int personIdInd, parentNamesInd, spouseNamesInd, childNamesInd;
    	
    	attributeValueVOList = personSearchCriteriaVO.getAttributeValueVOList();
    	domainValueFlags = new DomainValueFlags();
    	querySB = new StringBuilder();
    	querySB.append("SELECT * FROM person p LEFT OUTER JOIN tenant t ON p.tenant_fk = t.id WHERE p.overwritten_by_fk IS NULL AND p.deleter_fk IS NULL");
    	// TODO: AOP to take care of the following if block
    	if (SecurityContext.getCurrentTenantId() != null) {
    		querySB.append(" AND p.tenant_fk = ");
    		querySB.append(SecurityContext.getCurrentTenantId());
    	}
    	for(AttributeValueVO attributeValueVO : attributeValueVOList) {
    		domainValue = domainValueRepository.findById(attributeValueVO.getAttributeDvId())
    				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + attributeValueVO.getAttributeDvId(), null));
			domainValueFlags.setDomainValue(domainValue);
    		if (domainValue.getCategory().equals(Constants.CATEGORY_PERSON_ATTRIBUTE) && (attributeValueVO.getAttributeDvId() != Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME ||
    				!attributeValueVO.getAttributeValue().equalsIgnoreCase("X") && !attributeValueVO.getAttributeValue().equalsIgnoreCase("Y"))) {
	    		querySB.append(" AND (");
	    		querySB.append(buildQueryOneAv(attributeValueVO.getAttributeDvId(), attributeValueVO.getAttributeValue(), personSearchCriteriaVO.isLenient(), "person_fk = p.id"));
	    		if (attributeValueVO.isMaybeNotRegistered()) {
		    		querySB.append(" OR NOT ");
		    		querySB.append(buildQueryOneAv(attributeValueVO.getAttributeDvId(), "@Spl: ", personSearchCriteriaVO.isLenient(), "person_fk = p.id"));
	    		}
	    		querySB.append(")");
    		}
    		else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_PERSON_ID) {
	    		querySB.append(" AND p.id = ");
	    		querySB.append(attributeValueVO.getAttributeValue());
    		}
    		else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_PARENTS) {
	    		querySB.append(" AND (");
    			ntrmdtQuery = "EXISTS (SELECT 1 FROM relation r WHERE r.person_2_fk = p.id AND r.overwritten_by_fk IS NULL AND r.deleter_fk IS NULL AND ((" +
    			    		buildQueryOneAv(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, "@Spl: AND av.attribute_value IN ('" + Constants.RELATION_NAME_FATHER + "', '" + Constants.RELATION_NAME_MOTHER + "')", personSearchCriteriaVO.isLenient(),  "relation_fk = r.id");
	    		querySB.append(ntrmdtQuery);
	    		querySB.append(" AND ");
	    		querySB.append(buildQueryOneAv(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, attributeValueVO.getAttributeValue(), attributeValueVO.getAttributeValueList(), personSearchCriteriaVO.isLenient(),  "person_fk = r.person_1_fk"));
	    		querySB.append(")))");
	    		if (attributeValueVO.isMaybeNotRegistered()) {
		    		querySB.append(" OR NOT ");
		    		querySB.append(ntrmdtQuery);
		    		querySB.append(")))");
	    		}
	    		querySB.append(")");
    		}
    		else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_SPOUSES) {
	    		querySB.append(" AND (");
	    		ntrmdtQuery = "EXISTS (SELECT 1 FROM relation r WHERE p.id IN (r.person_1_fk, r.person_2_fk) AND r.overwritten_by_fk IS NULL AND r.deleter_fk IS NULL AND ((" +
	    			    	buildQueryOneAv(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, Constants.RELATION_NAME_HUSBAND, personSearchCriteriaVO.isLenient(),  "relation_fk = r.id");
	    		querySB.append(ntrmdtQuery);
	    		querySB.append(" AND ");
	    		querySB.append(buildQueryOneAv(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, attributeValueVO.getAttributeValue(), attributeValueVO.getAttributeValueList(), personSearchCriteriaVO.isLenient(),  "person_fk = CASE WHEN p.id = r.person_1_fk THEN r.person_2_fk ELSE r.person_1_fk END"));
	    		querySB.append(")))");
	    		if (attributeValueVO.isMaybeNotRegistered()) {
		    		querySB.append(" OR NOT ");
		    		querySB.append(ntrmdtQuery);
		    		querySB.append(")))");
	    		}
	    		querySB.append(")");
    		} else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_CHILDREN) {
	    		querySB.append(" AND (");
    			ntrmdtQuery = "EXISTS (SELECT 1 FROM relation r WHERE r.person_1_fk = p.id AND r.overwritten_by_fk IS NULL AND r.deleter_fk IS NULL AND ((" +
    			    		buildQueryOneAv(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, "@Spl: AND av.attribute_value IN ('" + Constants.RELATION_NAME_FATHER + "', '" + Constants.RELATION_NAME_MOTHER + "')", personSearchCriteriaVO.isLenient(),  "relation_fk = r.id");
	    		querySB.append(ntrmdtQuery);
	    		querySB.append(" AND ");
	    		querySB.append(buildQueryOneAv(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, attributeValueVO.getAttributeValue(), attributeValueVO.getAttributeValueList(), personSearchCriteriaVO.isLenient(),  "person_fk = r.person_2_fk"));
	    		querySB.append(")))");
	    		if (attributeValueVO.isMaybeNotRegistered()) {
		    		querySB.append(" OR NOT ");
		    		querySB.append(ntrmdtQuery);
		    		querySB.append(")))");
	    		}
	    		querySB.append(")");
    		} else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_SIBLINGS) {
	    		querySB.append(" AND (");
	    		ntrmdtQuery = "EXISTS (SELECT 1 FROM relation r WHERE r.person_2_fk = p.id AND " +
	    			    	buildQueryOneAv(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, "@Spl: AND av.attribute_value IN ('" + Constants.RELATION_NAME_FATHER + "', '" + Constants.RELATION_NAME_MOTHER + "')", personSearchCriteriaVO.isLenient(),  "relation_fk = r.id") +
	    			    	" AND (EXISTS (SELECT 1 FROM relation r2 WHERE r2.person_1_fk = r.person_1_fk AND r2.person_2_fk <> p.id AND " +
	    			    	buildQueryOneAv(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, "@Spl: AND av.attribute_value IN ('" + Constants.RELATION_NAME_FATHER + "', '" + Constants.RELATION_NAME_MOTHER + "')", personSearchCriteriaVO.isLenient(),  "relation_fk = r2.id");
	    		querySB.append(ntrmdtQuery);
	    		querySB.append(" AND ");
	    		querySB.append(buildQueryOneAv(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, attributeValueVO.getAttributeValue(), attributeValueVO.getAttributeValueList(), personSearchCriteriaVO.isLenient(),  "person_fk = r2.person_2_fk"));
	    		querySB.append(")))");
	    		if (attributeValueVO.isMaybeNotRegistered()) {
		    		querySB.append(" OR NOT ");
		    		querySB.append(ntrmdtQuery);
		    		querySB.append(")))");
	    		}
	    		querySB.append(")");
    		} else if (attributeValueVO.getAttributeDvId() == Constants.PERSON_ATTRIBUTE_DV_ID_ANY_NAME) {
	    		querySB.append(" AND (");
	    		for (long attributeDvId : Constants.PERSON_ATTRIBUTE_DV_IDS_ARRAY_NAME) {
		    		querySB.append(buildQueryOneAv(attributeDvId, attributeValueVO.getAttributeValue(), personSearchCriteriaVO.isLenient(), "person_fk = p.id"));
		    		querySB.append(" OR ");
	    		}
				querySB.delete(querySB.length() - 4, querySB.length());
	    		querySB.append(")");
    		}

    	}
		querySB.append(" ORDER BY p.id;");	// TODO Order by some match score
    	
    	personList = personRepository.executeDynamicQuery(querySB.toString());
    	
    	searchResultsVO = new SearchResultsVO();
    	searchResultsVO.setQueryToDb(querySB.toString());
    	if (personList.size() == 0) {
    		return searchResultsVO;
    	}
    	searchResultsVO.setCountInDb(personList.size());
    	
    	attributeVsColumnMap = new HashMap<Long, Integer>();
    	searchResultsList = new ArrayList<List<String>>(Constants.SEARCH_RESULTS_MAX_COUNT);
    	personAttributesList = new ArrayList<String>(); // For Header
    	searchResultsList.add(personAttributesList);
    	personIdInd = -1;
    	parentNamesInd = -1;
    	spouseNamesInd = -1;
    	childNamesInd = -1;
    	attributesCount = 0;
    	domainValueList = domainValueRepository.findByCategoryOrderByValueAsc(Constants.CATEGORY_ADDITIONAL_PERSON_ATTRIBUTE);
    	for (DomainValue dV : domainValueList) {
			domainValueFlags.setDomainValue(dV);
			if (domainValueFlags.getSearchResultColInd() != null) {
				UtilFuncs.listSet(personAttributesList, domainValueFlags.getSearchResultColInd(), dV.getDvValue(), null);
				attributesCount++;
				if (dV.getId() == Constants.PERSON_ATTRIBUTE_DV_ID_PERSON_ID) {
					personIdInd = domainValueFlags.getSearchResultColInd();
				} else if (dV.getId() == Constants.PERSON_ATTRIBUTE_DV_ID_PARENTS) {
					parentNamesInd = domainValueFlags.getSearchResultColInd();
				} else if (dV.getId() == Constants.PERSON_ATTRIBUTE_DV_ID_SPOUSES) {
					spouseNamesInd = domainValueFlags.getSearchResultColInd();
				} else if (dV.getId() == Constants.PERSON_ATTRIBUTE_DV_ID_CHILDREN) {
					childNamesInd = domainValueFlags.getSearchResultColInd();
				} else {
					throw new AppException("Unhandled additional person attribute " + dV.getValue(), null);
				}
			}
    	}
    	searchResultsCount = 0;
    	
    	for(Person person : personList) {
    		isPublicOnly = !isAuthorisedToManage(person);
    		searchResultsCount++;
    		personAttributesList = new ArrayList<String>();
    		searchResultsList.add(personAttributesList);
    		for (AttributeValue attributeValue : person.getAttributeValueList()) {
				domainValueFlags.setDomainValue(attributeValue.getAttribute());
    			if(serviceParts.isCurrentValidAttributeValue(attributeValue) && (!isPrivateAttributeValue(attributeValue, domainValueFlags.getPrivacyRestrictionType()) || !isPublicOnly)) {
    				if (domainValueFlags.getAttributeDomain() == null || domainValueFlags.getAttributeDomain().equals("")) {
    					attrVal = attributeValue.getAvValue();
    				}
    				else {
    					domainValue = domainValueRepository.findById(Long.valueOf(attributeValue.getAvValue()))
    							.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + attributeValue.getAvValue(), null));
    					attrVal = domainValue.getDvValue();
    				}
	    			if (attributeVsColumnMap.containsKey(attributeValue.getAttribute().getId())) {
	    				UtilFuncs.listSet(personAttributesList, attributeVsColumnMap.get(attributeValue.getAttribute().getId()), attrVal, "");
	    			}
	    			else {
	    				attributeVsColumnMap.put(attributeValue.getAttribute().getId(), attributesCount);
	    				UtilFuncs.listSet(searchResultsList.get(0), attributesCount, attributeValue.getAttribute().getDvValue(), "");
	    				UtilFuncs.listSet(personAttributesList, attributesCount, attrVal, "");
	    				attributesCount++;
	    			}
    			}
    		}
    		
    		// Add parents, spouses & children
			parentNamesSsv = "";
			spouseNamesSsv = "";
			childNamesSsv = "";
    		for (Map.Entry<Person, AttributeValue> relativeAttributeEntry : retrieveRelativesAndAttributes(person, Constants.RELATION_NAME_LIST_PARENT, Arrays.asList(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME))) {
    			parentNamesSsv += "/" + relativeAttributeEntry.getValue().getAvValue();
    		}

    		for (Map.Entry<Person, AttributeValue> relativeAttributeEntry : retrieveRelativesAndAttributes(person, Constants.RELATION_NAME_LIST_SPOUSE, Arrays.asList(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME))) {
    			spouseNamesSsv += "/" + relativeAttributeEntry.getValue().getAvValue();
    		}

    		for (Map.Entry<Person, AttributeValue> relativeAttributeEntry : retrieveRelativesAndAttributes(person, Constants.RELATION_NAME_LIST_CHILD, Arrays.asList(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME))) {
    			childNamesSsv += "/" + relativeAttributeEntry.getValue().getAvValue();
    		}

    		personAttributesList.set(personIdInd, String.valueOf(person.getId()));
    		personAttributesList.set(parentNamesInd, parentNamesSsv);
    		personAttributesList.set(spouseNamesInd, spouseNamesSsv);
    		personAttributesList.set(childNamesInd, childNamesSsv);

    		if (searchResultsCount == Constants.SEARCH_RESULTS_MAX_COUNT) {
    			break;
    		}
    	}
    	for (List<String> attributesList : searchResultsList) { // Make all attributes list to be of uniform size
    		if (attributesList.size() < attributesCount) {
    			UtilFuncs.listSet(attributesList, attributesCount - 1, "", "");
    		}
    	}
		searchResultsVO.setResultsList(searchResultsList.subList(0, searchResultsCount + 1));
    	return searchResultsVO;
    }

	private String buildQueryOneAv(long attributeDvId, String attributeValue, boolean isLenient, String avCriteria) {
		return buildQueryOneAv(attributeDvId, attributeValue, null, isLenient, avCriteria);
	}
	
	private String buildQueryOneAv(long attributeDvId, String attributeValue, List<String> attributeValueList, boolean isLenient, String avCriteria) {
    	StringBuilder querySB;
    	DomainValue domainValue;
    	DomainValueFlags domainValueFlags;
    	
    	// TODO attributeValueList is currently used on-need basis
    	
		querySB = new StringBuilder();
    	domainValueFlags = new DomainValueFlags();
		querySB.append(" EXISTS (SELECT 1 FROM attribute_value av WHERE av.overwritten_by_fk IS NULL AND av.deleter_fk IS NULL AND av.attribute_fk = ");
		querySB.append(attributeDvId);
		querySB.append(" AND av.");
		querySB.append(avCriteria);
		domainValue = domainValueRepository.findById(Long.valueOf(attributeDvId))
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + attributeDvId, null));
		domainValueFlags.setDomainValue(domainValue);
		if (attributeValue != null && attributeValue.startsWith("@Spl:")) {
			querySB.append(attributeValue.substring(5));
		} else {
			querySB.append(" AND (");
			if (domainValueFlags.getIsScriptConvertible() != null && domainValueFlags.getIsScriptConvertible()) {	// Beware: PostgreSQL specific syntax below
				if (isLenient) {
					if (attributeValueList == null) {
						attributeValueList = new ArrayList<String>();
						attributeValueList.add(attributeValue);
					}
					querySB.append("(");
					for (String av : attributeValueList) {
						for (String alternative : UtilFuncs.normaliseForSearch(av)) {
							querySB.append(" av.normalised_value LIKE '%");
							querySB.append(alternative);
							querySB.append("%' OR");
						}
					}
					querySB.delete(querySB.length() - 3, querySB.length());
					querySB.append(")");
				} else {
					querySB.append(" LOWER(av.attribute_value) LIKE '%");
					querySB.append(attributeValue.toLowerCase());
					querySB.append("%'");
				}
			} else {
				querySB.append(" LOWER(av.attribute_value) = '");
				querySB.append(attributeValue.toLowerCase());
				querySB.append("'");
			}
			if (domainValueFlags.getIsScriptConvertible() && !SecurityContext.getCurrentLanguageDvId().equals(Constants.DEFAULT_LANGUAGE_DV_ID)) {
				querySB.append(" OR EXISTS (SELECT 1 FROM translation t WHERE t.attribute_value_fk = av.id AND LOWER(t.value) LIKE '%");	// Beware: PostgreSQL specific syntax
				querySB.append(attributeValue.toLowerCase());
				querySB.append("%')");
			}
			querySB.append(")");
		}
		querySB.append(")");
		return querySB.toString();
	}
	
    private List<Map.Entry<Person, AttributeValue>> retrieveRelativesAndAttributes(Person forPerson, List<String> requiredRelationTypesList, List<Long> requiredAttributeTypesList) {
    	List<Map.Entry<Person, AttributeValue>> personAttributeValueList;
    	
    	personAttributeValueList = new ArrayList<Map.Entry<Person, AttributeValue>>();
    	for (RelatedPerson1VO relatedPerson1VO : retrieveRelatives(forPerson, requiredRelationTypesList)) {
    		for (AttributeValue attributeValue : relatedPerson1VO.person.getAttributeValueList()) {
    			if (requiredAttributeTypesList.contains(attributeValue.getAttribute().getId()) &&
    					serviceParts.isCurrentValidAttributeValue(attributeValue)) {
    				personAttributeValueList.add(new AbstractMap.SimpleEntry<Person, AttributeValue>(relatedPerson1VO.person, attributeValue));
    			}
    		}
    	}
    	return personAttributeValueList;
    }
    
    private List<RelatedPerson1VO> retrieveRelatives(Person forPerson, List<String> requiredRelationTypesList) {
    	List<RelatedPerson1VO> relatedPerson1VOList;
    	List<Relation> relationList;
    	RelatedPerson1VO relatedPerson1VO;
    	
    	relatedPerson1VOList = new ArrayList<RelatedPerson1VO>();
    	relationList = relationRepository.findByPerson1(forPerson);
    	relationList.addAll(relationRepository.findByPerson2(forPerson));
    	for (Relation relation : relationList) {
    		relatedPerson1VO = getOtherPerson(relation, forPerson);
			if (requiredRelationTypesList.contains(relatedPerson1VO.relationDvId)) {
				relatedPerson1VO.relation = relation;
   				relatedPerson1VOList.add(relatedPerson1VO);
    		}
    	}
    	Collections.sort(relatedPerson1VOList);

    	return relatedPerson1VOList;
    }
    
    private RelatedPerson1VO getOtherPerson(Relation relation, Person forPerson) {
    	long reqdAttributeDvId1, reqdAttributeDvId2;
    	RelatedPerson1VO relatedPerson1VO;
    	
    	relatedPerson1VO = serviceParts.new RelatedPerson1VO();
    	if (relation.getPerson1().equals(forPerson)) {
    		relatedPerson1VO.person = relation.getPerson2();
    		reqdAttributeDvId1 = Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1;
    		reqdAttributeDvId2 = Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1;
    	}
    	else {
    		relatedPerson1VO.person = relation.getPerson1();
    		reqdAttributeDvId1 = Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2;
    		reqdAttributeDvId2 = Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON1_FOR_PERSON2;
    	}
    	relatedPerson1VO.seqNo = 1D;
		for (AttributeValue attributeValue : relation.getAttributeValueList()) {
			if (attributeValue.getAttribute().getId() == reqdAttributeDvId1 &&
					serviceParts.isCurrentValidAttributeValue(attributeValue)) {
				relatedPerson1VO.relationDvId = attributeValue.getAttributeValue();
			}
			if (attributeValue.getAttribute().getId() == reqdAttributeDvId2 &&
					serviceParts.isCurrentValidAttributeValue(attributeValue)) {
				relatedPerson1VO.seqNo = Double.parseDouble(attributeValue.getAttributeValue());
			}
		}
		return relatedPerson1VO;
    }
    
    public List<String> retrieveGendersOfPersons(List<Long> personsList) {
    	List<String> gendersOfPersonsList;
    	Person person;
    	AttributeValue genderAv;
    	DomainValue attributeDv;
    	
    	gendersOfPersonsList = new ArrayList<String>(personsList.size());
    	for (long personId : personsList) {
        	person = personRepository.findByIdAndTenant(personId, SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Person Id " + personId, null));
			attributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER)
					.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, null));
			genderAv = fetchAttribute(person, null, attributeDv)
					.orElseThrow(() -> new AppException("Invalid gender for " + personId, null));
			gendersOfPersonsList.add(genderAv.getAttributeValue());
    	}
    	return gendersOfPersonsList;
    }
    
    public RelationVO saveRelation(RelatedPersonsVO saveRelationRequestVO) {
    	// Person 1 is expected to be one of Father, Mother, Husband
    	Person person1, person2, source;
    	Relation relation;
    	AttributeValue attributeValue1, attributeValue2, genderAv;
    	DomainValue attributeDv;
    	List<RelationVO> relationVOList;
    	
    	if (relationRepository.findRelationGivenPersons(saveRelationRequestVO.getPerson1Id(), saveRelationRequestVO.getPerson2Id(), SecurityContext.getCurrentTenantId()) != null) {
    		throw new AppException(saveRelationRequestVO.getPerson1Id() + " and " + saveRelationRequestVO.getPerson2Id() + " are already related.", null);
    	}
    	person1 = personRepository.findByIdAndTenant(saveRelationRequestVO.getPerson1Id(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + saveRelationRequestVO.getPerson1Id(), null));
    	person2 = personRepository.findByIdAndTenant(saveRelationRequestVO.getPerson2Id(), SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + saveRelationRequestVO.getPerson2Id(), null));
    	source = null;
    	if (saveRelationRequestVO.getSourceId() != null) {
    		source = personRepository.findByIdAndTenant(saveRelationRequestVO.getSourceId(), SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Source " + saveRelationRequestVO.getSourceId(), null));
    	}

    	relation = new Relation(person1, person2, source);
    	relation = relationRepository.save(relation);

    	attributeValue1 = new AttributeValue(source);
		attributeDv = domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, null));
		attributeValue1.setAttribute(attributeDv);
		attributeValue1.setAttributeValue(saveRelationRequestVO.getPerson1ForPerson2());
		attributeValue1.setRelation(relation);
		attributeValueRepository.save(attributeValue1);
		
		attributeValue2 = new AttributeValue(source);
		attributeDv = domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, null));
		attributeValue2.setAttribute(attributeDv);
		if (saveRelationRequestVO.getPerson1ForPerson2().equals(Constants.RELATION_NAME_MOTHER) || saveRelationRequestVO.getPerson1ForPerson2().equals(Constants.RELATION_NAME_FATHER)) {
			attributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER)
					.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, null));
			genderAv = fetchAttribute(person2, null, attributeDv)
					.orElseThrow(() -> new AppException("Invalid gender for " + saveRelationRequestVO.getPerson2Id(), null));
			if (genderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
				attributeValue2.setAttributeValue(Constants.RELATION_NAME_SON);
			} else if (genderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE)) {
				attributeValue2.setAttributeValue(Constants.RELATION_NAME_DAUGHTER);
			} else {
				throw new AppException("Incomplete support for Gender " + genderAv.getAttributeValue(), null);
			}
		} else if (saveRelationRequestVO.getPerson1ForPerson2().equals(Constants.RELATION_NAME_HUSBAND)) {
			attributeValue2.setAttributeValue(Constants.RELATION_NAME_WIFE);
		}
		attributeValue2.setRelation(relation);
		attributeValueRepository.save(attributeValue2);
		
		relation.setAttributeValueList(new ArrayList<AttributeValue>(Arrays.asList(attributeValue1, attributeValue2)));
    	relationVOList = new ArrayList<RelationVO>();
		serviceParts.addToRelationVOList(relationVOList, relation, null, false);
    	return relationVOList.get(0);
    }
    
    public void saveOtherRelation(SaveOtherRelationRequestVO saveOtherRelationRequestVO) {
    	Person source;
    	
    	if (relationRepository.findRelationGivenPersons(saveOtherRelationRequestVO.getPerson1Id(), saveOtherRelationRequestVO.getPerson2Id(), SecurityContext.getCurrentTenantId()) != null) {
    		throw new AppException(saveOtherRelationRequestVO.getPerson1Id() + " and " + saveOtherRelationRequestVO.getPerson2Id() + " are already related DIRECTly.", null);
    	}
    	source = null;
    	if (saveOtherRelationRequestVO.getSourceId() != null) {
    		source = personRepository.findByIdAndTenant(saveOtherRelationRequestVO.getSourceId(), SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Source " + saveOtherRelationRequestVO.getSourceId(), null));
    	}
    	
    	autoParents(saveOtherRelationRequestVO.getOtherRelationTypeDvId(), saveOtherRelationRequestVO.getPerson1Id(), saveOtherRelationRequestVO.getOtherRelationVia1DvId(), saveOtherRelationRequestVO.getPerson2Id(), saveOtherRelationRequestVO.getOtherRelationVia2DvId(), source, 0);
    }

    private void autoParents(long otherRelationTypeDvId, long person1Id, long otherRelationVia1DvId, long person2Id, long otherRelationVia2DvId, Person source, int recursionLevel) {
    	Person person1, person2;
    	List<RelatedPerson1VO> relatedPerson1VOList11, relatedPerson1VOList12, relatedPerson1VOList21, relatedPerson1VOList22;
    	long newPersonId, newRelationType, person1ParentId, person2ParentId;
    	Long sourceId;
    	Pair<Long, Long> parentTypePair;
    	
    	if (recursionLevel > 15) {
    		throw new AppException("Too much hierarchy to handle.", null);
    	}
    	System.out.println("autoParents ==> " + recursionLevel + "::" + otherRelationTypeDvId + "::" + person1Id + "::" + otherRelationVia1DvId + "::" + person2Id + "::" + otherRelationVia2DvId);
    	sourceId = source == null ? null : source.getId();
    	person1 = personRepository.findByIdAndTenant(person1Id, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + person1Id, null));
    	person2 = personRepository.findByIdAndTenant(person2Id, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + person2Id, null));
    	if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_SIBLING) {
    		relatedPerson1VOList11 = retrieveRelatives(person1, Arrays.asList(Constants.RELATION_NAME_FATHER));
    		relatedPerson1VOList12 = retrieveRelatives(person1, Arrays.asList(Constants.RELATION_NAME_MOTHER));
    		relatedPerson1VOList21 = retrieveRelatives(person2, Arrays.asList(Constants.RELATION_NAME_FATHER));
    		relatedPerson1VOList22 = retrieveRelatives(person2, Arrays.asList(Constants.RELATION_NAME_MOTHER));
    		if (relatedPerson1VOList11.size() > 1 || relatedPerson1VOList12.size() > 1 || relatedPerson1VOList21.size() > 1 || relatedPerson1VOList22.size() > 1) {
    			throw new AppException("Cannot automatically add required parents hierarchy. You have to manually establish this relation.", null);
    		}
    		if ((relatedPerson1VOList11.size() > 0 || relatedPerson1VOList12.size() > 0) && (relatedPerson1VOList21.size() > 0 || relatedPerson1VOList22.size() > 0)) {
    			if (relatedPerson1VOList11.size() > 0 && relatedPerson1VOList21.size() > 0 && relatedPerson1VOList11.get(0).person.getId() == relatedPerson1VOList21.get(0).person.getId() ||
    					relatedPerson1VOList12.size() > 0 && relatedPerson1VOList22.size() > 0 && relatedPerson1VOList12.get(0).person.getId() == relatedPerson1VOList22.get(0).person.getId()) {
        			throw new AppException("The required relation seems to exist already.", null);
    			} else if (recursionLevel == 0) {
        			throw new AppException("Cannot automatically add required parents hierarchy. You have to manually establish this relation.", null);
    			} else {
	    			autoParents(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, person1Id, otherRelationVia1DvId, person2Id, otherRelationVia2DvId, source, recursionLevel);
	    			return;
    			}
    		}
    		if (relatedPerson1VOList11.size() == 0 && relatedPerson1VOList12.size() == 0 && relatedPerson1VOList21.size() == 0 && relatedPerson1VOList22.size() == 0) {
    			newPersonId = savePersonAttributes(new SaveAttributesRequestVO(
    					-1L,
    					Arrays.asList(
    							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, "X"),
    							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, Constants.GENDER_NAME_MALE)
    							),
    					sourceId
    					)).getEntityId();
    			generateParentRelation(person1Id, newPersonId, Constants.RELATION_NAME_FATHER, recursionLevel>0, source);
    			generateParentRelation(person2Id, newPersonId, Constants.RELATION_NAME_FATHER, recursionLevel>0, source);
    		} else {
    			if (relatedPerson1VOList11.size() == 1) { // Person1's father is the father of Person2
        			generateParentRelation(person2Id, relatedPerson1VOList11.get(0).person.getId(), Constants.RELATION_NAME_FATHER, recursionLevel>0, source);
	    		}
    			if (relatedPerson1VOList12.size() == 1) { // Person1's mother is the mother of Person2
        			generateParentRelation(person2Id, relatedPerson1VOList12.get(0).person.getId(), Constants.RELATION_NAME_MOTHER, recursionLevel>0, source);
	    		}
    			if (relatedPerson1VOList21.size() == 1) { // Person2's father is the father of Person1
        			generateParentRelation(person1Id, relatedPerson1VOList21.get(0).person.getId(), Constants.RELATION_NAME_FATHER, recursionLevel>0, source);
	    		}
    			if (relatedPerson1VOList22.size() == 1) { // Person2's mother is the mother of Person1
    				generateParentRelation(person1Id, relatedPerson1VOList22.get(0).person.getId(), Constants.RELATION_NAME_MOTHER, recursionLevel>0, source);
	    		}
    		}
    	} else {	// otherRelationTypeDvId is COUSIN or COUSIN_BROTHER_SISTER
    		parentTypePair = determineParentType(otherRelationTypeDvId, person1, otherRelationVia1DvId, person2, otherRelationVia2DvId);
    		person1ParentId = getOrCreateParent(person1Id, parentTypePair.getValue0(), sourceId);
    		person2ParentId = getOrCreateParent(person2Id, parentTypePair.getValue1(), sourceId);
    		newRelationType = -1L;
        	if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN && parentTypePair.getValue0().equals(parentTypePair.getValue1())) {
        		newRelationType = Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN;
        	} else if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN && !parentTypePair.getValue0().equals(parentTypePair.getValue1())) {
        		newRelationType = Constants.OTHER_RELATION_TYPE_DV_ID_SIBLING;
        	} else if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER && parentTypePair.getValue0().equals(parentTypePair.getValue1())) {
        		newRelationType = Constants.OTHER_RELATION_TYPE_DV_ID_SIBLING;
        	} else if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER && !parentTypePair.getValue0().equals(parentTypePair.getValue1())) {
        		newRelationType = Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN;
        	}
    		autoParents(newRelationType, person1ParentId, Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN, person2ParentId, Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN, source, ++recursionLevel);
    	}
    }

    private void generateParentRelation(long childPersonId, long fatherPersonId, String relationName, boolean isAutoGenerated, Person source) {
    	long relationId;
    	Relation relation;
    	
    	relationId = Long.parseLong(saveRelation(new RelatedPersonsVO(
    			fatherPersonId,
    			childPersonId,
    			relationName,
				source == null ? null : source.getId()
				)).getKey());
		if (isAutoGenerated) {
			relation = relationRepository.findByIdAndTenant(relationId, SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Person Id " + relationId, null));
			insertAttributeValue(
					new AttributeValueVO(-1L, Constants.RELATION_ATTRIBUTE_DV_ID_IS_AUTO_GENERATED, Constants.BOOL_TRUE),
					null,
					relation,
					source
					);
		}
    }

    private Pair<Long, Long> determineParentType(long otherRelationTypeDvId, Person person1, long otherRelationVia1DvId, Person person2, long otherRelationVia2DvId) {
    	// Map NOT_KNOWN to one of FATHER or MOTHER
    	long parentType1, parentType2;
    	List<RelatedPerson1VO> relatedPerson1VOList11, relatedPerson1VOList12, relatedPerson1VOList21, relatedPerson1VOList22;
    	Quintet<Long, Integer, Integer, Integer, Integer> parentTypeDtKey;
    	
    	parentType1 = otherRelationVia1DvId;
    	parentType2 = otherRelationVia2DvId;
    	if (parentType1 == Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN && parentType1 == Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN) {
    		relatedPerson1VOList11 = retrieveRelatives(person1, Arrays.asList(Constants.RELATION_NAME_FATHER));
    		relatedPerson1VOList12 = retrieveRelatives(person1, Arrays.asList(Constants.RELATION_NAME_MOTHER));
    		relatedPerson1VOList21 = retrieveRelatives(person2, Arrays.asList(Constants.RELATION_NAME_FATHER));
    		relatedPerson1VOList22 = retrieveRelatives(person2, Arrays.asList(Constants.RELATION_NAME_MOTHER));
    		parentTypeDtKey = Quintet.with(otherRelationTypeDvId, relatedPerson1VOList11.size(), relatedPerson1VOList12.size(), relatedPerson1VOList21.size(), relatedPerson1VOList22.size());
    		
    		if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 0, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 0, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 0, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 0, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 1, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 1, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 1, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 0, 1, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 0, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 0, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 0, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 0, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 1, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 1, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 1, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN_BROTHER_SISTER, 1, 1, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 0, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 0, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 0, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 0, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 1, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 1, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 1, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 0, 1, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 0, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 0, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 0, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 0, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 1, 0, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 1, 0, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 1, 1, 0)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_MOTHER, Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		else if (parentTypeDtKey.equals(Quintet.with(Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN, 1, 1, 1, 1)))
        		return Pair.with(Constants.OTHER_RELATION_VIA_DV_ID_FATHER, Constants.OTHER_RELATION_VIA_DV_ID_MOTHER);
    			
    	} else if (parentType1 == Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN) {
    		if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN) {
    			parentType1 = (parentType2 == Constants.OTHER_RELATION_VIA_DV_ID_FATHER ?
    					Constants.OTHER_RELATION_VIA_DV_ID_MOTHER : Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		} else {
    			parentType1 = parentType2;
    		}
    	} else if (parentType2 == Constants.OTHER_RELATION_VIA_DV_ID_NOT_KNOWN) {
    		if (otherRelationTypeDvId == Constants.OTHER_RELATION_TYPE_DV_ID_COUSIN) {
    			parentType2 = (parentType1 == Constants.OTHER_RELATION_VIA_DV_ID_FATHER ?
    					Constants.OTHER_RELATION_VIA_DV_ID_MOTHER : Constants.OTHER_RELATION_VIA_DV_ID_FATHER);
    		} else {
    			parentType2 = parentType1;
    		}
    	}
		return Pair.with(parentType1, parentType2);
    }

    private long getOrCreateParent(long childPersonId, long parentTypeDvId, Long sourceId) {
    	List<RelatedPerson1VO> relatedPerson1VOList1, relatedPerson1VOList2;
    	Person childPerson;
    	long parentPersonId;
    	
    	childPerson = personRepository.findByIdAndTenant(childPersonId, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + childPersonId, null));
		relatedPerson1VOList1 = retrieveRelatives(childPerson, Arrays.asList(Constants.RELATION_NAME_FATHER));
		relatedPerson1VOList2 = retrieveRelatives(childPerson, Arrays.asList(Constants.RELATION_NAME_MOTHER));
		if (parentTypeDvId == Constants.OTHER_RELATION_VIA_DV_ID_FATHER && relatedPerson1VOList1.size() == 0) {
			parentPersonId = savePersonAttributes(new SaveAttributesRequestVO(
					-1L,
					Arrays.asList(
							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, "X"),
							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, Constants.GENDER_NAME_MALE)
							),
					sourceId
					)).getEntityId();
			saveRelation(new RelatedPersonsVO(
					parentPersonId,
					childPersonId,
					Constants.RELATION_NAME_FATHER,
					sourceId
					));
			return parentPersonId;
		} else if (parentTypeDvId == Constants.OTHER_RELATION_VIA_DV_ID_FATHER && relatedPerson1VOList1.size() == 1) {
			return relatedPerson1VOList1.get(0).person.getId();
		} else if (parentTypeDvId == Constants.OTHER_RELATION_VIA_DV_ID_MOTHER && relatedPerson1VOList2.size() == 0) {
			parentPersonId = savePersonAttributes(new SaveAttributesRequestVO(
					-1L,
					Arrays.asList(
							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, "Y"),
							new AttributeValueVO(-1L, Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, Constants.GENDER_NAME_FEMALE)
							),
					sourceId
					)).getEntityId();
			saveRelation(new RelatedPersonsVO(
					parentPersonId,
					childPersonId,
					Constants.RELATION_NAME_MOTHER,
					sourceId
					));
			return parentPersonId;
		} else if (parentTypeDvId == Constants.OTHER_RELATION_VIA_DV_ID_MOTHER && relatedPerson1VOList2.size() == 1) {
			return relatedPerson1VOList2.get(0).person.getId();
		} else {
    		throw new AppException("Cannot automatically add required parents hierarchy. You have to manually establish this relation.", null);
		}
    }
    
    public void deleteRelation(long relationId) {
    	Relation relation;
    	Timestamp deletedAt;
    	
		relation = relationRepository.findByIdAndTenant(relationId, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Relation Id " + relationId, null));
    	
    	deletedAt = new Timestamp(System.currentTimeMillis());
		for (AttributeValue attributeValue : relation.getAttributeValueList()) {
			attributeValue.setDeleter(SecurityContext.getCurrentUser());
			attributeValue.setDeletedAt(deletedAt);
		}
    	relation.setDeleter(SecurityContext.getCurrentUser());
    	relation.setDeletedAt(deletedAt);
    	relationRepository.save(relation);
    }
        
    public void deletePerson(long personId) {
    	Person person;
    	List<Relation> relationList;
    	Timestamp deletedAt;
    	
		person = personRepository.findByIdAndTenant(personId, SecurityContext.getCurrentTenant())
				.orElseThrow(() -> new AppException("Invalid Person Id " + personId, null));
    	
    	deletedAt = new Timestamp(System.currentTimeMillis());
    	
    	relationList = relationRepository.findByPerson1(person);
    	relationList.addAll(relationRepository.findByPerson2(person));
    	for (Relation relation : relationList) {
    		for (AttributeValue attributeValue : relation.getAttributeValueList()) {
    			attributeValue.setDeleter(SecurityContext.getCurrentUser());
    			attributeValue.setDeletedAt(deletedAt);
    			attributeValueRepository.save(attributeValue);
    		}
    		relation.setDeleter(SecurityContext.getCurrentUser());
    		relation.setDeletedAt(deletedAt);
        	relationRepository.save(relation);
		}
    	
		for (AttributeValue attributeValue : person.getAttributeValueList()) {
			attributeValue.setDeleter(SecurityContext.getCurrentUser());
			attributeValue.setDeletedAt(deletedAt);
			attributeValueRepository.save(attributeValue);
		}
    	person.setDeleter(SecurityContext.getCurrentUser());
    	person.setDeletedAt(deletedAt);
    	personRepository.save(person);
    }

    public List<List<Object>> importPrData(String function, Long sourceId, Iterable<CSVRecord> csvRecords) {
    	// Two passes, one to validate and one to store into DB. This avoids gap in person/relation/attributeValue ids, caused by exception during storing.
    	Pair<List<List<Object>>, List<Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>>>> preImportData;
		List<List<Object>> validationMessageList;
		List<Object> validationMessage;
		
		preImportData = validatePrData(csvRecords);
		validationMessageList = preImportData.getValue0();
		if (validationMessageList.size() == 1) {
			if (function.equals(Constants.UPLOAD_FUNCTION_STORE)) {
				validationMessage = new ArrayList<Object>(1);
				validationMessageList.set(0, validationMessage);
				validationMessage.add("File imported Successfully.");
				try {
					storePrData(sourceId, csvRecords);
				} catch (Exception e) {
					e.printStackTrace();
					validationMessage.set(0, e.getMessage());
				}
			} else {
				validationMessage = new ArrayList<Object>(1);
				validationMessageList.set(0, validationMessage);
				validationMessage.add("Duplicate checked Successfully.");
				try {
					return checkDuplicatesPrData(preImportData.getValue1());
				} catch (Exception e) {
					e.printStackTrace();
					validationMessage.set(0, e.getMessage());
				}
			}
		}
		return validationMessageList;
    }
    
    private Pair<List<List<Object>>, List<Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>>>> validatePrData(Iterable<CSVRecord> csvRecords) {
		List<List<Object>> validationMessageList;
		List<Object> validationMessageHeader;
    	int cellCount, cellInd, lastCellInd, lastRecordLevel, rowInd;
    	Iterator<String> strItr;
    	String cellContent, personAttributeValuesArr[], currentGender, previousGender, currentName;
    	Long personId;
    	Optional<Person> person;
    	AttributeValue genderAv, nameAv;
    	DomainValue genderAttributeDv, nameAttributeDv, attributeValueDv;
    	Map<String, Pair<String, String>> referenceIdMap;
    	List<Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>>> sheetContent;
    	
    	validationMessageList = new ArrayList<List<Object>>();
		validationMessageHeader = new ArrayList<Object>(3);
		validationMessageList.add(validationMessageHeader);
		
		validationMessageHeader.add("Row");
		validationMessageHeader.add("Column");
		validationMessageHeader.add("Error");
		
		referenceIdMap = new HashMap<String, Pair<String, String>>();
		sheetContent = new ArrayList<Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>>>();
		genderAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER)
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, null));
	
		nameAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME)
				.orElseThrow(() -> new AppException("Invalid Attribute Dv Id " + Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, null));
		previousGender = null;
		lastRecordLevel = -1;
		rowInd = -1;
    	recordLoop: for (CSVRecord csvRecord : csvRecords) {
    		rowInd++;
    		cellCount = 0;
    		strItr = csvRecord.iterator();	// In this version of CSVRecord, toList() and values() are not public
    		cellInd = -1;					// and regular iterator doesn't support nextIndex()
    		lastCellInd = -1;
    		while (strItr.hasNext()) {
    			cellInd++;
				personId = null;
    			cellContent = strItr.next();
    			if (cellContent.equals("")) {
    				continue;
    			}
				cellCount++;
				if (cellCount > 2) {
					addValidationMessage(validationMessageList, csvRecord, null, "Invalid record structure: More than two values found.");
					continue recordLoop;
				}
				
				if (lastCellInd == -1) {	// Current one is first non-null value
					if (cellInd / 2 > lastRecordLevel + 1) {
    					addValidationMessage(validationMessageList, csvRecord, null, "Invalid record structure: Atleast one parent should be given.");
    					lastRecordLevel = cellInd / 2;
    					continue recordLoop;
					}
					 // After this point, don't use lastCellInd, lastRecordLevel
					lastRecordLevel = cellInd / 2;
					lastCellInd = cellInd;
				} else {
					if (cellInd != lastCellInd + 1) {	// Should be adjacent to each other
    					addValidationMessage(validationMessageList, csvRecord, null, "Invalid record structure: Two values are not adjacent to each other.");
    					continue recordLoop;
					}
					if (cellInd % 2 == 0) {
    					addValidationMessage(validationMessageList, csvRecord, null, "Invalid record structure: First value should be in odd column (A, C, E, ...) and second one in even column (B, D, F, ...).");
    					continue recordLoop;
					}
				}
    			
    			personAttributeValuesArr = cellContent.split("#", -1);
    			if (personAttributeValuesArr.length < Constants.UPLOAD_CELL_STRUCTURE_MIN_LEN || personAttributeValuesArr.length > Constants.UPLOAD_CELL_STRUCTURE_MAX_LEN) {
					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Missing or extraneous components.");
					continue recordLoop;
    			}
    			
    			if (!personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_SEQNO].equals("")) {
    		    	try {
    		    		Double.parseDouble(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_SEQNO]);
    		    	} catch(NumberFormatException nfe) {
    					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Sequence no. not numeric.");
    					continue recordLoop;
    		    	}
    			}
    			
    			currentGender = null;
    			if (personAttributeValuesArr.length == 2) {
    		    	try {
    		    		personId = Long.parseLong(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO]);
    		    	} catch(NumberFormatException nfe) {
    		    		// NOP
    		    	}
    		    	if (personId == null) {
    		    		if (referenceIdMap.containsKey(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO])) {
    		    			currentGender = referenceIdMap.get(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO]).getValue1();
    		    			currentName = referenceIdMap.get(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO]).getValue0();
    		    		} else {
	    					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Invalid reference id.");
	    					continue recordLoop;
    		    		}
    		    	} else {
	    	    		person = personRepository.findByIdAndTenant(personId, SecurityContext.getCurrentTenant());
	    	    		if (!person.isPresent()) {
	    					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Person doesn't exist.");
	    					continue recordLoop;
	    	    		}
	    				genderAv = fetchAttribute(person.get(), null, genderAttributeDv)
	    						.orElseThrow(() -> new AppException("Invalid gender", null));
	    				attributeValueDv = domainValueRepository.findById(Long.valueOf(genderAv.getAttributeValue()))
	            				.orElseThrow(() -> new AppException("Invalid Attribute Value Dv Id ", null));
	    				currentGender = attributeValueDv.getValue().substring(0,1);	// TODO: Incorrect logic (In some languages, duplicates can be there; In some languages, a single character could be made up of multiple unicodes)
	    				nameAv = fetchAttribute(person.get(), null, nameAttributeDv)
	    						.orElseThrow(() -> new AppException("Invalid name", null));
	    	    		currentName = nameAv.getAttributeValue();
    		    	}
    			} else {
    	    		if (personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME].equals("")) {
    					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Person name cannot be empty.");
    					continue recordLoop;
    	    		}
    	    		currentName = personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME];
    	    		currentGender = personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_GENDER];
    	    		if (!currentGender.equals("M") && !currentGender.equals("F")) {
    					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Unsupported Gender.");
    					continue recordLoop;
    				}
        			if (personAttributeValuesArr.length > Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS && !personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS].equals("")) {
        	    		if (referenceIdMap.containsKey(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS])) {
        					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cell content: Reference id " + personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS] + " is used already.");
        					continue recordLoop;
        	    		}
        				referenceIdMap.put(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS], Pair.with(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME], currentGender));
        			}
    	    		
    			}
				if (cellCount == 1) {
					previousGender = currentGender;
				} else if (currentGender.equals(previousGender)) {
					previousGender = null;
					addValidationMessage(validationMessageList, csvRecord, cellInd, "Invalid cells contents: Same Gender Spouse relation is currently unsupported.");
					continue recordLoop;
				}
				
				sheetContent.add(Octet.with(rowInd, cellInd, personId, currentName, currentGender, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()));
    		}
    	}
		return Pair.with(validationMessageList, sheetContent);
    }

    private void addValidationMessage(List<List<Object>> validationMessageList, CSVRecord csvRecord, Integer colInd, String message) {
		List<Object> validationMessage;
		
		validationMessage = new ArrayList<Object>(3);
		validationMessageList.add(validationMessage);
		
		validationMessage.add(csvRecord.getRecordNumber());
		validationMessage.add(colInd == null ? null : colInd + 1);
		validationMessage.add(message);
    	
    }

    private List<List<Object>> checkDuplicatesPrData(List<Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>>> sheetContent) {
		List<List<Object>> duplicatesContents;
		List<Object> duplicatesRow;
		int tmpInd, currInd;
		List<AttributeValueVO> attributeValueVOList;
		AttributeValueVO attributeValueVO;
		SearchResultsVO searchResultsVO;
		
		// TODO: Using label, surName
		duplicatesContents = new ArrayList<List<Object>>();
		currInd = -1;
		for (Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>> cellContentTuple : sheetContent) {
			// 0: row, 1: column, 2: personId, 3: name, 4: gender, 5: parents, 6: spouses, 7: children
			currInd++;
			if (cellContentTuple.getValue2() != null) {
				continue;
			}
			if (cellContentTuple.getValue1() % 2 == 0) { // Columns: A, C, E, ...
				tmpInd = currInd - 1;
				while (tmpInd > -1 && sheetContent.get(tmpInd).getValue1().compareTo(cellContentTuple.getValue1()) >= 0) tmpInd--; // Locate Parent
				if (tmpInd > -1 && sheetContent.get(tmpInd).getValue1().equals(cellContentTuple.getValue1() - 1)) {
					addNonDummy(cellContentTuple.getValue5(), sheetContent.get(tmpInd).getValue3()); // add to parents of current
					if (sheetContent.get(tmpInd).getValue2() == null) {
						addNonDummy(sheetContent.get(tmpInd).getValue7(), cellContentTuple.getValue3()); // add to children of parent
					}
					tmpInd--;
				}
				if (tmpInd > -1 && sheetContent.get(tmpInd).getValue1().equals(cellContentTuple.getValue1() - 2)) {
					addNonDummy(cellContentTuple.getValue5(), sheetContent.get(tmpInd).getValue3()); // add to parents of current
					if (sheetContent.get(tmpInd).getValue2() == null) {
						addNonDummy(sheetContent.get(tmpInd).getValue7(), cellContentTuple.getValue3()); // add to children of parent
					}
				}
			} else { // Columns: B, D, F, ...
				tmpInd = currInd - 1; // Locate Spouse
				if (tmpInd > -1 && sheetContent.get(tmpInd).getValue0().equals(cellContentTuple.getValue0())) {
					addNonDummy(cellContentTuple.getValue6(), sheetContent.get(tmpInd).getValue3()); // add to spouses of current
					if (sheetContent.get(tmpInd).getValue2() == null) {
						addNonDummy(sheetContent.get(tmpInd).getValue6(), cellContentTuple.getValue3()); // add to spouses of spouse
					}
				}
			}
		}
		
		for (Octet<Integer, Integer, Long, String, String, List<String>, List<String>, List<String>> cellContentTuple : sheetContent) {
			if (cellContentTuple.getValue2() != null || (cellContentTuple.getValue5().size() == 0 && cellContentTuple.getValue6().size() == 0 && cellContentTuple.getValue7().size() == 0)) {
				continue;
			}
			attributeValueVOList = new ArrayList<AttributeValueVO>();
			attributeValueVO = new AttributeValueVO();
			attributeValueVOList.add(attributeValueVO);
			attributeValueVO.setAttributeDvId(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME);
			attributeValueVO.setAttributeValue(cellContentTuple.getValue3());
			/* TODO GENDER attributeValueVO = new AttributeValueVO();
			attributeValueVOList.add(attributeValueVO);
			attributeValueVO.setAttributeDvId(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER);
			attributeValueVO.setAttributeName(cellContentTuple.getValue4()); After converting from "M", "F" to 59, 60 */
			if (cellContentTuple.getValue5().size() > 0) {
				attributeValueVO = new AttributeValueVO();
				attributeValueVOList.add(attributeValueVO);
				attributeValueVO.setAttributeDvId(Constants.PERSON_ATTRIBUTE_DV_ID_PARENTS);
				attributeValueVO.setAttributeValueList(cellContentTuple.getValue5());
			}
			if (cellContentTuple.getValue6().size() > 0) {
				attributeValueVO = new AttributeValueVO();
				attributeValueVOList.add(attributeValueVO);
				attributeValueVO.setAttributeDvId(Constants.PERSON_ATTRIBUTE_DV_ID_SPOUSES);
				attributeValueVO.setAttributeValueList(cellContentTuple.getValue6());
			}
			if (cellContentTuple.getValue7().size() > 0) {
				attributeValueVO = new AttributeValueVO();
				attributeValueVOList.add(attributeValueVO);
				attributeValueVO.setAttributeDvId(Constants.PERSON_ATTRIBUTE_DV_ID_CHILDREN);
				attributeValueVO.setAttributeValueList(cellContentTuple.getValue7());
			}
			searchResultsVO = searchPerson(new PersonSearchCriteriaVO(true, attributeValueVOList));
			if (searchResultsVO.getResultsList() != null && searchResultsVO.getResultsList().size() < 10) {
				if (duplicatesContents.size() < cellContentTuple.getValue0() + 1) {
					duplicatesRow = new ArrayList<Object>();
					UtilFuncs.listSet(duplicatesContents, cellContentTuple.getValue0().floatValue(), duplicatesRow, new ArrayList<Object>());
				} else {
					duplicatesRow = duplicatesContents.get(cellContentTuple.getValue0());
				}
				UtilFuncs.listSet(duplicatesRow, cellContentTuple.getValue1().floatValue(), searchResultsVO.getResultsList().get(1).get(0), null);
				UtilFuncs.listSet(duplicatesRow, cellContentTuple.getValue1().floatValue() + 2, searchResultsVO.getQueryToDb(), null);
			}
			
		}
    	return duplicatesContents;
    }
    
    private void addNonDummy(List<String> targetList, String toAdd) {
    	 if (!toAdd.equalsIgnoreCase("X") && !toAdd.equalsIgnoreCase("Y")) {
    		 targetList.add(toAdd);
    	 }
    }
    
    private void storePrData(Long sourceId, Iterable<CSVRecord> csvRecords) {
    	/* A cell content (Person details) is either skipped (if person id) or INSERTed.
    	 * For relationship to be INSERTed, no prior relationship should exist between the two persons already.
    	 * There is no DELETE or MODIFY of Person and Relation.
    	 * Special first name DITTO with gender to handle multiple spouses Vs. Empty cell to mean details are not known
    	 * Different sequence within each parent not supported
    	 */
    	int level, cellCount, cellInd;
    	Person mainPerson, spousePerson, source;
    	Relation relation;
    	AttributeValue attributeValue, mainPersonGenderAv, spousePersonGenderAv;
    	DomainValue firstNamePersAttributeDv, genderPersAttributeDv, labelPersAttributeDv, surNamePersAttributeDv, person1ForPerson2RelAttributeDv, person2ForPerson1RelAttributeDv, sequenceOfPerson2ForPerson1RelAttributeDv;
    	List<Person> malePersonList, femalePersonList;
    	ParsedCellContentVO parsedCellContentVO;
    	Double withinSpouseSequenceNo, withinParentSequenceNo;
    	boolean isRelationNewlyCreated;
    	Iterator<String> strItr;
    	String cellContent;
    	Map<String, Person> referenceIdMap;
    	
		firstNamePersAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.PERSON_ATTRIBUTE_DV_ID_FIRST_NAME, null));
		genderPersAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_GENDER)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.PERSON_ATTRIBUTE_DV_ID_GENDER, null));
		labelPersAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_LABEL)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.PERSON_ATTRIBUTE_DV_ID_LABEL, null));
		surNamePersAttributeDv = domainValueRepository.findById(Constants.PERSON_ATTRIBUTE_DV_ID_SUR_NAME)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.PERSON_ATTRIBUTE_DV_ID_SUR_NAME, null));
		person1ForPerson2RelAttributeDv =  domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, null));
		person2ForPerson1RelAttributeDv =  domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, null));
		sequenceOfPerson2ForPerson1RelAttributeDv =  domainValueRepository.findById(Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1)
				.orElseThrow(() -> new AppException("Attribute Dv Id missing: " + Constants.RELATION_ATTRIBUTE_DV_ID_SEQUENCE_OF_PERSON2_FOR_PERSON1, null));
    	source = null;
    	if (sourceId != null) {
    		source = personRepository.findByIdAndTenant(sourceId, SecurityContext.getCurrentTenant())
    				.orElseThrow(() -> new AppException("Invalid Source " + sourceId, null));
    	}

		malePersonList = new ArrayList<Person>();
		femalePersonList = new ArrayList<Person>();
		referenceIdMap = new HashMap<String, Person>();

    	for (CSVRecord csvRecord : csvRecords) {
			mainPerson = null;
			spousePerson = null;
	    	mainPersonGenderAv = null;
	    	spousePersonGenderAv = null;
	    	withinParentSequenceNo = null;
	    	withinSpouseSequenceNo = null;
    		
    		cellCount = 0;
    		strItr = csvRecord.iterator();
    		cellInd = -1;
    		level = -1;
    		while (strItr.hasNext()) {
    			cellInd++;
    			cellContent = strItr.next();
    			if (cellContent.equals("")) {
    				continue;
    			}
				cellCount++;
				if (cellCount == 1) {
					level = cellInd / 2;
				}
    	    	parsedCellContentVO = cellContentsToPerson(cellContent, level, malePersonList, femalePersonList, referenceIdMap, firstNamePersAttributeDv, genderPersAttributeDv, labelPersAttributeDv, surNamePersAttributeDv, source);
				if (cellInd % 2 == 0) {
	    	    	mainPerson = parsedCellContentVO.person;
	    	    	withinParentSequenceNo = parsedCellContentVO.sequenceNo;
	    	    	mainPersonGenderAv = parsedCellContentVO.genderAv;
				} else {
        	    	spousePerson = parsedCellContentVO.person;
        	    	withinSpouseSequenceNo = parsedCellContentVO.sequenceNo;
        	    	spousePersonGenderAv = parsedCellContentVO.genderAv;
				}
    		}
    		
    		if (mainPerson == null) {
    			if (spousePerson == null) {
    				continue;
    			} else {
	    			// Insert a dummy
	    			if (spousePersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
	    				cellContent = "#Unknown#F#";
	    			} else { // if (spousePersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE))
	    				cellContent = "#Unknown#M#";
	    			}
	    			cellCount++;
	    	    	parsedCellContentVO = cellContentsToPerson(cellContent, level, malePersonList, femalePersonList, referenceIdMap, firstNamePersAttributeDv, genderPersAttributeDv, labelPersAttributeDv, surNamePersAttributeDv, source);
	    	    	mainPerson = parsedCellContentVO.person;
	    	    	mainPersonGenderAv = parsedCellContentVO.genderAv;
    			}
    		}

    		if (cellCount == 2) {
    	    	isRelationNewlyCreated = false;
    	    	if ((relation = relationRepository.findRelationGivenPersons(mainPerson.getId(), spousePerson.getId(), SecurityContext.getCurrentTenantId())) == null) {
    	    		isRelationNewlyCreated = true;
        			if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
        				relation = new Relation(mainPerson, spousePerson, source);
        			} else if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE)) {
        				relation = new Relation(spousePerson, mainPerson, source);
        			}
        	    	relation = relationRepository.save(relation);
        	    	
    				attributeValue = new AttributeValue(person1ForPerson2RelAttributeDv, Constants.RELATION_NAME_HUSBAND, null, relation, source);
    	    		attributeValueRepository.save(attributeValue);
    	    		
    				attributeValue = new AttributeValue(person2ForPerson1RelAttributeDv, Constants.RELATION_NAME_WIFE, null, relation, source);
    	    		attributeValueRepository.save(attributeValue);
    	    		
    	    	}
    	    	saveSequenceNo(relation, sequenceOfPerson2ForPerson1RelAttributeDv, withinSpouseSequenceNo, isRelationNewlyCreated, source);
    			
    		}

    		if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE) ||
    				spousePerson != null &&
    				spousePersonGenderAv != null && spousePersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE)) {
    			UtilFuncs.listSet(malePersonList, level, mainPerson, null);
    			UtilFuncs.listSet(femalePersonList, level, spousePerson, null);
    		} else if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE) ||
    				spousePerson != null &&
    				spousePersonGenderAv != null && spousePersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
    			UtilFuncs.listSet(malePersonList, level, spousePerson, null);
    			UtilFuncs.listSet(femalePersonList, level, mainPerson, null);
			}
    		for (int ind = level+1; ind < malePersonList.size(); ind++) {
    			malePersonList.set(ind, null);
    			femalePersonList.set(ind, null);
    		}
    		
    		if (mainPerson != null) {
        		establishParent(mainPerson, mainPersonGenderAv, withinParentSequenceNo, level, Constants.RELATION_NAME_FATHER, malePersonList, person1ForPerson2RelAttributeDv, person2ForPerson1RelAttributeDv, sequenceOfPerson2ForPerson1RelAttributeDv, source);
        		establishParent(mainPerson, mainPersonGenderAv, withinParentSequenceNo, level, Constants.RELATION_NAME_MOTHER, femalePersonList, person1ForPerson2RelAttributeDv, person2ForPerson1RelAttributeDv, sequenceOfPerson2ForPerson1RelAttributeDv, source);
    		}
    	}
    }
    
    private ParsedCellContentVO cellContentsToPerson(String cellContents, int level, List<Person> malePersonList, List<Person> femalePersonList, Map<String, Person> referenceIdMap, DomainValue firstNamePersAttributeDv, DomainValue genderPersAttributeDv, DomainValue labelPersAttributeDv, DomainValue surNamePersAttributeDv, Person source) {
    	long personId;
    	Person person;
    	String[] personAttributeValuesArr;
    	AttributeValue attributeValue, genderAv;
    	ParsedCellContentVO parsedCellContentVO;
    	Double sequenceNo;
    	boolean isMale;
    	List<AttributeValue> attributeValueList;
    	
		personAttributeValuesArr = cellContents.split("#", -1);
		
    	sequenceNo = null;
		if (!personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_SEQNO].equals("")) {
    		sequenceNo = Double.parseDouble(personAttributeValuesArr[0]);
		}
		
		if (personAttributeValuesArr.length == 2) {
	    	try {
	    		personId = Long.parseLong(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO]);
	    		person = personRepository.findByIdAndTenant(personId, SecurityContext.getCurrentTenant())
	    				.orElseThrow(() -> new AppException("Invalid Person Id " + personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO], null));
	    	} catch(NumberFormatException nfe) {
	    		person = referenceIdMap.get(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFER_TO]);
	    	}
			genderAv = fetchAttribute(person, null, genderPersAttributeDv)
					.orElseThrow(() -> new AppException("Invalid gender", null));
	    	
			isMale = false;
    		if (genderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
    			isMale = true;
			}
    		
		} else {
	    	
			isMale = false;
    		if (personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_GENDER].equals("M")) {
    			isMale = true;
			}
    		
    		if (personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME].equalsIgnoreCase("DITTO")) {
    			if (isMale) {
    				person = malePersonList.get(level);
    			} else {
    				person = femalePersonList.get(level);
    			}
    			genderAv = fetchAttribute(person, null, genderPersAttributeDv)
    					.orElseThrow(() -> new AppException("Invalid gender", null));
    		} else {
        		person = new Person(source);
        		person = personRepository.save(person);
        		if (personAttributeValuesArr.length > Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS && !personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS].equals("")) {
        			referenceIdMap.put(personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_REFERRED_AS], person);
        		}
        		
        		attributeValueList = new ArrayList<AttributeValue>(3);
        		
	    		attributeValue = new AttributeValue(firstNamePersAttributeDv, personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_FIRST_NAME], person, null, source);
	    		attributeValueRepository.save(attributeValue);
	    		attributeValueList.add(attributeValue);
	    		
	        	genderAv = new AttributeValue(genderPersAttributeDv, isMale? Constants.GENDER_NAME_MALE : Constants.GENDER_NAME_FEMALE, person, null, source);
	    		attributeValueRepository.save(genderAv);
	    		attributeValueList.add(genderAv);
	    		
	    		if (personAttributeValuesArr.length > Constants.UPLOAD_CELL_STRUCTURE_POSITION_SUR_NAME && !personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_SUR_NAME].equals("")) {
		    		attributeValue = new AttributeValue(surNamePersAttributeDv, personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_SUR_NAME], person, null, source);
		    		attributeValueRepository.save(attributeValue);
		    		attributeValueList.add(attributeValue);
	    		}
	    		
	    		if (personAttributeValuesArr.length > Constants.UPLOAD_CELL_STRUCTURE_POSITION_LABEL && !personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_LABEL].equals("")) {
		    		attributeValue = new AttributeValue(labelPersAttributeDv, personAttributeValuesArr[Constants.UPLOAD_CELL_STRUCTURE_POSITION_LABEL], person, null, source);
		    		attributeValueRepository.save(attributeValue);
		    		attributeValueList.add(attributeValue);
	    		}
	    		
	    		person.setAttributeValueList(attributeValueList);	// Later retrieval of attributeValueList from referenceIdMap's person will otherwise be NULL
	    															// Before introduction of getPersonAttribute method, this last was obtained from repository
    		}
    	}
    	
    	parsedCellContentVO = new ParsedCellContentVO();
    	parsedCellContentVO.person = person;
    	parsedCellContentVO.sequenceNo = sequenceNo;
    	parsedCellContentVO.genderAv = genderAv;
    	return parsedCellContentVO;
    }
    
    private void establishParent(Person mainPerson, AttributeValue mainPersonGenderAv, Double sequenceNo, int level, String parentRelationName, List<Person> personList, DomainValue person1ForPerson2RelAttributeDv, DomainValue person2ForPerson1RelAttributeDv, DomainValue sequenceOfPerson2ForPerson1RelAttributeDv, Person source) {
    	AttributeValue attributeValue;
    	Relation relation;
    	boolean isRelationNewlyCreated;

    	if (level > 0 && personList.get(level - 1) != null) {
    		
    		isRelationNewlyCreated = false;
    		if ((relation = relationRepository.findRelationGivenPersons(mainPerson.getId(), personList.get(level - 1).getId(), SecurityContext.getCurrentTenantId())) == null) {
        		isRelationNewlyCreated = true;
		    	relation = new Relation(personList.get(level - 1), mainPerson, source);
		    	relation = relationRepository.save(relation);
		    	
				attributeValue = new AttributeValue(person1ForPerson2RelAttributeDv, parentRelationName, null, relation, source);
	    		attributeValueRepository.save(attributeValue);
	    		
				if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_MALE)) {
					attributeValue = new AttributeValue(person2ForPerson1RelAttributeDv, Constants.RELATION_NAME_SON, null, relation, source);
				} else { // if (mainPersonGenderAv.getAttributeValue().equals(Constants.GENDER_NAME_FEMALE))
					attributeValue = new AttributeValue(person2ForPerson1RelAttributeDv, Constants.RELATION_NAME_DAUGHTER, null, relation, source);
				}
	    		attributeValueRepository.save(attributeValue);
    		}
    		
	    	saveSequenceNo(relation, sequenceOfPerson2ForPerson1RelAttributeDv, sequenceNo, isRelationNewlyCreated, source);
    	}
    		
    }

    private void saveSequenceNo(Relation relation, DomainValue sequenceOfPerson2ForPerson1RelAttributeDv, Double sequenceNo, boolean isRelationNewlyCreated, Person source) {
    	AttributeValue attributeValue;
    	String formattedSequenceNo;
    	
		if (sequenceNo != null) {
			if (sequenceNo.doubleValue() == sequenceNo.intValue()) {
				formattedSequenceNo = String.format("%d", sequenceNo.intValue());
			} else {
				formattedSequenceNo = String.format("%.1f", sequenceNo);	// Beware - Only one digit after decimal
			}
			if (isRelationNewlyCreated) {
				attributeValue = null;
			} else {
				attributeValue = fetchAttribute(null, relation, sequenceOfPerson2ForPerson1RelAttributeDv)
						.orElseGet(() -> null);
			}
			if (attributeValue == null) {
				attributeValue = new AttributeValue(sequenceOfPerson2ForPerson1RelAttributeDv, formattedSequenceNo, null, relation, source);
			} else {
				attributeValue.setAttributeValue(formattedSequenceNo);
			}
    		attributeValueRepository.save(attributeValue);
		}
    }
    
    private Optional<AttributeValue> fetchAttribute(Person person, Relation relation, DomainValue persAttributeDv) {
    	DomainValueFlags domainValueFlags;
    	List<AttributeValue> attributeValueList;
    	
    	domainValueFlags = new DomainValueFlags(persAttributeDv);
    	attributeValueList = (person == null ? relation.getAttributeValueList() : person.getAttributeValueList());
    	for (AttributeValue av : attributeValueList) {
    		if (av.getAttribute().equals(persAttributeDv) &&
    				(domainValueFlags.getRepetitionType().equals(Constants.FLAG_ATTRIBUTE_REPETITION_NOT_ALLOWED) ||
    				serviceParts.isCurrentValidAttributeValue(av))) {
    			return Optional.of(av); // TODO: In case of FLAG_ATTRIBUTE_REPETITION_OVERLAPPING_ALLOWED, this returns only the first, not the list!
    		}
    	}
    	return Optional.empty();
    }
    
    // Classes that can be avoided with JavaTuples
    protected class RelatedPerson2VO {
    	Person person;
    	int level;
    	double sequence;
    	boolean isSpouse;	// Whether, during tree traversal, this node was added as a spouse of another node
    	int parentInd;
    	boolean isFirstKid;
    }
    
    protected class RelatedPerson3VO  implements Comparable<RelatedPerson3VO> {
    	long personId;
    	double seqNo;

    	public RelatedPerson3VO(long personId, double seqNo) {
    		this.personId = personId;
    		this.seqNo = seqNo;
    	}
    	
    	public int compareTo(RelatedPerson3VO relatedPerson3VO) {
    		return (this.seqNo < relatedPerson3VO.seqNo ? -1 : this.seqNo == relatedPerson3VO.seqNo ? 0 : 1);
    	}
    	
        public boolean equals(Object relatedPerson3VO) {
        	return this.personId == ((RelatedPerson3VO)relatedPerson3VO).personId;
        }
    }
    
    protected class TreeIntermediateOut1VO {
    	String personId;
    	int level;
    	List<TreeIntermediateOut2VO> directRelativesList;
    }
    
    protected class TreeIntermediateOut2VO {
    	String spouseId;
    	List<String> kidsList;
    }
    
    protected class ParsedCellContentVO {
    	Person person;
    	Double sequenceNo;
    	AttributeValue genderAv;
    }
}
