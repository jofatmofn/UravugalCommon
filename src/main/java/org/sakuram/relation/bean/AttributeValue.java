package org.sakuram.relation.bean;

import java.sql.Date;
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
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Where;
import org.sakuram.relation.util.DomainValueFlags;
import org.sakuram.relation.util.SecurityContext;
import org.sakuram.relation.util.UtilFuncs;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EnableAutoConfiguration
@ComponentScan
@Getter @Setter
@NoArgsConstructor
@Entity
@Where(clause="overwritten_by_fk is null and deleter_fk is null")
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = "long")})
@Filter(name = "tenantFilter", condition = "tenant_fk = :tenantId")
@Table(name="attribute_value")
public class AttributeValue {

	@Id	 
	@SequenceGenerator(name="attribute_value_seq_generator",sequenceName="attribute_value_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="attribute_value_seq_generator")
	@Column(name="id", nullable=false)
	private long id;
	
	@ManyToOne
	@JoinColumn(name="person_fk", nullable=true)
	private Person person;
	
	@ManyToOne
	@JoinColumn(name="relation_fk", nullable=true)
	private Relation relation;
	
	@ManyToOne
	@JoinColumn(name="attribute_fk", nullable=false)
	private DomainValue attribute;
	
	@Column(name="attribute_value", nullable=false)
	private String attributeValue;
	
	@Column(name="normalised_value", nullable=true)		// For Search purpose, applicable to free-text values
	private String normalisedValue;
	
	@Column(name="is_value_approximate", nullable=false)
	private boolean isValueApproximate;
	
	@Column(name="start_date", nullable=true)
	private Date startDate;
	
	@Column(name="end_date", nullable=true)
	private Date endDate;
	
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
	private AttributeValue overwrittenBy;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="deleter_fk", nullable=true)
	private AppUser deleter;
	
	@Column(name="deleted_at", nullable=true)
	private Timestamp deletedAt;

	@JsonIgnore
	@OneToMany(mappedBy = "attributeValue", cascade = CascadeType.ALL)
	private List<Translation> translationList;

	@Column(name="is_private", nullable=true)
	private Boolean isPrivate;
	
	@Transient
	private Translation translation;

	public String getAvValue() {
		return translation == null ? attributeValue : translation.getValue();
	}
	
	public AttributeValue(Person source) {
		this.source = source;
		this.creator = SecurityContext.getCurrentUser();
	}
	
	public AttributeValue(DomainValue attribute, String attributeValue, Person person, Relation relation, Person source) {
		this.attribute = attribute;
		this.attributeValue = attributeValue;
		this.person = person;
		this.relation = relation;
		this.creator = SecurityContext.getCurrentUser();
		this.source = source;
	}
	
	public AttributeValue(AttributeValue attributeValue) {	// Clone
		this.attribute = attributeValue.attribute;
		this.attributeValue = attributeValue.attributeValue;
		this.creator = SecurityContext.getCurrentUser();
		this.endDate = attributeValue.endDate;
		this.isValueApproximate = attributeValue.isValueApproximate;
		this.person = attributeValue.person;
		this.relation = attributeValue.relation;
		this.startDate = attributeValue.startDate;
		// source, createdAt, deletedAt, deleter, id, overwrittenBy, tenant, translation, translationList
	}
	
	@PrePersist
	@PreUpdate
	public void prePersist() {
	    StringBuilder sb;
	    DomainValueFlags domainValueFlags;
	    
	    tenant = SecurityContext.getCurrentTenant();
	    domainValueFlags = new DomainValueFlags(attribute);
	    
	    if (domainValueFlags.getIsScriptConvertible() != null && domainValueFlags.getIsScriptConvertible()) {
		    sb = new StringBuilder();
		    for (String alternative : UtilFuncs.normaliseForSearch(this.attributeValue)) {
		    	sb.append("/");
		    	sb.append(alternative);
		    }
	    	sb.append("/");
		    normalisedValue = sb.toString();
	    }
	}

	@PostLoad
	protected void translate() {
		for (Translation translation : translationList) {
			if (SecurityContext.getCurrentLanguageDvId().equals(translation.getLanguage().getId())) {
				this.translation = translation;
				break;
			}
		}
	}
}
