package org.app;


import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.app.config.CalculationProperties;

@SpringBootApplication(exclude = { QdrantVectorStoreAutoConfiguration.class, OpenAiEmbeddingAutoConfiguration.class })
@EnableConfigurationProperties(CalculationProperties.class)
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
