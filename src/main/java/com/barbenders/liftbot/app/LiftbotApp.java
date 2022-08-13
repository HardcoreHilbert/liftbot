package com.barbenders.liftbot.app;

import com.slack.api.bolt.App;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiftbotApp {
    @Bean
    public App initLiftbotApp(){
        App liftbotApp = new App();
        liftbotApp.command("/hello", (slashCommandRequest, context) -> context.ack("BeepBoop here comes the joop"));
        return liftbotApp;
    }
}
