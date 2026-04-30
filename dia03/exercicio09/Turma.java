package dia03.exercicio09;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Turma {

    private final List<Aluno> alunos = new ArrayList<>();

    public void adicionar(Aluno a) {
        alunos.add(a);
    }

    public void limpar() {
        alunos.clear();
    }

    public int tamanho() {
        return alunos.size();
    }

    public List<Aluno> getAlunos() {
        return new ArrayList<>(alunos);
    }

    public double maiorMedia() {
        return alunos.stream().mapToDouble(Aluno::getMedia).max().orElse(0.0);
    }

    public double menorMedia() {
        return alunos.stream().mapToDouble(Aluno::getMedia).min().orElse(0.0);
    }

    public double mediaGeral() {
        return alunos.stream().mapToDouble(Aluno::getMedia).average().orElse(0.0);
    }

    public long contar(String status) {
        return alunos.stream().filter(a -> a.getStatus().equals(status)).count();
    }

    public List<Aluno> melhoresAlunos() {
        double max = maiorMedia();
        return alunos.stream()
                .filter(a -> a.getMedia() == max)
                .collect(Collectors.toList());
    }

    public String relatorioCompleto() {
        return (alunos.isEmpty())
                ? "Nenhum aluno cadastrado."
                : montarRelatorio();
    }

    private String montarRelatorio() {
        StringBuilder sb = new StringBuilder();

        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("           RELATÓRIO INDIVIDUAL\n");
        sb.append("═══════════════════════════════════════════════════\n");
        alunos.forEach(a -> sb.append(a.relatorioIndividual()).append("\n"));

        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("           ESTATÍSTICAS DA TURMA\n");
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append(String.format("Maior média.......: %.2f%n", maiorMedia()));
        sb.append(String.format("Menor média.......: %.2f%n", menorMedia()));
        sb.append(String.format("Média geral.......: %.2f%n", mediaGeral()));

        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("           DISTRIBUIÇÃO DE RESULTADOS\n");
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append(String.format("APROVADOS.........: %d%n", contar("APROVADO")));
        sb.append(String.format("RECUPERAÇÃO.......: %d%n", contar("RECUPERAÇÃO")));
        sb.append(String.format("REPROVADOS........: %d%n", contar("REPROVADO")));

        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("           MELHOR(ES) ALUNO(S)\n");
        sb.append("═══════════════════════════════════════════════════\n");
        List<Aluno> melhores = melhoresAlunos();
        sb.append(String.format("Maior média da turma: %.2f%n", maiorMedia()));
        sb.append("Aluno(s):\n");
        melhores.forEach(a -> sb.append("  • ").append(a.getNome())
                .append(String.format(" (média: %.2f)%n", a.getMedia())));

        return sb.toString();
    }
}
