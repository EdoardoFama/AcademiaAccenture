package com.accenture.pessoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.accenture.pessoa.entity.Pessoa;
import com.accenture.pessoa.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
public class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private PessoaService pessoaService;

    @Test
    public void testSavePessoa() {
        // 1. Arrange (Preparar os dados)
        Pessoa pessoaParaSalvar = new Pessoa();
        pessoaParaSalvar.setName("Vadik");
        pessoaParaSalvar.setAge(24);
        pessoaParaSalvar.setEmail("vadik@yahoo.co.in");

        Pessoa pessoaRetornadaPeloBanco = new Pessoa();
        pessoaRetornadaPeloBanco.setId(1L);
        pessoaRetornadaPeloBanco.setName("Vadik");
        pessoaRetornadaPeloBanco.setAge(24);
        pessoaRetornadaPeloBanco.setEmail("vadik@yahoo.co.in");

        // Quando o Service pedir pro Repository salvar, retorne a pessoa mockada com ID 1
        when(pessoaRepository.save(any(Pessoa.class))).thenReturn(pessoaRetornadaPeloBanco);

        // 2. Act (Ação - Chamar o Service)
        Pessoa resultado = pessoaService.save(pessoaParaSalvar);

        // 3. Assert (Verificar se a camada Service funcionou corretamente)
        assertNotNull(resultado, "O resultado não deveria ser nulo!");
        assertEquals(1L, resultado.getId(), "O ID gerado deve ser 1");
        assertEquals("Vadik", resultado.getName(), "O nome salvo deve ser Vadik");
        
        // Verifica se a camada de Service realmente repassou a responsabilidade para o Repository
        verify(pessoaRepository).save(any(Pessoa.class));
    }
}
