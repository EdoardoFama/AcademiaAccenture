package com.accenture.pessoa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.accenture.pessoa.entity.Pessoa;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

}
