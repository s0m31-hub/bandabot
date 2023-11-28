package org.nwolfhub.bandabot;

import org.nwolfhub.bandabot.telegram.TelegramHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class BandabotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BandabotApplication.class, args);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configuration.class);
		TelegramHandler handler = context.getBean(TelegramHandler.class);
		handler.startListening();
	}
}
