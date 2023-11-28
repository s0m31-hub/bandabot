package org.nwolfhub.bandabot;

import org.nwolfhub.bandabot.telegram.TelegramHandler;
import org.nwolfhub.bandabot.wolvesville.WereWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class BandabotApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(BandabotApplication.class, args);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configuration.class);
		TelegramHandler handler = context.getBean(TelegramHandler.class);
		handler.startListening();
		WereWorker worker = context.getBean(WereWorker.class);
		worker.launch();
	}
}
