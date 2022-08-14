package com.barbenders.liftbot.app;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.event.AppHomeOpenedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;

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
        initAddExerciseHomeView(liftbotApp);
//        liftbotApp.event(AppMentionEvent.class, (payload,context) ->{
//            if(payload.getEvent().getText().contains("hello")) {
//                String userName = context.client().usersInfo(r -> r
//                        .token(context.getBotToken())
//                        .user(payload.getEvent().getUser())).getUser().getRealName();
//                context.say("hey " + userName);
//            } else {
//                context.say("beep boop here comes the joop");
//            }
//            return context.ack();
//        });
        return liftbotApp;
    }

    private void initBotCommands(App liftbotApp) {
        liftbotApp.command("/lbmenu", (slashCommandRequest, context) -> {
            StringBuilder menu = new StringBuilder("----COMMANDS AVAILABLE----\n");
            menu.append("/lbmenu  [print command menu]\n");
            menu.append("/add    [COMING SOON]\n");
            menu.append("/get    [COMING SOON]\n");
            menu.append("/update [COMING SOON]\n");
            menu.append("/delete [COMING SOON]\n");
            return context.ack(menu.toString());
        });

        liftbotApp.command("/add", (slashCommandRequest, context) -> {
            LOGGER.debug("/add command received: {}",slashCommandRequest.getPayload().getText());
            return context.ack("command received: " + slashCommandRequest.getPayload().getText());
        });
    }

    private void initAddExerciseHomeView(App liftbotApp) {
        liftbotApp.event(AppHomeOpenedEvent.class, (request, context) ->{
            String userId = request.getEvent().getUser();
            try {
                ViewsPublishRequest addView = ViewsPublishRequest.builder()
                        .viewAsString(new String(Files.readAllBytes(new ClassPathResource("add_exercise.json").getFile().toPath())))
                        .token(System.getenv("SLACK_BOT_TOKEN"))
                        .userId(userId)
                        .build();
                context.client().viewsPublish(addView);
            } catch(Exception e) {
                LOGGER.error("Exception: ",e);
            }
            return context.ack();
        });
    }
}
