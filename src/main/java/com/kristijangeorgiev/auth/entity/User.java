package com.kristijangeorgiev.auth.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * <h2>User</h2>
 * 
 * @author Kristijan Georgiev
 * 
 *         User entity
 *
 */

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseIdEntity implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String email;
	private String username;
	private String password;
	private boolean enabled;

	@Column(name = "account_locked")
	private boolean accountNonLocked;

	@Column(name = "account_expired")
	private boolean accountNonExpired;

	@Column(name = "credentials_expired")
	private boolean credentialsNonExpired;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "role_user", joinColumns = {
			@JoinColumn(name = "user_id", referencedColumnName = "id") }, inverseJoinColumns = {
					@JoinColumn(name = "role_id", referencedColumnName = "id") })
	private List<Role> roles;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !accountNonExpired;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return !credentialsNonExpired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !accountNonLocked;
	}

	/*
	 * Get roles and permissions and add them as a Set of GrantedAuthority
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

		roles.forEach(r -> {
			authorities.add(new SimpleGrantedAuthority(r.getName()));
			r.getPermissions().forEach(p -> {
				authorities.add(new SimpleGrantedAuthority(p.getName()));
			});
		});

		return authorities;
	}
}
