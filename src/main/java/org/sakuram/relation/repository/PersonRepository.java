package org.sakuram.relation.repository;

import java.util.List;
import java.util.Optional;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, Long>, PersonRepositoryCustom, MultiTenancyInterface {
	Optional<Person> findByIdAndTenant(Long id, Tenant tenant);
	
	@org.springframework.data.jpa.repository.Query(nativeQuery = true,
			value = "SELECT * FROM person p WHERE p.tenant_fk = :tenant AND p.overwritten_by_fk IS NULL AND p.deleter_fk IS NULL " +
					"AND EXISTS (SELECT 1 FROM relation r WHERE r.person_1_fk = :person1 AND r.person_2_fk = p.id AND " +
					"EXISTS (SELECT 1 FROM attribute_value av WHERE av.overwritten_by_fk IS NULL AND av.deleter_fk IS NULL " +
					"AND av.relation_fk = r.id AND av.attribute_fk = 35 AND av.attribute_value IN ('5', '6')))")
	public List<Person> findKids(@Param("person1") long person1, @Param("tenant") long tenant);
	
	public long countByTenant(Tenant tenant);
}
