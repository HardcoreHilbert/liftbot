package com.barbenders.liftbot.app;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.model.event.AppMentionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiftbotApp {

    static Logger LOGGER = LoggerFactory.getLogger(LiftbotApp.class);

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

        initBotCommands(liftbotApp);
        liftbotApp.event(AppMentionEvent.class, (payload,context) ->{
            if(payload.getEvent().getText().contains("hello")) {
                String userName = context.client().usersInfo(r -> r
                        .token(context.getBotToken())
                        .user(payload.getEvent().getUser())).getUser().getRealName();
                context.say("hey " + userName);
            } else {
                context.say("beep boop here comes the joop");
            }
            return context.ack();
        });
        return liftbotApp;
    }

    private void initBotCommands(App liftbotApp) {
        liftbotApp.command("/hello", (slashCommandRequest, context) -> {
            StringBuilder menu = new StringBuilder("----COMMANDS AVAILABLE----\n");
            menu.append("/hello [print command menu]\n");
            menu.append("/add   [COMING SOON]");
            return context.ack(menu.toString());
        });

        liftbotApp.command("/add", (slashCommandRequest, context) -> {
            LOGGER.debug("/add command received: {}",slashCommandRequest.getPayload().getText());
            return context.ack("command received: " + slashCommandRequest.getPayload().getText());
        });
    }
}
