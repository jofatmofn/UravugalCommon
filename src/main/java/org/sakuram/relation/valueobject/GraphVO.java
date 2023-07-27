package org.sakuram.relation.valueobject;

import java.util.List;

public class GraphVO {
	private List<PersonVO> nodes;
	private List<RelationVO> edges;
	
	public List<PersonVO> getNodes() {
		return nodes;
	}
	
	public void setNodes(List<PersonVO> nodes) {
		this.nodes = nodes;
	}
	
	public List<RelationVO> getEdges() {
		return edges;
	}
	
	public void setEdges(List<RelationVO> edges) {
		this.edges = edges;
	}
	
}
