package dia03.exercicio08;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ContaCorrente {
    private int numero;
    private Cliente cliente;          
    private double saldo;
    private LocalDateTime data;     
    private List<String> historico;

    public ContaCorrente(int numero, Cliente cliente, double saldoInicial) {
        this.numero = numero;
        this.cliente = cliente;
        this.saldo = saldoInicial;
        this.data = LocalDateTime.now();
        this.historico = new ArrayList<>();
        registrar("Conta aberta com saldo inicial de R$ " + String.format("%.2f", saldoInicial));
    }

    private void registrar(String descricao) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        historico.add("[" + LocalDateTime.now().format(fmt) + "] " + descricao);
    }

    public boolean depositar(double valor) {
        if (valor <= 0) {
            return false;
        }
        saldo += valor;
        registrar("Depósito: +R$ " + String.format("%.2f", valor)
                + " | Saldo: R$ " + String.format("%.2f", saldo));
        return true;
    }

    public boolean sacar(double valor) {
        if (valor <= 0 || valor > saldo) {
            return false;
        }
        saldo -= valor;
        registrar("Saque:   -R$ " + String.format("%.2f", valor)
                + " | Saldo: R$ " + String.format("%.2f", saldo));
        return true;
    }

    public boolean transferir(ContaCorrente destino, double valor) {
        if (destino == null || valor <= 0 || valor > saldo) {
            return false;
        }
        this.saldo -= valor;
        destino.saldo += valor;

        this.registrar("Transferência enviada: -R$ " + String.format("%.2f", valor)
                + " para conta " + destino.getNumero()
                + " (" + destino.getCliente().getNomeCompleto() + ")"
                + " | Saldo: R$ " + String.format("%.2f", this.saldo));

        destino.registrar("Transferência recebida: +R$ " + String.format("%.2f", valor)
                + " da conta " + this.numero
                + " (" + this.getCliente().getNomeCompleto() + ")"
                + " | Saldo: R$ " + String.format("%.2f", destino.saldo));
        return true;
    }

    public String exibirExtrato() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("======== EXTRATO ========\n");
        sb.append("Conta nº: ").append(numero).append("\n");
        sb.append("Titular:  ").append(cliente.getNomeCompleto()).append("\n");
        sb.append("CPF:      ").append(cliente.getCpf()).append("\n");
        sb.append("Aberta em: ").append(data.format(fmt)).append("\n");
        sb.append("-------------------------\n");
        sb.append("Movimentações:\n");
        if (historico.isEmpty()) {
            sb.append("  (nenhuma movimentação)\n");
        } else {
            for (String mov : historico) {
                sb.append("  ").append(mov).append("\n");
            }
        }
        sb.append("-------------------------\n");
        sb.append("Saldo atual: R$ ").append(String.format("%.2f", saldo)).append("\n");
        sb.append("=========================\n");
        return sb.toString();
    }

    public int getNumero() { return numero; }
    public Cliente getCliente() { return cliente; }
    public double getSaldo() { return saldo; }
    public LocalDateTime getData() { return data; }

    @Override
    public String toString() {
        return "Conta " + numero + " - " + cliente.getNomeCompleto();
    }
}
