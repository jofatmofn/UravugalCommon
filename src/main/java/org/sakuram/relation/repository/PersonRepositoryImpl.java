package org.sakuram.relation.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.logging.log4j.LogManager;
import org.sakuram.relation.bean.Person;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepositoryImpl implements PersonRepositoryCustom {

    @PersistenceContext
    public EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@Override
	public List<Person> executeDynamicQuery(String queryString) {
    	LogManager.getLogger().info(queryString);
    	Query query;
    	query = entityManager.createNativeQuery(queryString, Person.class);
    	return query.getResultList();
	}

}
