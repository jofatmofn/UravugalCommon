package org.sakuram.relation.bean;

import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.sakuram.relation.util.SecurityContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@EnableAutoConfiguration
@ComponentScan
@Getter @Setter
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY)
@Table(name="domain_value")
public class DomainValue {

	@Id	 
	@Column(name="id", nullable=false)
	private long id;
	
	@Column(name="category", nullable=false)
	private String category;
	
	@Column(name="value", nullable=false)
	private String value;
	
	@Column(name="flags_csv", nullable=true)
	private String flagsCsv;

	@JsonIgnore
	@OneToMany(mappedBy = "domainValue", cascade = CascadeType.ALL)
	private List<Translation> translationList;

	@Transient
	private String translatedValue;
	
	public String getDvValue() {
		return translatedValue == null ? value : translatedValue;
	}
	
	@PostLoad
	protected void translate() {
		for (Translation translation : translationList) {
			if (SecurityContext.getCurrentLanguageDvId().equals(translation.getLanguage().getId())) {
				translatedValue = translation.getValue();
				break;
			}
		}
	}
}
