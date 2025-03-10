package org.sakuram.relation.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.sakuram.relation.algo.ShortestPathBreadthFirst;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.repository.PersonRepository;
import org.sakuram.relation.repository.RelationRepository;
import org.sakuram.relation.util.AppException;
import org.sakuram.relation.util.PatternBasedXY;
import org.sakuram.relation.util.PatternBasedXY.XY;
import org.sakuram.relation.valueobject.RelatedPersonsVO;
import org.sakuram.relation.valueobject.RelationVO;
import org.sakuram.relation.valueobject.GraphVO;
import org.sakuram.relation.valueobject.PersonVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AlgoService {
	
	@Autowired
	PersonRepository personRepository;
	@Autowired
	RelationRepository relationRepository;

	@Autowired
	ShortestPathBreadthFirst shortestPathBreadthFirst;
	@Autowired
	ServiceParts serviceParts;
	@Autowired
	PatternBasedXY patternBasedXY;
	@Autowired
	RelationSimplification relationSimplification;
	
	@Value("${relation.application.ascertain.relation.graph.pattern}")
	String ASCERTAIN_RELATION_GRAPH_PATTERN;
	
	public GraphVO retrieveRelationPath(RelatedPersonsVO relatedPersonsVO) {
    	List<Person> personList;
    	Map<Long, Person> allPersonsMap;
    	LinkedList<Person> relatedPersonList;
    	List<String> excludeRelationIdList;
    	List<String> excludePersonIdList;
    	XY xy;
    	int ind;
    	Relation relation;
    	
    	GraphVO retrieveRelationsResponseVO;
    	Map<Long, PersonVO> personVOMap;
    	List<RelationVO> relationVOList;
    	PersonVO personVO;
    	
    	excludeRelationIdList = Arrays.asList(relatedPersonsVO.getExcludeRelationIdCsv().split(", *"));
    	excludePersonIdList = Arrays.asList(relatedPersonsVO.getExcludePersonIdCsv().split(", *"));
    	personList = personRepository.findAll();
    	allPersonsMap = new HashMap<Long, Person>(personList.size());
    	for (Person person : personList) {
    		allPersonsMap.put(person.getId(), person);
    	}
    	
    	relatedPersonList = shortestPathBreadthFirst.findPathBiBFS(allPersonsMap, relatedPersonsVO.getPerson1Id(), relatedPersonsVO.getPerson2Id(), excludeRelationIdList, excludePersonIdList);
    	if (relatedPersonList == null) {
    		throw new AppException("No relation could be established between the two!", null);
    	}
    	retrieveRelationsResponseVO = new GraphVO();
    	personVOMap = new HashMap<Long, PersonVO>();
    	relationVOList = new ArrayList<RelationVO>();
    	retrieveRelationsResponseVO.setEdges(relationVOList);
    	patternBasedXY.init(ASCERTAIN_RELATION_GRAPH_PATTERN, 10, 10, 1);
    	for (ind = 0; ind < relatedPersonList.size(); ind++) {
			LogManager.getLogger().debug("Person: " + relatedPersonList.get(ind).getId());
    		personVO = serviceParts.addToPersonVOMap(personVOMap, relatedPersonList.get(ind));
    		xy = patternBasedXY.getNextXY();
    		personVO.getAttributes().setX(xy.x);
    		personVO.getAttributes().setY(xy.y);
    		if (ind < relatedPersonList.size() - 1) {
    			relation = relationRepository.findByPerson1AndPerson2(relatedPersonList.get(ind), relatedPersonList.get(ind+1));
    			if (relation == null) {
        			relation = relationRepository.findByPerson1AndPerson2(relatedPersonList.get(ind+1), relatedPersonList.get(ind));
        			if (relation == null) {
        				throw new AppException("No relation between " + relatedPersonList.get(ind).getId() + " and " + relatedPersonList.get(ind+1).getId() + "!", null);
        			}
    			}
    			serviceParts.addToRelationVOList(relationVOList, relation, relatedPersonList.get(ind), true);
    		}
    	}
    	
    	retrieveRelationsResponseVO.setNodes(new ArrayList<PersonVO>(personVOMap.values()));
    	relationSimplification.addSimplifiedRelationPaths(retrieveRelationsResponseVO);
    	return retrieveRelationsResponseVO;
	}

}
