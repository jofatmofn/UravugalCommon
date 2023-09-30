package org.sakuram.relation.algo;
// https://www.geeksforgeeks.org/design-data-structures-for-a-very-large-social-network-like-facebook-or-linkedln/

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.repository.RelationRepository;
import org.sakuram.relation.util.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ShortestPathBreadthFirst {
	
	@Autowired
	RelationRepository relationRepository;
	
	public LinkedList<Person> findPathBiBFS(Map<Long, Person> people, long source, long destination, List<String> excludeRelationIdList, List<String> excludePersonIdList) {
		if (people.get(source) == null || people.get(destination) == null) {
			throw new AppException("Person 1 and/or Person 2 do not belong to the project", null);
		}
		BFSData sourceData = new BFSData(people.get(source));
		BFSData destData = new BFSData(people.get(destination));
		
		while (!sourceData.isFinished() && !destData.isFinished()) {
			/* Search out from source. */
			Person collision = searchLevel(people, sourceData, destData, excludeRelationIdList, excludePersonIdList);
			if (collision != null) {
				return mergePaths(sourceData, destData, collision.getId());
			}
			
			/* Search out from destination. */
			collision = searchLevel(people, destData, sourceData, excludeRelationIdList, excludePersonIdList);
			if (collision != null) {
				return mergePaths(sourceData, destData, collision.getId());
			}
		}
		return null;
	}

	/* Search one level and return collision, if any.*/
	Person searchLevel(Map<Long, Person> people, BFSData primary, BFSData secondary, List<String> excludeRelationIdList, List<String> excludePersonIdList) {
		/* We only want to search one level at a time. Count how many nodes are currently in the 
		 * primary's level and only do that many nodes. We continue to add nodes to the end. */
		int count = primary.toVisit.size();
		for (int i= 0; i < count; i++) {
			/* Pull out first node. */
			PathNode pathNode = primary.toVisit.poll();
			long personId = pathNode.getPerson().getId(); 

			/* Check if it's already been visited. */
			if (secondary.visited.containsKey(personId)) {
				return pathNode.getPerson(); 
			}

			/* Add friends to queue. */
			Person person = pathNode.getPerson();
			
	    	Set<Person> relatedPersonSet;
	    	List<Relation> participatingRelationList;
	    	
	    	relatedPersonSet = new HashSet<Person>();
	    	participatingRelationList = relationRepository.findByPerson1(person);
	    	for (Relation relation : participatingRelationList) {
	    		if (!excludeRelationIdList.contains(String.valueOf(relation.getId())) && !excludePersonIdList.contains(String.valueOf(relation.getPerson2().getId()))) {
	    			relatedPersonSet.add(relation.getPerson2());
	    		}
	    	}
	    	participatingRelationList = relationRepository.findByPerson2(person);
	    	for (Relation relation : participatingRelationList) {
	    		if (!excludeRelationIdList.contains(String.valueOf(relation.getId())) && !excludePersonIdList.contains(String.valueOf(relation.getPerson1().getId()))) {
	    			relatedPersonSet.add(relation.getPerson1());
	    		}
	    	}
			for (Person friend : relatedPersonSet) {
				long friendId = friend.getId();
				if (!primary.visited.containsKey(friendId)) {
					// Person friend = people.get(friendId);
					PathNode next = new PathNode(friend, pathNode);
					primary.visited.put(friendId, next);
					primary.toVisit.add(next);
				}
			}
		}
		return null;
	}
	
	/* Merge paths where searches met at the connection. */
	LinkedList<Person> mergePaths(BFSData bfsl, BFSData bfs2, long connection) {
		// endl -> source, end2 -> dest
		PathNode endl = bfsl.visited.get(connection);
		PathNode end2 = bfs2.visited.get(connection);
		
		LinkedList<Person> pathOne = endl.collapse(false);
		LinkedList<Person> pathTwo = end2.collapse(true);
		
		pathTwo.removeFirst(); // remove connection
		pathOne.addAll(pathTwo); // add second path
		
		return pathOne;
	}
	
	class PathNode {
		private Person person = null;
		private PathNode previousNode = null;
		public PathNode(Person p, PathNode previous) {
			person = p;
			previousNode = previous;
		}
		
		public Person getPerson() {
			return person;
		}
		
		public LinkedList<Person> collapse(boolean startsWithRoot) {
			LinkedList<Person> path= new LinkedList<Person>();
			PathNode node = this;
			while (node != null) {
				if (startsWithRoot) {
					path.addLast(node.person);
				}
				else {
					path.addFirst(node.person);
				}
				node = node.previousNode;
			}
			
			return path;
		}
	}
	
	class BFSData {
		public Queue<PathNode> toVisit = new LinkedList<PathNode>();
		public HashMap<Long, PathNode> visited = new HashMap<Long, PathNode>();
		
		public BFSData(Person root) {
			PathNode sourcePath = new PathNode(root, null);
			toVisit.add(sourcePath);
			visited.put(root.getId(), sourcePath);
		}
		
		public boolean isFinished() {
			return toVisit.isEmpty();
		}
	}
	
}
