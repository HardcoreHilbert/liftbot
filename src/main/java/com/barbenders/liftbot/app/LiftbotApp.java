package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class LiftbotApp {

    static Logger LOGGER = LoggerFactory.getLogger(LiftbotApp.class);

    private static final String OPEN = "&lt;";
    private static final String CLOSE = "&gt;";
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
            String userId = request.getPayload().getUser().getId();
            ViewState viewState = request.getPayload().getView().getState();
            Exercise exerciseRecord = new Exercise();
            exerciseRecord.setUserid(viewState.getValues().get("user_selection").get("users_select-action").getSelectedUser());
            exerciseRecord.setName(viewState.getValues().get("exercise_name_input").get("plain_text_input-action").getValue());
            exerciseRecord.setEquipment(viewState.getValues().get("equipment_needed_input").get("plain_text_input-action").getValue());
            exerciseRecord.setSets(viewState.getValues().get("sets_input").get("plain_text_input-action").getValue());
            exerciseRecord.setReps(viewState.getValues().get("reps_input").get("plain_text_input-action").getValue());
            exerciseRecord.setWeight(viewState.getValues().get("weight_input").get("plain_text_input-action").getValue());
            LOGGER.info("saving record to database: {}",exerciseRecord);
            repo.save(exerciseRecord);


            MarkdownTextObject htmlElement = MarkdownTextObject.builder()
                    .text(createTableMarkdown(exerciseRecord.getUserid()))
                    .build();
            ContextBlock tableLayout = ContextBlock.builder().blockId("exercise_table").build();
            tableLayout.getElements().add(htmlElement);

            View savedRecordView = View.builder()
                    .type("home")
                    .blocks(new ArrayList<>())
                    .build();
            savedRecordView.getBlocks().add(tableLayout);

            ViewsPublishRequest updateView = ViewsPublishRequest.builder()
                    .view(savedRecordView)
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .userId(userId)
                    .build();
            context.client().viewsPublish(updateView);
            return context.ack();
        });
    }

    private String createTableMarkdown(String userId) {
        List<Exercise> allRecords = repo.getAllExercisesForUser(userId);
        String oth = OPEN + "th" + CLOSE;
        String cth = OPEN + "/th" + CLOSE;
        String otd = OPEN + "td" + CLOSE;
        String ctd = OPEN + "/td" + CLOSE;
        StringBuilder markdownText = new StringBuilder(OPEN).append("table").append(CLOSE);
        markdownText.append(OPEN).append("tr").append(CLOSE);
        markdownText.append(oth).append("Exercise Name").append(cth);
        markdownText.append(oth).append("Equipment Needed").append(cth);
        markdownText.append(oth).append("Sets").append(cth);
        markdownText.append(oth).append("Reps").append(cth);
        markdownText.append(oth).append("Weight").append(cth);
        markdownText.append(OPEN).append("/tr").append(CLOSE);
        for(Exercise record : allRecords) {
            markdownText.append(OPEN).append("tr").append(CLOSE);
            markdownText.append(otd).append(record.getName()).append(ctd);
            markdownText.append(otd).append(record.getEquipment()).append(ctd);
            markdownText.append(otd).append(record.getSets()).append(ctd);
            markdownText.append(otd).append(record.getReps()).append(ctd);
            markdownText.append(otd).append(record.getWeight()).append(ctd);
            markdownText.append(OPEN).append("/tr").append(CLOSE);
        }
        markdownText.append(OPEN).append("/table").append(CLOSE);
        return markdownText.toString();
    }

}
