package com.kristijangeorgiev.auth.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * <h2>BaseIdEntity</h2>
 * 
 * @author Kristijan Georgiev
 * 
 *         MappedSuperclass that extends the {@link BaseEntity} class and is
 *         extended by entity classes that have ID field of type Long
 *
 */

@Data
@MappedSuperclass
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseIdEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long id;

}
