package org.sakuram.relation.repository;

import org.sakuram.relation.bean.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
	Tenant findByProjectId(String projectId);
}
