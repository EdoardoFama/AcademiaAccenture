package com.accenture.pessoa.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accenture.pessoa.entity.Pessoa;
import com.accenture.pessoa.service.PessoaService;

@RestController
@RequestMapping(value = "/pessoas")
public class PessoaController {

	@Autowired
	private PessoaService service;

	@GetMapping
	public List<Pessoa> getAllPessoas() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Pessoa> getPessoaById(@PathVariable Long id) {
		Optional<Pessoa> p = service.getPessoaById(id);
		return p.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public Pessoa createPessoa(@RequestBody Pessoa pessoa) {
		return service.save(pessoa);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Pessoa> updateContato(@PathVariable Long id, @RequestBody Pessoa pessoaDetails) {
		Optional<Pessoa> updatePessoa = service.update(id, pessoaDetails);
		return updatePessoa.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
