package org.sakuram.relation.repository;

import org.sakuram.relation.bean.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	AppUser findByIdentityProviderAndIdentityProviderUserId(String identityProvider, String identityProviderUserId);
}
