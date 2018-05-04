package com.kristijangeorgiev.auth.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * <h2>BaseEntity</h2>
 * 
 * @author Kristijan Georgiev
 * 
 *         MappedSuperclass that contains all the necessary fields
 *
 */

@Data
@MappedSuperclass
@NoArgsConstructor
public class BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Version
	protected Long version;

	@CreationTimestamp
	protected LocalDateTime createdOn;

	@UpdateTimestamp
	protected LocalDateTime updatedOn;

}
