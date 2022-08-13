package com.barbenders.liftbot.app;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.model.event.AppMentionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiftbotApp {

    @Bean
    public AppConfig loadSingleWorkspaceAppConfig() {
        return AppConfig.builder()
                .singleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"))
                .signingSecret(System.getenv("SLACK_SIGNING_SECRET"))
                .build();
    }

    @Bean
    public App initLiftbotApp(AppConfig config){
        App liftbotApp = new App(config);

        liftbotApp.command("/hello", (slashCommandRequest, context) -> context.ack("BeepBoop here comes the joop"));
        liftbotApp.event(AppMentionEvent.class, (payload,context) ->{
            context.say(payload.toString());
            return context.ack();
        });
        return liftbotApp;
    }
}
