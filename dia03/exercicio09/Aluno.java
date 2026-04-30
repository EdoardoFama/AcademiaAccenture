package dia03.exercicio09;
public class Aluno {

    private final String nome;
    private final double[] notas;

    public Aluno(String nome, double[] notas) {
        validarNome(nome);
        validarQuantidadeNotas(notas);
        java.util.Arrays.stream(notas).forEach(Aluno::validarNota);

        this.nome = nome;
        this.notas = notas.clone();
    }

    public static void validarNome(String nome) {
        String n = (nome == null) ? "" : nome.trim();
        java.util.Optional.of(n)
                .filter(s -> s.length() >= 3)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nome inválido: deve ter no mínimo 3 caracteres."));
    }

    public static void validarNota(double nota) {
        java.util.Optional.of(nota)
                .filter(v -> v >= 0 && v <= 100)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nota inválida: " + nota + ". Deve estar entre 0 e 100."));
    }

    private static void validarQuantidadeNotas(double[] notas) {
        java.util.Optional.ofNullable(notas)
                .filter(arr -> arr.length == 3)
                .orElseThrow(() -> new IllegalArgumentException(
                        "É necessário fornecer exatamente 3 notas."));
    }

    public String getNome() {
        return nome;
    }

    public double[] getNotas() {
        return notas.clone();
    }

    public double getMedia() {
        return java.util.Arrays.stream(notas).average().orElse(0.0);
    }


    public String getStatus() {
        double media = getMedia();
        return (media >= 70) ? "APROVADO"
                : (media >= 50) ? "RECUPERAÇÃO"
                : "REPROVADO";
    }

    public String relatorioIndividual() {
        return String.format("%s | Notas: %.0f, %.0f, %.0f | Média: %.2f | %s",
                nome, notas[0], notas[1], notas[2], getMedia(), getStatus());
    }
}
