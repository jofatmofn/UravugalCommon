package org.sakuram.relation.repository;

import java.util.List;

import org.sakuram.relation.bean.DomainValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainValueRepository extends JpaRepository<DomainValue, Long>{
	public List<DomainValue> findAllByOrderByCategoryAscValueAsc();
}
