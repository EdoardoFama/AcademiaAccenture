package com.accenture.consumo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.accenture.consumo.interfaces.CepService;
import com.accenture.consumo.interfaces.EnderecoRepository;
import com.accenture.consumo.model.Endereco;

@RestController
@CrossOrigin(origins = "*") // Permite acesso do front-end
public class CepRestController {

	@Autowired
	private CepService cepService;
	
	@Autowired
	private EnderecoRepository enderecoRepository;

	@GetMapping("/api/{cep}")
	public ResponseEntity<Endereco> getCep(@PathVariable String cep) {
		
		Endereco endereco = cepService.buscaEnderecoPorCep(cep);
		
		if (endereco != null && endereco.getCep() != null) {
			// Salvar no Banco de Dados
			enderecoRepository.save(endereco);
			return ResponseEntity.ok().body(endereco);
		} else {
			return ResponseEntity.notFound().build(); 
		}
	}

}
