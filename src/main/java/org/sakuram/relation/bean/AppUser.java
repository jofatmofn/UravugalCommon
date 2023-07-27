package org.sakuram.relation.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="app_user", uniqueConstraints=@UniqueConstraint(columnNames={"identity_provider","identity_provider_user_id"}))
public class AppUser {

	@Id
	@SequenceGenerator(name="app_user_seq_generator",sequenceName="app_user_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="app_user_seq_generator")
	@Column(name="id", nullable=false)
	private long id;
	
	@Column(name="identity_provider_user_id", nullable=false)
	private String identityProviderUserId;

	@Column(name="identity_provider", nullable=false)
	private String identityProvider;

	@Column(name="email_id", nullable=true)
	private String emailId;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getIdentityProviderUserId() {
		return identityProviderUserId;
	}

	public void setIdentityProviderUserId(String identityProviderUserId) {
		this.identityProviderUserId = identityProviderUserId;
	}

	public String getIdentityProvider() {
		return identityProvider;
	}

	public void setIdentityProvider(String identityProvider) {
		this.identityProvider = identityProvider;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

}
