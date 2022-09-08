package com.barbenders.liftbot.views;

import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.User;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.UsersSelectElement;
import com.slack.api.model.view.View;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NavigationView {

    private final List<LayoutBlock> blocks;
    private final User user;

    public NavigationView(User user) {
        this.user = user;
        this.blocks = new ArrayList<>();
    }

    public ViewsPublishRequest getHomeLandingView(String token) {
        View homeLanding = View.builder()
                .type("home")
                .privateMetadata(user.getId())
                .blocks(getHomeLanding())
                .build();
        return ViewsPublishRequest.builder()
                .view(homeLanding)
                .token(token)
                .userId(user.getId())
                .build();
    }

    public List<LayoutBlock> getHomeLanding() {
        log.info("getting [Home Landing] view for user [{}]",user.getProfile().getDisplayName());

        if(user.isAdmin()) {
            log.debug("user is admin, returning admin landing");
            blocks.add(SectionBlock.builder()
                            .text(MarkdownTextObject.builder().text("Whose fate are we controlling today?").build())
                            .accessory(UsersSelectElement.builder().actionId("selected_user").build())
                            .build());
        } else {
            log.debug("user is not admin, returning normal user landing");
            blocks.add(getActionChoiceLayout());
        }
        return blocks;
    }

    private ActionsBlock getActionChoiceLayout() {
        log.info("getting [Action Choice] view for user [{}]",user.getProfile().getDisplayName());
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

    public List<LayoutBlock> getAdminChoiceActionView() {
        log.info("getting [Admin Action Choice] view for admin [{}]",user.getProfile().getDisplayName());
        blocks.add(HeaderBlock.builder().blockId("selected_user_name")
                        .text(new PlainTextObject(user.getProfile().getDisplayName(),true)).build());
        blocks.add(new DividerBlock());
        blocks.add(getActionChoiceLayout());
        return blocks;
    }
}
