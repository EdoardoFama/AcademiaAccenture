package dia03.exercicio09;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AppGUI extends JFrame {

    private final Turma turma = new Turma();

    private final JTextField campoNome = new JTextField(15);
    private final JTextField campoNota1 = new JTextField(5);
    private final JTextField campoNota2 = new JTextField(5);
    private final JTextField campoNota3 = new JTextField(5);

    private final DefaultListModel<String> modeloLista = new DefaultListModel<>();
    private final JList<String> listaAlunos = new JList<>(modeloLista);

    private final JTextArea areaRelatorio = new JTextArea();
    private final JLabel statusBar = new JLabel(" Pronto. Cadastre alunos para começar.");

    public AppGUI() {
        super("Atividade 9 — Sistema de Notas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(montarPainelCadastro(), BorderLayout.WEST);
        add(montarPainelRelatorio(), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        carregarAlunosIniciais();
    }

    private void carregarAlunosIniciais() {
        Object[][] seed = {
            {"Júlio Vieira",  new double[]{100, 100, 100}},
            {"Clodosfalo",    new double[]{ 45,  60,  35}},
            {"Filomena",      new double[]{ 85,  78,  92}},
            {"Rocketnildo",   new double[]{ 55,  68,  62}},
            {"Lidia",         new double[]{ 72,  74,  76}},
            {"Chuck Norris",  new double[]{100, 100, 100}}
        };

        java.util.Arrays.stream(seed).forEach(linha -> {
            Aluno a = new Aluno((String) linha[0], (double[]) linha[1]);
            turma.adicionar(a);
            modeloLista.addElement(a.relatorioIndividual());
        });

        statusBar.setText(" " + turma.tamanho() + " alunos pré-cadastrados. Clique em 'Gerar Relatório'.");
    }

    private JPanel montarPainelCadastro() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(new EmptyBorder(10, 10, 10, 10));
        painel.setPreferredSize(new Dimension(320, 0));

        // ── Formulário ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Cadastro de Aluno"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Nome:"), g);
        g.gridx = 1; g.gridwidth = 3;
        form.add(campoNome, g);

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = 1;
        form.add(new JLabel("Nota 1:"), g);
        g.gridx = 1; form.add(campoNota1, g);

        g.gridx = 0; g.gridy = 2;
        form.add(new JLabel("Nota 2:"), g);
        g.gridx = 1; form.add(campoNota2, g);

        g.gridx = 0; g.gridy = 3;
        form.add(new JLabel("Nota 3:"), g);
        g.gridx = 1; form.add(campoNota3, g);

        JButton btnAdicionar = new JButton("Adicionar Aluno");
        btnAdicionar.addActionListener(this::adicionarAluno);
        g.gridx = 0; g.gridy = 4; g.gridwidth = 4;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(btnAdicionar, g);

        JPanel painelLista = new JPanel(new BorderLayout());
        painelLista.setBorder(new TitledBorder("Alunos Cadastrados"));
        listaAlunos.setVisibleRowCount(8);
        painelLista.add(new JScrollPane(listaAlunos), BorderLayout.CENTER);

        JPanel acoes = new JPanel(new GridLayout(1, 2, 6, 6));
        JButton btnGerar = new JButton("Gerar Relatório");
        JButton btnLimpar = new JButton("Limpar Tudo");
        btnGerar.addActionListener(e -> gerarRelatorio());
        btnLimpar.addActionListener(e -> limparTudo());
        acoes.add(btnGerar);
        acoes.add(btnLimpar);

        painel.add(form);
        painel.add(Box.createVerticalStrut(8));
        painel.add(painelLista);
        painel.add(Box.createVerticalStrut(8));
        painel.add(acoes);

        return painel;
    }

    private JPanel montarPainelRelatorio() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Relatório"));
        areaRelatorio.setEditable(false);
        areaRelatorio.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaRelatorio.setText("\n   Cadastre alunos no painel à esquerda e clique em 'Gerar Relatório'.\n");
        painel.add(new JScrollPane(areaRelatorio), BorderLayout.CENTER);
        return painel;
    }

    private void adicionarAluno(ActionEvent e) {
        try {
            String nome = campoNome.getText().trim();
            Aluno.validarNome(nome);

            double n1 = lerNotaComRetentativa("Nota 1", campoNota1.getText());
            double n2 = lerNotaComRetentativa("Nota 2", campoNota2.getText());
            double n3 = lerNotaComRetentativa("Nota 3", campoNota3.getText());

            Aluno a = new Aluno(nome, new double[]{n1, n2, n3});
            turma.adicionar(a);
            modeloLista.addElement(a.relatorioIndividual());

            limparCamposCadastro();
            statusBar.setText(" Aluno '" + nome + "' adicionado. Total: " + turma.tamanho());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Erro de validação",
                    JOptionPane.ERROR_MESSAGE);
            statusBar.setText(" Erro: " + ex.getMessage());
        } catch (OperacaoCanceladaException ex) {
            statusBar.setText(" Cadastro cancelado pelo usuário.");
        }
    }

    private double lerNotaComRetentativa(String rotulo, String valorInicial)
            throws OperacaoCanceladaException {
        String valor = valorInicial;
        while (true) {
            try {
                double n = Double.parseDouble(valor.replace(",", ".").trim());
                Aluno.validarNota(n);
                return n;
            } catch (IllegalArgumentException ex) {
                String entrada = JOptionPane.showInputDialog(this,
                        rotulo + " inválida (\"" + valor + "\").\n"
                                + "Digite um número entre 0 e 100:",
                        rotulo + " — corrigir",
                        JOptionPane.WARNING_MESSAGE);
                // Sem if: ternária + exceção para sair
                valor = (entrada == null)
                        ? lancarCancelamento()
                        : entrada;
            }
        }
    }

    private String lancarCancelamento() throws OperacaoCanceladaException {
        throw new OperacaoCanceladaException();
    }

    private void gerarRelatorio() {
        areaRelatorio.setText(turma.relatorioCompleto());
        areaRelatorio.setCaretPosition(0);
        statusBar.setText(" Relatório gerado para " + turma.tamanho() + " aluno(s).");
    }

    private void limparTudo() {
        int op = JOptionPane.showConfirmDialog(this,
                "Remover todos os alunos cadastrados?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        // Ternária no lugar do if
        boolean confirmado = (op == JOptionPane.YES_OPTION);
        java.util.Optional.of(confirmado)
                .filter(Boolean::booleanValue)
                .ifPresent(b -> executarLimpeza());
    }

    private void executarLimpeza() {
        turma.limpar();
        modeloLista.clear();
        areaRelatorio.setText("");
        limparCamposCadastro();
        statusBar.setText(" Lista limpa.");
    }

    private void limparCamposCadastro() {
        campoNome.setText("");
        campoNota1.setText("");
        campoNota2.setText("");
        campoNota3.setText("");
        campoNome.requestFocus();
    }

    private static class OperacaoCanceladaException extends Exception {}

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AppGUI().setVisible(true));
    }
}