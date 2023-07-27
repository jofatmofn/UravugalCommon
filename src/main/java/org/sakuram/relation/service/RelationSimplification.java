package org.sakuram.relation.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.sakuram.relation.bean.DomainValue;
import org.sakuram.relation.repository.DomainValueRepository;
import org.sakuram.relation.util.AppException;
import org.sakuram.relation.util.Constants;
import org.sakuram.relation.valueobject.GraphVO;
import org.sakuram.relation.valueobject.RelationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelationSimplification {

	@Autowired
	DomainValueRepository domainValueRepository;

	private final int MAX_RELATION_PATH_FOR_SIMPLIFICATION = 3;
			
	public void addSimplifiedRelationPaths(GraphVO graphVO) {
		List<RelationVO> additionalRelationVOList, simplifiedRelationVOList, newSimplifiedRelationVOList;
		int ind1, ind2, simpleRelationInd;
		List<SimplificationRuleVO> simplificationRuleVOList;
		Relation1VO simplifiedRelation, currRelation1VO, nextRelation1VO, nextToNextRelation1VO;
		RelationVO relationVO;
		boolean noNewSimplification;
		DomainValue relName1Dv, relName2Dv;
		
		LogManager.getLogger().debug("Original size: " + graphVO.getEdges().size());
		simpleRelationInd = -1;
		simplificationRuleVOList = getRelationSimplificationRules();
		newSimplifiedRelationVOList = new ArrayList<RelationVO>(graphVO.getEdges());
		additionalRelationVOList = new ArrayList<RelationVO>();
		noNewSimplification = false;
		
		while(!noNewSimplification) {
			LogManager.getLogger().debug("New Round of Simplification");
			simplifiedRelationVOList = new ArrayList<RelationVO>(newSimplifiedRelationVOList);
			newSimplifiedRelationVOList = new ArrayList<RelationVO>();
			ind1 = 0;
			noNewSimplification = true;
			while (ind1 < simplifiedRelationVOList.size()) {
				LogManager.getLogger().debug("ind1: " + ind1);
				currRelation1VO = new Relation1VO(Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)), Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)));
				for (ind2 = 2; ind2 <= MAX_RELATION_PATH_FOR_SIMPLIFICATION; ind2++) {
					LogManager.getLogger().debug("ind2: " + ind2);
					simplifiedRelation = null;
					if (ind1 + ind2 - 1 < simplifiedRelationVOList.size()) {
						nextRelation1VO = new Relation1VO(Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1+1).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)), Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1+1).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)));
						if (ind2 == 2) {
							simplifiedRelation = getSimplifiedRelation(simplificationRuleVOList, Arrays.asList(currRelation1VO, nextRelation1VO));
						}
						else if (ind2 == 3) {
							nextToNextRelation1VO = new Relation1VO(Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1+2).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2)), Constants.RELATION_NAME_TO_ID_MAP.get(simplifiedRelationVOList.get(ind1+2).getAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1)));
							simplifiedRelation = getSimplifiedRelation(simplificationRuleVOList, Arrays.asList(currRelation1VO, nextRelation1VO, nextToNextRelation1VO));
						}
					}
					if (ind2 == MAX_RELATION_PATH_FOR_SIMPLIFICATION && simplifiedRelation == null) {
						LogManager.getLogger().debug("No simplification");
						newSimplifiedRelationVOList.add(simplifiedRelationVOList.get(ind1));
					}
					else if (simplifiedRelation != null) {
						LogManager.getLogger().debug("Simplification");
						noNewSimplification = false;
						relationVO = new RelationVO();
						additionalRelationVOList.add(relationVO);
						newSimplifiedRelationVOList.add(relationVO);
						simpleRelationInd++;
						relationVO.setId("S"+String.valueOf(simpleRelationInd));
						relationVO.determineSource(simplifiedRelationVOList.get(ind1).getSource());
						relationVO.determineTarget(simplifiedRelationVOList.get(ind1 + ind2 - 1).getTarget());
						relationVO.setSize(0.5);
						relationVO.setType(Constants.EDGE_TYPE_SIMPLIFIED_RELATION);
						relName1Dv = domainValueRepository.findById(Long.valueOf(simplifiedRelation.person1ForPerson2DvId))
								.orElseThrow(() -> new AppException("Invalid Dv Id ", null));
						relName2Dv = domainValueRepository.findById(Long.valueOf(simplifiedRelation.person2ForPerson1DvId))
								.orElseThrow(() -> new AppException("Invalid Dv Id ", null));
						relationVO.setAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON1_FOR_PERSON2, relName1Dv.getValue(), relName1Dv.getDvValue());
						relationVO.setAttribute(Constants.RELATION_ATTRIBUTE_DV_ID_PERSON2_FOR_PERSON1, relName1Dv.getValue(), relName2Dv.getDvValue());
						relationVO.determineLabel(false);
						ind1 = ind1 + ind2 - 1;
						break; // from for ind2
					}
					// else next ind2
				} // for ind2
				ind1++;
			}	// while ind1
		} // while newSimplification
		LogManager.getLogger().debug("Additions: " + additionalRelationVOList.size());
		graphVO.getEdges().addAll(additionalRelationVOList);
	}
	
	@SuppressWarnings("unused")
	private Relation1VO createRelation1VO(String rel1, String rel2) {
		if (rel1.equals(Constants.RELATION_NAME_FATHER) || rel1.equals(Constants.RELATION_NAME_MOTHER) || rel1.equals(Constants.RELATION_NAME_HUSBAND)) {
			return new Relation1VO(rel1, rel2);
		}
		else {
			return new Relation1VO(rel2, rel1);
		}
	}
	
	private Relation1VO getSimplifiedRelation(List<SimplificationRuleVO> simplificationRuleVOList, List<Relation1VO> existingRelationList) {
		int ind;
		boolean isMatched;
		for (Relation1VO relation1VO: existingRelationList) LogManager.getLogger().debug(relation1VO.person1ForPerson2DvId + "::" + relation1VO.person2ForPerson1DvId);
		for (SimplificationRuleVO simplificationRuleVO : simplificationRuleVOList) {
			if (simplificationRuleVO.ruleList.size() != existingRelationList.size()) {
				continue;
			}
			isMatched = true;
			// for (Relation1VO relation1VO: simplificationRuleVO.ruleList) LogManager.getLogger().debug(relation1VO.person1ForPerson2DvId + "::" + relation1VO.person2ForPerson1DvId);
			for (ind = 0; ind < simplificationRuleVO.ruleList.size(); ind++) {
				if (!simplificationRuleVO.ruleList.get(ind).equals(existingRelationList.get(ind))) {
					isMatched = false;
					break;
				}
			}
			if (isMatched) {
				LogManager.getLogger().debug("Matched!!!");
				return simplificationRuleVO.simplifiedRelation;
			}
		}
		return null;
	}
	
	/* TODO: To be based on project configuration */
	public List<SimplificationRuleVO> getRelationSimplificationRules() {
		List<SimplificationRuleVO> simplificationRuleVOList;
		SimplificationRuleVO simplificationRuleVO;
		
		simplificationRuleVOList = new ArrayList<SimplificationRuleVO>();
		
		/* 3-to-1 */
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);

		/* */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_MALE, Constants.RELATION_NAME_COUSIN_FEMALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_BROTHER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);

		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_COUSIN_SISTER, Constants.RELATION_NAME_COUSIN_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_COUSIN_FEMALE, Constants.RELATION_NAME_COUSIN_MALE);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);

		/* 2-to-1 */
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_BROTHER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_SON);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_BROTHER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_SON, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_FATHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_FATHER, Constants.RELATION_NAME_DAUGHTER);
		
		simplificationRuleVO = new SimplificationRuleVO(Constants.RELATION_NAME_SISTER, Constants.RELATION_NAME_SISTER);
		simplificationRuleVOList.add(simplificationRuleVO);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_DAUGHTER, Constants.RELATION_NAME_MOTHER);
		simplificationRuleVO.addRule(Constants.RELATION_NAME_MOTHER, Constants.RELATION_NAME_DAUGHTER);
		
		return simplificationRuleVOList;
	}
	
    protected class Relation1VO {
    	String person1ForPerson2DvId;
    	String person2ForPerson1DvId;
    	public Relation1VO(String person1ForPerson2DvId, String person2ForPerson1DvId) {
    		this.person1ForPerson2DvId = person1ForPerson2DvId;
    		this.person2ForPerson1DvId = person2ForPerson1DvId;
    	}
    	public boolean equals(Relation1VO otherRelation1VO) {
    		return (this.person1ForPerson2DvId.equals(otherRelation1VO.person1ForPerson2DvId) && this.person2ForPerson1DvId.equals(otherRelation1VO.person2ForPerson1DvId));
    	}
	}
    
    protected class SimplificationRuleVO {
    	List<Relation1VO> ruleList;
    	Relation1VO simplifiedRelation;
    	
    	public SimplificationRuleVO(String simplifiedPerson1ForPerson2DvId, String simplifiedPerson2ForPerson1DvId) {
    		ruleList = new ArrayList<Relation1VO>();
    		simplifiedRelation = new Relation1VO(simplifiedPerson1ForPerson2DvId, simplifiedPerson2ForPerson1DvId);
    	}
    	
    	public void addRule(String person1ForPerson2DvId, String person2ForPerson1DvId) {
    		ruleList.add(new Relation1VO(person1ForPerson2DvId, person2ForPerson1DvId));
    	}
	}
}
