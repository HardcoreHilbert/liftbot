package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.barbenders.liftbot.util.LiftbotUtil;
import com.barbenders.liftbot.views.NavigationView;
import com.barbenders.liftbot.views.RecordsView;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.User;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LiftbotApp {

    @Autowired
    ExerciseRepository repo;

    @Bean
    public AppConfig loadSingleWorkspaceAppConfig() {
        log.info("loading single workspace app configuration");
        return AppConfig.builder()
                .singleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"))
                .signingSecret(System.getenv("SLACK_SIGNING_SECRET"))
                .build();
    }

    @Bean
    public App initLiftbotApp(AppConfig config){
        log.info("initializing LiftBot application");
        App liftbotApp = new App(config);

        initHomeView(liftbotApp);
        initUserSelectionAction(liftbotApp);
        initAddRecordAction(liftbotApp);
        initViewRecordsAction(liftbotApp);
        initSaveRecordAction(liftbotApp);
        initHomeNavAction(liftbotApp);

        return liftbotApp;
    }

    private void initHomeView(App liftBot) {
        log.info("initializing home screen view");
        liftBot.event(AppHomeOpenedEvent.class, (request, context) -> {
            User currentUser = LiftbotUtil.getUserWithId(context, request.getEvent().getUser());
            log.info("user that opened home view: {}", currentUser.getName());

            context.client().viewsPublish(new NavigationView(currentUser).getHomeLandingView(context.getBotToken()));

            return context.ack();
        });
    }

    private void initHomeNavAction(App liftBot) {
        log.info("initializing Home Nav action");
        liftBot.blockAction("nav_home", (request, context) -> {
            User currentUser = LiftbotUtil.getUserWithId(context,request.getPayload().getUser().getId());
            log.info("user that opened home view: {}", currentUser.getName());

            context.client().viewsPublish(new NavigationView(currentUser).getHomeLandingView(context.getBotToken()));

            return context.ack();
        });
    }

    private void initUserSelectionAction(App liftBot) {
        log.info("initializing User Selection action");
        liftBot.blockAction("selected_user", (request, context) -> {

            User selectedUser = LiftbotUtil.getSelectedUserFromRequest(context,request);
            log.info("user: [{}] selected user: [{}]", request.getPayload().getUser().getName(), selectedUser.getName());

            View actionChoiceView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new NavigationView(selectedUser).getAdminChoiceActionView())
                    .build();
            ViewsPublishRequest addView = ViewsPublishRequest.builder()
                    .view(actionChoiceView)
                    .token(context.getBotToken())
                    .userId(request.getPayload().getUser().getId())
                    .build();
            context.client().viewsPublish(addView);

            return context.ack();
        });
    }

    private void initAddRecordAction(App liftbotApp) {
        log.info("initializing Add Record action");
        liftbotApp.blockAction("add_record", (request, context) -> {

            User selectedUser = LiftbotUtil.getUserWithId(context, request.getPayload().getView().getPrivateMetadata());
            log.info("the [add_record] action is being performed on user: [{}]", selectedUser.getName());

            View addRecordView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAddRecordView())
                    .build();
            ViewsPublishRequest addView = ViewsPublishRequest.builder()
                    .view(addRecordView)
                    .token(context.getBotToken())
                    .userId(request.getPayload().getUser().getId())
                    .build();
            context.client().viewsPublish(addView);

            return context.ack();
        });
    }

    private void initViewRecordsAction(App liftbotApp) {
        log.info("initializing View Records action");
        liftbotApp.blockAction("view_records", (request, context) -> {

            User selectedUser = LiftbotUtil.getUserWithId(context, request.getPayload().getView().getPrivateMetadata());
            log.info("the [view_record] action is being performed on user: [{}]", selectedUser);

            View viewRecordsView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAllRecordsView(repo))
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

    private void initSaveRecordAction(App liftbotApp) {
        log.info("initializing Save Record action");
        liftbotApp.blockAction("exercise_save", (request,context) -> {

            Exercise record = LiftbotUtil.getRecordFromPayload(request.getPayload());
            try {
                Double.parseDouble(record.getWeight());
            } catch (NumberFormatException e) {
                String error = "{response_action:{weight_input:weight must be a number}}";
                //return Response.ok(context.respond(context.getResponseUrl(),error));
            }
            repo.save(record);
            log.debug("saving record to db: {}",record);

            User selectedUser = LiftbotUtil.getUserWithId(context,record.getUserid());
            log.info("the [exercise_save] action is being performed on user: [{}]", selectedUser.getName());

            View savedRecordView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAllRecordsView(repo))
                    .build();
            ViewsPublishRequest updateView = ViewsPublishRequest.builder()
                    .view(savedRecordView)
                    .token(context.getBotToken())
                    .userId(request.getPayload().getUser().getId())
                    .build();
            context.client().viewsPublish(updateView);

            return context.ack();
        });
    }
}
