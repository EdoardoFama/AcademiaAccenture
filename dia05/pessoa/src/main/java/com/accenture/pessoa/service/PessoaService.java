package com.accenture.pessoa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.accenture.pessoa.entity.Pessoa;
import com.accenture.pessoa.repository.PessoaRepository;

@Service
public class PessoaService {

    @Autowired
    private PessoaRepository pessoaRepository;

    public List<Pessoa> findAll() {
        return pessoaRepository.findAll();
    }

    public Optional<Pessoa> findById(long id) {
        return pessoaRepository.findById(id);
    }

    public Pessoa save(Pessoa pessoa) {
        return pessoaRepository.save(pessoa);
    }

    public Optional<Pessoa> update(long id, Pessoa newPessoa) {
        Optional<Pessoa> oldPessoa = pessoaRepository.findById(id);
        if (oldPessoa.isPresent()) {
            Pessoa pessoa = oldPessoa.get();
            pessoa.setName(newPessoa.getName());
            pessoa.setAge(newPessoa.getAge());
            pessoa.setEmail(newPessoa.getEmail());
            return Optional.of(pessoaRepository.save(pessoa));
        }
        return Optional.empty();
    }

    public boolean delete(long id) {
        Optional<Pessoa> pessoa = pessoaRepository.findById(id);
        if (pessoa.isPresent()) {
            pessoaRepository.delete(pessoa.get());
            return true;
        }
        return false;
    }
}
