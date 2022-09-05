package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.UsersSelectElement;
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
import java.util.Map;
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


        initHomeView(liftbotApp);
        initAddRecordAction(liftbotApp);
        initViewRecordsAction(liftbotApp);
        initSaveAction(liftbotApp);

        return liftbotApp;
    }

    private void initHomeView(App liftBot) {
        LOGGER.info("initializing home screen view");
        liftBot.event(AppHomeOpenedEvent.class, (request, context) -> {
            String userId = request.getEvent().getUser();
            LOGGER.info("user that opened home view: {}", userId);
            try {
                View homeLanding = View.builder()
                        .type("home")
                        .blocks(createHomeLanding(userId))
                        .build();

                ViewsPublishRequest addView = ViewsPublishRequest.builder()
                        .view(homeLanding)
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

    private List<LayoutBlock> createHomeLanding(String userId) {
        List<LayoutBlock> blocks = new ArrayList<>();

        SectionBlock sectionBlock = SectionBlock.builder()
                .text(MarkdownTextObject.builder().text("choose user").build())
                .accessory(UsersSelectElement.builder().actionId("selected_user").build())
                .build();

        ActionsBlock actionsBlock = ActionsBlock.builder()
                .elements(new ArrayList<BlockElement>(){
                    {
                        add(ButtonElement.builder()
                                .actionId("view_records")
                                .text(PlainTextObject.builder().text("View Workout Records").build())
                                .build());
                        add(ButtonElement.builder()
                                .actionId("add_record")
                                .text(PlainTextObject.builder().text("Add New Record").build())
                                .build());
                    }
                })
                .build();

        blocks.add(new DividerBlock());
        blocks.add(sectionBlock);
        blocks.add(new DividerBlock());
        blocks.add(actionsBlock);

        return blocks;
    }

    private void initAddRecordAction(App liftbotApp) {
        LOGGER.info("initializing Add Record action");
        liftbotApp.blockAction("add_record", (request, context) -> {

            String userId = request.getPayload().getUser().getId();
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

    private void initViewRecordsAction(App liftbotApp) {
        LOGGER.info("initializing View Records action");
        liftbotApp.blockAction("view_records", (request, context) -> {
            BlockActionPayload.User user = request.getPayload().getUser();
            LOGGER.info("user using the app: {}", user.getUsername());

            ViewState viewState = request.getPayload().getView().getState();
            LOGGER.info("viewState.getValues(): {}",viewState.getValues());//.get("selected_user").getSelectedUser());
            Map<String, ViewState.Value> valueMap = viewState.getValues().values()
                    .stream()
                    .filter(value -> value.containsKey("selected_user")).findFirst().get();
            LOGGER.info("selected user id: {}",valueMap.get("selected_user").getSelectedUser());

            View viewRecordsView = View.builder()
                    .type("home")
                    .blocks(createAllRecordsView(valueMap.get("selected_user").getSelectedUser()))
                    .build();

            LOGGER.info(viewRecordsView.toString());

            ViewsPublishRequest recordView = ViewsPublishRequest.builder()
                    .view(viewRecordsView)
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .userId(user.getId())
                    .build();
            context.client().viewsPublish(recordView);
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
                    .blocks(createAllRecordsView(exerciseRecord.getUserid()))
                    .build();

            LOGGER.info(savedRecordView.toString());

            ViewsPublishRequest updateView = ViewsPublishRequest.builder()
                    .view(savedRecordView)
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .userId(userId)
                    .build();
            context.client().viewsPublish(updateView);
            return context.ack();
        });
    }

    private List<LayoutBlock> createAllRecordsView(String userId) {
        List<Exercise> allRecords = repo.getAllExercisesForUser(userId);
        List<LayoutBlock> blocks = new ArrayList<>();

        //build title
        HeaderBlock title = HeaderBlock.builder()
                .text(new PlainTextObject(userId,true)).build();
        blocks.add(title);

        for(Exercise record : allRecords) {
            blocks.add(new DividerBlock());
            blocks.add(SectionBlock.builder()
                    .text(MarkdownTextObject.builder().text("*"+record.getName()+"*").build()).build()
            );
            blocks.add(SectionBlock.builder()
                    .text(MarkdownTextObject.builder().text("("+record.getEquipment()+") : "
                            +record.getSets()+" x "+record.getReps()+" x "+record.getWeight()+"lb").build()).build()
            );
        }
        return blocks;
    }

}
