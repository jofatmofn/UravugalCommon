package org.sakuram.relation.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.sakuram.relation.util.SecurityContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EnableAutoConfiguration
@ComponentScan
@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name="translation")
public class Translation {

	@Id
	@SequenceGenerator(name="translation_seq_generator",sequenceName="translation_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="translation_seq_generator")
	@Column(name="id", nullable=false)
	private long id;
	
	@ManyToOne
	@JoinColumn(name="attribute_value_fk", nullable=true)
	private AttributeValue attributeValue;
	
	@ManyToOne
	@JoinColumn(name="domain_value_fk", nullable=true)
	private DomainValue domainValue;
	
	@ManyToOne
	@JoinColumn(name="language_fk", nullable=false)
	private DomainValue language;
	
	@Column(name="value", nullable=false)
	private String value;
	
	public Translation(AttributeValue attributeValue,  DomainValue domainValue, String value) {
		this.attributeValue = attributeValue;
		this.domainValue = domainValue;
		this.language = SecurityContext.getCurrentLanguageDv();
		this.value = value;
	}
	
	@PrePersist
	@PreUpdate
	public void prePersist() {
		language = SecurityContext.getCurrentLanguageDv();
	}

}
