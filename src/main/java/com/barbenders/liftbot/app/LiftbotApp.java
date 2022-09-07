package com.barbenders.liftbot.app;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.barbenders.liftbot.util.LiftbotUtil;
import com.barbenders.liftbot.views.NavigationView;
import com.barbenders.liftbot.views.RecordsView;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.User;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            User currentUser = LiftbotUtil.getUserWithId(context, request.getEvent().getUser());
            LOGGER.info("user that opened home view: {}", currentUser.getName());

            View homeLanding = View.builder()
                    .type("home")
                    .blocks(new NavigationView(currentUser).getHomeLanding())// || currentUser.getId().equals("U03MT066GJU")))
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

    private void initUserSelectionAction(App liftBot) {
        LOGGER.info("initializing User Selection action");
        liftBot.blockAction("selected_user", (request, context) -> {

            BlockActionPayload.User currentUser = request.getPayload().getUser();
            LOGGER.info("current user: {}", currentUser.getName());

            User selectedUser = LiftbotUtil.getSelectedUserFromRequest(context,request);

            View actionChoiceView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new NavigationView(selectedUser).getAdminChoiceActionView())
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

    private void initAddRecordAction(App liftbotApp) {
        LOGGER.info("initializing Add Record action");
        liftbotApp.blockAction("add_record", (request, context) -> {

            String userId = request.getPayload().getUser().getId();
            LOGGER.debug("current user: {}", userId);

            User selectedUser = LiftbotUtil.getUserWithId(context,request.getPayload().getView().getPrivateMetadata());
            LOGGER.debug("selected user: {}", selectedUser.getName());

            View addRecordView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAddRecordView())
                    .build();
            ViewsPublishRequest addView = ViewsPublishRequest.builder()
                    .view(addRecordView)
                    .token(context.getBotToken())
                    .userId(userId)
                    .build();
            context.client().viewsPublish(addView);

            return context.ack();
        });
    }

    private void initViewRecordsAction(App liftbotApp) {
        LOGGER.info("initializing View Records action");
        liftbotApp.blockAction("view_records", (request, context) -> {

            User selectedUser = LiftbotUtil.getUserWithId(context, request.getPayload().getView().getPrivateMetadata());
            LOGGER.debug("selected user: {}",selectedUser.getRealName());

            View viewRecordsView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAllRecordsView())
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

    private void initSaveAction(App liftbotApp) {
        liftbotApp.blockAction("exercise_save", (request,context) -> {
            LOGGER.info("Save Action invoked by {}", request.getPayload().getUser().getName());
            Exercise record = LiftbotUtil.getRecordFromPayload(request.getPayload());
            LOGGER.debug("saving record to db: {}",record);
            repo.save(record);

            User selectedUser = LiftbotUtil.getUserWithId(context,record.getUserid());

            View savedRecordView = View.builder()
                    .type("home")
                    .privateMetadata(selectedUser.getId())
                    .blocks(new RecordsView(selectedUser).getAllRecordsView())
                    .build();

            LOGGER.info(savedRecordView.toString());

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
