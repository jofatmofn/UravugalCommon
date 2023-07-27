package org.sakuram.relation.repository;

import java.util.Optional;
import org.sakuram.relation.bean.Person;
import org.sakuram.relation.bean.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long>, PersonRepositoryCustom, MultiTenancyInterface {
	Optional<Person> findByIdAndTenant(Long id, Tenant tenant);
}
