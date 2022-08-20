package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.composition.TextObject;
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

            View savedRecordView = View.builder()
                    .type("home")
                    .blocks(createTableBlock(exerciseRecord.getUserid()))
                    .build();

            ViewsPublishRequest updateView = ViewsPublishRequest.builder()
                    .view(savedRecordView)
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .userId(userId)
                    .build();
            context.client().viewsPublish(updateView);
            return context.ack();
        });
    }

    private List<LayoutBlock> createTableBlock(String userId) {
        List<Exercise> allRecords = repo.getAllExercisesForUser(userId);
        List<LayoutBlock> blocks = new ArrayList<>();

        //build title
        HeaderBlock title = HeaderBlock.builder()
                .text(new PlainTextObject(userId,true)).build();
        blocks.add(0,title);

        List<TextObject> fields = new ArrayList<>();
        fields.add(MarkdownTextObject.builder().text("*Exercise Name*").build());
        fields.add(MarkdownTextObject.builder().text("*Equipment Needed*").build());
        fields.add(MarkdownTextObject.builder().text("*Sets*").build());
        fields.add(MarkdownTextObject.builder().text("*Reps*").build());
        fields.add(MarkdownTextObject.builder().text("*Weight*").build());
        SectionBlock header = SectionBlock.builder().fields(fields).build();
        blocks.add(1,header);

        int count = 2;
        for(Exercise record : allRecords) {
            List<TextObject> recordLine = new ArrayList<>();
            recordLine.add(MarkdownTextObject.builder().text(record.getName()).build());
            recordLine.add(MarkdownTextObject.builder().text(record.getEquipment()).build());
            recordLine.add(MarkdownTextObject.builder().text(record.getSets()).build());
            recordLine.add(MarkdownTextObject.builder().text(record.getReps()).build());
            recordLine.add(MarkdownTextObject.builder().text(record.getWeight()).build());
            blocks.add(count,SectionBlock.builder().fields(recordLine).build());
            count++;
        }
        return blocks;
    }

}
