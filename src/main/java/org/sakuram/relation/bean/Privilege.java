package org.sakuram.relation.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="privilege")
public class Privilege {

	@Id
	@SequenceGenerator(name="privilege_seq_generator",sequenceName="privilege_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="privilege_seq_generator")
	@Column(name="id", nullable=false)
	private long id;
	
	@ManyToOne
	@JoinColumn(name="user_fk", nullable=true)
	private AppUser appUser;
	
	@ManyToOne
	@JoinColumn(name="tenant_fk", nullable=true)
	private Tenant tenant;

	@ManyToOne
	@JoinColumn(name="role_fk", nullable=false)
	private DomainValue role;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public AppUser getAppUser() {
		return appUser;
	}

	public void setAppUser(AppUser appUser) {
		this.appUser = appUser;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	public DomainValue getRole() {
		return role;
	}

	public void setRole(DomainValue role) {
		this.role = role;
	}
	
}
