package com.accenture.pessoa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import com.accenture.pessoa.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	UserDetails findByLogin(String login);

}
