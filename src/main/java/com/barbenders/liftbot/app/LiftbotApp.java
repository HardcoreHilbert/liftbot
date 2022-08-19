package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.ViewState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Configuration
public class LiftbotApp {

    static Logger LOGGER = LoggerFactory.getLogger(LiftbotApp.class);

    @Autowired
    ExerciseRepository repo;

    @Bean
    public AppConfig loadSingleWorkspaceAppConfig() {
        LOGGER.info("loading single workspace app configuration");
        return AppConfig.builder()
                .singleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"))
                .signingSecret(System.getenv("SLACK_SIGNING_SECRET"))
                .build();
    }

    @Bean
    public App initLiftbotApp(AppConfig config){
        LOGGER.info("initializing LiftBot application");
        App liftbotApp = new App(config);


        initAddExerciseHomeView(liftbotApp);
        initSaveAction(liftbotApp);

        return liftbotApp;
    }

    private void initAddExerciseHomeView(App liftbotApp) {
        LOGGER.info("initializing LiftBot view Add Exercise");
        liftbotApp.event(AppHomeOpenedEvent.class, (request, context) -> {
            String userId = request.getEvent().getUser();
            LOGGER.info("user: {}", userId);
            String viewString = new BufferedReader(new InputStreamReader(
                    new ClassPathResource("add_exercise.json").getInputStream()))
                    .lines().collect(Collectors.joining());
            LOGGER.debug("viewString: {}", viewString);
            try {
                ViewsPublishRequest addView = ViewsPublishRequest.builder()
                        .viewAsString(viewString)
                        .token(System.getenv("SLACK_BOT_TOKEN"))
                        .userId(userId)
                        .build();
                context.client().viewsPublish(addView);
            } catch (Exception e) {
                LOGGER.error("Exception: ", e);
            }
            return context.ack();
        });
    }

    private void initSaveAction(App liftbotApp) {
        liftbotApp.blockAction("exercise_save", (request,context) -> {
            ViewState viewState = request.getPayload().getView().getState();
            Exercise exerciseRecord = new Exercise();
            exerciseRecord.setUserid(viewState.getValues().get("user_selection").get("users_select-action").getSelectedUser());
            exerciseRecord.setUserid(viewState.getValues().get("exercise_name_input").get("plain_text_input-action").getValue());
            exerciseRecord.setUserid(viewState.getValues().get("equipment_needed_input").get("plain_text_input-action").getValue());
            exerciseRecord.setUserid(viewState.getValues().get("sets_input").get("plain_text_input-action").getValue());
            exerciseRecord.setUserid(viewState.getValues().get("reps_input").get("plain_text_input-action").getValue());
            exerciseRecord.setUserid(viewState.getValues().get("weight_input").get("plain_text_input-action").getValue());
            LOGGER.info("saving record to database: {}",exerciseRecord);
            repo.save(exerciseRecord);
            context.respond("saved record to database: " + exerciseRecord);
            return context.ack();
        });
    }
}
