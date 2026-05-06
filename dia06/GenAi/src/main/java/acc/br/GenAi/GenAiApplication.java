package acc.br.GenAi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.ai.chat.client.ChatClient;
import acc.br.GenAi.model.ChatHistory;
import acc.br.GenAi.repository.ChatHistoryRepository;
import javax.swing.JOptionPane;
import java.awt.HeadlessException;

@SpringBootApplication
public class GenAiApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(GenAiApplication.class, args);

		try {
			System.setProperty("java.awt.headless", "false");
			String prompt = JOptionPane.showInputDialog("Digite seu prompt para a IA:");
			if (prompt != null && !prompt.isEmpty()) {
				ChatClient chatClient = context.getBean(ChatClient.Builder.class).build();
				String response = chatClient.prompt().user(prompt).call().content();

				ChatHistoryRepository repository = context.getBean(ChatHistoryRepository.class);
				repository.save(new ChatHistory(prompt, response));

				JOptionPane.showMessageDialog(null, "Resposta da IA:\n" + response);
			}
		} catch (HeadlessException e) {
			System.out.println("Ambiente sem interface gráfica. Use os endpoints da API.");
		}
	}

}
