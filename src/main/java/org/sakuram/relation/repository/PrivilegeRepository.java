package org.sakuram.relation.repository;

import org.sakuram.relation.bean.Privilege;
import org.sakuram.relation.bean.Tenant;
import org.sakuram.relation.bean.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
	Privilege findByTenantAndAppUser(Tenant tenant, AppUser appUser);
}
