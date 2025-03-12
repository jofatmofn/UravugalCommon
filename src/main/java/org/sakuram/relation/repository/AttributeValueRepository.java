package org.sakuram.relation.repository;

import java.util.List;
import java.util.Optional;

import org.sakuram.relation.bean.AttributeValue;
import org.sakuram.relation.bean.DomainValue;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.bean.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long>, MultiTenancyInterface {
	Optional<AttributeValue> findByIdAndTenant(Long id, Tenant tenant);
	List<AttributeValue> findByPerson(Person person); // Just as a local fix; Should ideally use person.getAttributeValueList
	List<AttributeValue> findByRelation(Relation relation);
	Optional<AttributeValue> findByPersonAndAttribute(Person person, DomainValue attribute);
	Optional<AttributeValue> findByRelationAndAttribute(Relation relation, DomainValue attribute);
}
