package org.sakuram.relation.bean;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Where;
import org.sakuram.relation.util.SecurityContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@EnableAutoConfiguration
@ComponentScan
@Entity
@Where(clause="overwritten_by_fk is null and deleter_fk is null")
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = "long")})
@Filter(name = "tenantFilter", condition = "tenant_fk = :tenantId")
@Table(name="relation")
public class Relation {

	@Id	 
	@SequenceGenerator(name="relation_seq_generator",sequenceName="relation_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="relation_seq_generator")
	@Column(name="id", nullable=false)
	private long id;
	
	@ManyToOne
	@JoinColumn(name="person_1_fk", nullable=true)
	private Person person1;
	
	@ManyToOne
	@JoinColumn(name="person_2_fk", nullable=true)
	private Person person2;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="tenant_fk", nullable=false)
	private Tenant tenant;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="creator_fk", nullable=false)
	private AppUser creator;
	
	@ManyToOne
	@JoinColumn(name="source_fk", nullable=true)
	private Person source;
	
	@Column(name="created_at", nullable=false, updatable=false)
	@CreationTimestamp
	private Timestamp createdAt;

	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="overwritten_by_fk", nullable=true)
	private Relation overwrittenBy;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="deleter_fk", nullable=true)
	private AppUser deleter;
	
	@Column(name="deleted_at", nullable=true)
	private Timestamp deletedAt;

	@JsonIgnore
	@OneToMany(mappedBy="relation", cascade=CascadeType.ALL)
	@Where(clause="overwritten_by_fk is null and deleter_fk is null")
	private List<AttributeValue> attributeValueList;

	public Relation(Person person1, Person person2, Person source) {
		this.person1 = person1;
		this.person2 = person2;
		this.creator = SecurityContext.getCurrentUser();
		this.source = source;
	}
	
	@PrePersist
	@PreUpdate
	public void prePersist() {
	    tenant = SecurityContext.getCurrentTenant();
	}
	
}
