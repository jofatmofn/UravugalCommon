package org.sakuram.relation.service;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.sakuram.relation.bean.AppUser;
import org.sakuram.relation.bean.DomainValue;
import org.sakuram.relation.bean.Privilege;
import org.sakuram.relation.bean.Tenant;
import org.sakuram.relation.repository.AppUserRepository;
import org.sakuram.relation.repository.DomainValueRepository;
import org.sakuram.relation.repository.PersonRepository;
import org.sakuram.relation.repository.PrivilegeRepository;
import org.sakuram.relation.repository.TenantRepository;
import org.sakuram.relation.util.AppException;
import org.sakuram.relation.util.Constants;
import org.sakuram.relation.valueobject.ProjectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectUserService {
	@Autowired
	TenantRepository tenantRepository;
	@Autowired
	AppUserRepository appUserRepository;
	@Autowired
	PrivilegeRepository privilegeRepository;
	@Autowired
	DomainValueRepository domainValueRepository;
	@Autowired
	PersonRepository personRepository;
	
    public ProjectVO switchProject(String projectId, Long userId) {
    	Tenant tenant;
    	ProjectVO projectVO;
    	
    	if (projectId == null) {
    		throw new AppException("Specify a Project to switch to", null);
    	}
    	tenant = tenantRepository.findByProjectId(projectId);
    	if (tenant == null) {
    		throw new AppException("Specified Project " + projectId + " does not exist", null);
    	}
    	projectVO = new ProjectVO();
    	projectVO.setTenantId(tenant.getId());
    	projectVO.setProjectName(tenant.getProjectName());
    	projectVO.setAppReadOnly(isAppReadOnly(tenant.getId(), userId));
    	projectVO.setPersonCount(personRepository.countByTenant(tenant));

    	return projectVO;
    }

    public Long findOrSaveUser(String identityProvider, String identityProviderUserId, String emailId) {
    	AppUser appUser;
    	
    	appUser = appUserRepository.findByIdentityProviderAndIdentityProviderUserId(identityProvider, identityProviderUserId);
    	if (appUser == null) {
    		appUser = new AppUser();
    		appUser.setIdentityProvider(identityProvider);
    		appUser.setIdentityProviderUserId(identityProviderUserId);
    		appUser.setEmailId(emailId);
    		appUser = appUserRepository.save(appUser);
    	}
    	return appUser.getId();
    }
    
    public boolean isAppReadOnly(Long tenantId, Long appUserId) {
    	AppUser appUser;
    	Tenant tenant;
    	boolean isReadOnly;
    	
    	isReadOnly = true;
    	if (tenantId != null && appUserId != null) {
    		tenant = tenantRepository.findById(tenantId)
    				.orElseThrow(() -> new AppException("Invalid Tenant Id " + tenantId, null));
    		appUser = appUserRepository.findById(appUserId)
    				.orElseThrow(() -> new AppException("Invalid User Id " + appUserId, null));
    		isReadOnly = isAppReadOnly(tenant, appUser);
    	}
		LogManager.getLogger().debug("Tenant: " + tenantId + ". AppUser: " + appUserId + ". Privilege: " + isReadOnly);
    	return isReadOnly;
    }
    
    public boolean isAppReadOnly(Tenant tenant, AppUser appUser) {
    	Privilege privilege;
    	long roleDvId;
    	
		privilege = privilegeRepository.findByTenantAndAppUser(tenant, appUser);
		if (privilege != null) {
			roleDvId = privilege.getRole().getId();
			if (roleDvId == Constants.ROLE_DV_ID_COLLABORATOR || roleDvId == Constants.ROLE_DV_ID_CREATOR) {
				return false;
			}
		}
    	return true;
    }
    
    public Tenant createProject(String projectName, Long appUserId) {
    	Tenant tenant;
    	Random random;
    	String projectId;
    	AppUser appUser;
    	Privilege privilege;
    	DomainValue roleDv;
    	
    	if (projectName == null) {
    		throw new AppException("Specify a name for the Project to be created", null);
    	}
    	if (appUserId == null) {
    		throw new AppException("Login first to create a project", null);
    	}
		appUser = appUserRepository.findById(appUserId)
				.orElseThrow(() -> new AppException("Invalid User Id " + appUserId, null));
		roleDv = domainValueRepository.findById(Constants.ROLE_DV_ID_CREATOR)
				.orElseThrow(() -> new AppException("Invalid Role Dv Id " + Constants.ROLE_DV_ID_CREATOR, null));
    	
        random = new Random();
        projectId = random.ints(48, 122 + 1)
          .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
          .limit(16)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
        
    	tenant = new Tenant();
    	tenant.setProjectId(projectId);
    	tenant.setProjectName(projectName);
        tenant = tenantRepository.save(tenant);

        privilege = new Privilege();
        privilege.setAppUser(appUser);
        privilege.setRole(roleDv);
        privilege.setTenant(tenant);
        privilege = privilegeRepository.save(privilege);
        
    	return tenant;
    }

}
