package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.User;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
        initUserSelectionAction(liftbotApp);
        initAddRecordAction(liftbotApp);
        initViewRecordsAction(liftbotApp);
        initSaveAction(liftbotApp);

        return liftbotApp;
    }

    private void initHomeView(App liftBot) {
        LOGGER.info("initializing home screen view");
        liftBot.event(AppHomeOpenedEvent.class, (request, context) -> {
            User currentUser = getUserWithId(context, request.getEvent().getUser());
            LOGGER.info("user that opened home view: {}", currentUser.getName());

            View homeLanding = View.builder()
                    .type("home")
                    .blocks(createHomeLanding(currentUser.isAdmin()))
                    .build();
            ViewsPublishRequest homeView = ViewsPublishRequest.builder()
                    .view(homeLanding)
                    .token(context.getBotToken())
                    .userId(currentUser.getId())
                    .build();
            context.client().viewsPublish(homeView);

            return context.ack();
        });
    }

    private List<LayoutBlock> createHomeLanding(boolean isAdmin) {
        LOGGER.info("creating home landing layout");

        if(isAdmin) {
            return new ArrayList<LayoutBlock>() {
                {
                    add(SectionBlock.builder()
                            .text(MarkdownTextObject.builder().text("Whose fate are we controlling today?").build())
                            .accessory(UsersSelectElement.builder().actionId("selected_user").build())
                            .build());
                }
            };
        } else {
            return new ArrayList<LayoutBlock>() {
                {
                    add(createActionChoiceLayout());
                }
            };
        }
    }

    private ActionsBlock createActionChoiceLayout() {
        return ActionsBlock.builder()
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
    }

    private void initUserSelectionAction(App liftBot) {
        LOGGER.info("initializing User Selection action");
        liftBot.blockAction("selected_user", (request, context) -> {

            BlockActionPayload.User currentUser = request.getPayload().getUser();
            LOGGER.info("current user: {}", currentUser.getName());

            User selectedUser = getUserWithId(context,getSelectedUserIdFromRequest(request));

            View actionChoiceView = View.builder()
                    .type("home")
                    .blocks(createAdminChoiceActionView(selectedUser))
                    .build();

            ViewsPublishRequest addView = ViewsPublishRequest.builder()
                    .view(actionChoiceView)
                    .token(context.getBotToken())
                    .userId(currentUser.getId())
                    .build();
            context.client().viewsPublish(addView);

            return context.ack();
        });
    }

    private ArrayList<LayoutBlock> createAdminChoiceActionView(User selectedUser) {
        return new ArrayList<LayoutBlock>(){
            {
                add(HeaderBlock.builder().blockId("selected_user_name")
                        .text(new PlainTextObject(selectedUser.getName(),true)).build());
                add(SectionBlock.builder().blockId("selected_user_id")
                        .text(new PlainTextObject(selectedUser.getId(), false)).build());
                add(new DividerBlock());
                add(createActionChoiceLayout());
            }
        };
    }

    private void initAddRecordAction(App liftbotApp) {
        LOGGER.info("initializing Add Record action");
        liftbotApp.blockAction("add_record", (request, context) -> {

            String userId = request.getPayload().getUser().getId();
            LOGGER.info("user: {}", userId);

            User selectedUser = getUserWithId(context,getSelectedUserIdFromRequest(request));

            View addRecordView = View.builder()
                    .type("home")
                    .blocks(createAdminChoiceActionView(selectedUser))
                    .build();
            ViewsPublishRequest addView = ViewsPublishRequest.builder()
                    .view(addRecordView)
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .userId(userId)
                    .build();
            context.client().viewsPublish(addView);

            return context.ack();
        });
    }

    private User getUserWithId(Context context, String userId) {
        try {
            return context.client().usersInfo(UsersInfoRequest.builder()
                    .user(userId)
                    .token(context.getBotToken()).build()).getUser();
        } catch (SlackApiException | IOException ex) {
            LOGGER.error("Exception while retrieving user info for id: {}",userId, ex);
        }
        return null;
    }

    private String getSelectedUserIdFromRequest(BlockActionRequest request) throws NoSuchElementException {
        return request.getPayload().getView().getState()
                .getValues().values().stream()
                .filter(value -> value.containsKey("selected_user")).findFirst().get()
                .get("selected_user").getSelectedUser();
    }

    private User getSelectedUser(Context context, BlockActionRequest request) {
        try {
            return getUserWithId(context, getSelectedUserIdFromRequest(request));
        } catch (NoSuchElementException nse) {
            return getUserWithId(context, request.getPayload().getUser().getId());
        }
    }

    private void initViewRecordsAction(App liftbotApp) {
        LOGGER.info("initializing View Records action");
        liftbotApp.blockAction("view_records", (request, context) -> {

            User selectedUser = getSelectedUser(context, request);
            LOGGER.debug("selected user: {}",selectedUser.getRealName());

            View viewRecordsView = View.builder()
                    .type("home")
                    .blocks(createAddRecordView(selectedUser))
                    .build();

            ViewsPublishRequest recordView = ViewsPublishRequest.builder()
                    .view(viewRecordsView)
                    .token(context.getBotToken())
                    .userId(request.getPayload().getUser().getId())
                    .build();
            context.client().viewsPublish(recordView);
            return context.ack();
        });
    }

    private List<LayoutBlock> createAddRecordView(User user) {

        List<LayoutBlock> blocks = new ArrayList<>();

        //build title
        blocks.add(HeaderBlock.builder().blockId("selected_user_name")
                .text(new PlainTextObject(user.getName(),true)).build());
        blocks.add(SectionBlock.builder().blockId("selected_user_id")
                .text(new PlainTextObject(user.getId(), false)).build());
        blocks.add(InputBlock.builder().blockId("exercise_name_input")
                .label(new PlainTextObject("Exercise Name",true)).build());
        blocks.add(InputBlock.builder().blockId("equipment_needed_input")
                .label(new PlainTextObject("Equipment Needed",true)).build());
        blocks.add(InputBlock.builder().blockId("sets_input")
                .label(new PlainTextObject("Sets",true)).build());
        blocks.add(InputBlock.builder().blockId("reps_input")
                .label(new PlainTextObject("Reps",true)).build());
        blocks.add(InputBlock.builder().blockId("weight_input")
                .label(new PlainTextObject("Weight",true)).build());
        blocks.add(ActionsBlock.builder().elements(new ArrayList<BlockElement>(){
            {
                add(ButtonElement.builder()
                        .text(new PlainTextObject("Save",true))
                        .actionId("exercise_save").build());
            }
        }).build());
        return blocks;
    }

    private Exercise getRecordFromPayload(BlockActionPayload payload) {
        Map<String,Map<String, ViewState.Value>> vsValues = payload.getView().getState().getValues();
        Exercise record = new Exercise();
        record.setUserid(vsValues.get("user_selection").get("users_select-action").getSelectedUser());
        record.setName(vsValues.get("exercise_name_input").get("plain_text_input-action").getValue());
        record.setEquipment(vsValues.get("equipment_needed_input").get("plain_text_input-action").getValue());
        record.setSets(vsValues.get("sets_input").get("plain_text_input-action").getValue());
        record.setReps(vsValues.get("reps_input").get("plain_text_input-action").getValue());
        record.setWeight(vsValues.get("weight_input").get("plain_text_input-action").getValue());
        return record;
    }

    private void initSaveAction(App liftbotApp) {
        liftbotApp.blockAction("exercise_save", (request,context) -> {
            String userId = request.getPayload().getUser().getId();

            Exercise record = getRecordFromPayload(request.getPayload());
            LOGGER.debug("saving record to db: {}",record);
            repo.save(record);

            User selectedUser = getUserWithId(context,record.getUserid());

            View savedRecordView = View.builder()
                    .type("home")
                    .blocks(createAllRecordsView(selectedUser))
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

    private List<LayoutBlock> createAllRecordsView(User user) {

        List<Exercise> allRecords = repo.getAllExercisesForUser(user.getId());
        List<LayoutBlock> blocks = new ArrayList<>();

        //build title
        HeaderBlock title = HeaderBlock.builder()
                .text(new PlainTextObject(user.getRealName(),true)).build();
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
