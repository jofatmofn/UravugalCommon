package org.sakuram.relation.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Relation;
import org.sakuram.relation.bean.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelationRepository extends JpaRepository<Relation, Long>, MultiTenancyInterface {
	Optional<Relation> findByIdAndTenant(Long id, Tenant tenant);
	List<Relation> findByPerson1(Person person1);
	List<Relation> findByPerson2(Person person2);
	Relation findByPerson1AndPerson2(Person person1, Person person2);	// TODO: If more than 1 entity instance is returned?
	List<Relation> findByPerson1InAndPerson2In(Set<Person> person1Set, Set<Person> person2Set);
	
	@Query(nativeQuery = true,
			value = "SELECT * FROM RELATION r WHERE (r.person_1_fk = :person1 AND r.person_2_fk = :person2 OR " +
					"r.person_1_fk = :person2 AND r.person_2_fk = :person1) " +
					"AND r.tenant_fk = :tenant AND r.overwritten_by_fk IS NULL AND r.deleter_fk IS NULL")
	Relation findRelationGivenPersons(@Param("person1") long person1, @Param("person2") long person2, @Param("tenant") long tenant);
}
