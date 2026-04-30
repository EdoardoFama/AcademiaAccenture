package dia03.exercicio08;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final List<ContaCorrente> contas = new ArrayList<>();
    private static int proximoNumero = 1001;

    private static JComboBox<ContaCorrente> comboContas;
    private static JTextArea areaExtrato;
    private static JLabel labelSaldo;

    public static void main(String[] args) {
        Cliente c1 = new Cliente("Ana", "Silva", "111.222.333-44");
        Cliente c2 = new Cliente("Bruno", "Souza", "555.666.777-88");
        contas.add(new ContaCorrente(proximoNumero++, c1, 1000.0));
        contas.add(new ContaCorrente(proximoNumero++, c2, 500.0));

        SwingUtilities.invokeLater(Main::criarJanela);
    }

    private static void criarJanela() {
        JFrame frame = new JFrame("Banco - Conta Corrente");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.setLocationRelativeTo(null);

        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Conta:"));
        comboContas = new JComboBox<>(contas.toArray(new ContaCorrente[0]));
        comboContas.addActionListener(e -> atualizarTela());
        topo.add(comboContas);

        labelSaldo = new JLabel();
        labelSaldo.setFont(labelSaldo.getFont().deriveFont(Font.BOLD, 14f));
        topo.add(Box.createHorizontalStrut(20));
        topo.add(labelSaldo);

        principal.add(topo, BorderLayout.NORTH);

        areaExtrato = new JTextArea();
        areaExtrato.setEditable(false);
        areaExtrato.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        principal.add(new JScrollPane(areaExtrato), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new GridLayout(2, 3, 8, 8));

        JButton btnDepositar = new JButton("Depositar");
        JButton btnSacar = new JButton("Sacar");
        JButton btnTransferir = new JButton("Transferir");
        JButton btnExtrato = new JButton("Atualizar Extrato");
        JButton btnNovaConta = new JButton("Nova Conta");
        JButton btnSair = new JButton("Sair");

        btnDepositar.addActionListener(e -> acaoDepositar());
        btnSacar.addActionListener(e -> acaoSacar());
        btnTransferir.addActionListener(e -> acaoTransferir());
        btnExtrato.addActionListener(e -> atualizarTela());
        btnNovaConta.addActionListener(e -> acaoNovaConta());
        btnSair.addActionListener(e -> System.exit(0));

        botoes.add(btnDepositar);
        botoes.add(btnSacar);
        botoes.add(btnTransferir);
        botoes.add(btnExtrato);
        botoes.add(btnNovaConta);
        botoes.add(btnSair);

        principal.add(botoes, BorderLayout.SOUTH);

        frame.setContentPane(principal);
        frame.setVisible(true);

        atualizarTela();
    }

    private static ContaCorrente contaSelecionada() {
        return (ContaCorrente) comboContas.getSelectedItem();
    }

    private static void atualizarTela() {
        ContaCorrente c = contaSelecionada();
        if (c == null) {
            areaExtrato.setText("");
            labelSaldo.setText("");
            return;
        }
        areaExtrato.setText(c.exibirExtrato());
        labelSaldo.setText("Saldo: R$ " + String.format("%.2f", c.getSaldo()));
    }

    private static Double pedirValor(String titulo) {
        String s = JOptionPane.showInputDialog(null, "Valor (R$):", titulo, JOptionPane.QUESTION_MESSAGE);
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Valor inválido!", "Erro", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static void acaoDepositar() {
        ContaCorrente c = contaSelecionada();
        if (c == null) return;
        Double v = pedirValor("Depósito");
        if (v == null) return;
        if (c.depositar(v)) {
            atualizarTela();
        } else {
            JOptionPane.showMessageDialog(null, "Não foi possível depositar esse valor.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void acaoSacar() {
        ContaCorrente c = contaSelecionada();
        if (c == null) return;
        Double v = pedirValor("Saque");
        if (v == null) return;
        if (c.sacar(v)) {
            atualizarTela();
        } else {
            JOptionPane.showMessageDialog(null, "Saldo insuficiente ou valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void acaoTransferir() {
        ContaCorrente origem = contaSelecionada();
        if (origem == null) return;

        List<ContaCorrente> destinos = new ArrayList<>();
        for (ContaCorrente c : contas) {
            if (c != origem) destinos.add(c);
        }
        if (destinos.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Não há outras contas para transferir.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ContaCorrente destino = (ContaCorrente) JOptionPane.showInputDialog(
                null, "Conta destino:", "Transferência",
                JOptionPane.QUESTION_MESSAGE, null,
                destinos.toArray(), destinos.get(0));
        if (destino == null) return;

        Double v = pedirValor("Transferência");
        if (v == null) return;

        if (origem.transferir(destino, v)) {
            atualizarTela();
            JOptionPane.showMessageDialog(null, "Transferência realizada com sucesso!");
        } else {
            JOptionPane.showMessageDialog(null, "Não foi possível transferir (saldo ou valor inválido).", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void acaoNovaConta() {
        JTextField nome = new JTextField();
        JTextField sobrenome = new JTextField();
        JTextField cpf = new JTextField();
        JTextField saldo = new JTextField("0");

        Object[] campos = {
                "Nome:", nome,
                "Sobrenome:", sobrenome,
                "CPF:", cpf,
                "Saldo inicial:", saldo
        };

        int op = JOptionPane.showConfirmDialog(null, campos, "Nova Conta", JOptionPane.OK_CANCEL_OPTION);
        if (op != JOptionPane.OK_OPTION) return;

        if (nome.getText().isBlank() || sobrenome.getText().isBlank() || cpf.getText().isBlank()) {
            JOptionPane.showMessageDialog(null, "Preencha todos os campos.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double s;
        try {
            s = Double.parseDouble(saldo.getText().replace(",", "."));
            if (s < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Saldo inicial inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Cliente cli = new Cliente(nome.getText().trim(), sobrenome.getText().trim(), cpf.getText().trim());
        ContaCorrente nova = new ContaCorrente(proximoNumero++, cli, s);
        contas.add(nova);
        comboContas.addItem(nova);
        comboContas.setSelectedItem(nova);
        atualizarTela();
    }
}
