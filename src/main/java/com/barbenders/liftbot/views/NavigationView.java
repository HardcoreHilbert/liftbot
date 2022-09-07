package com.barbenders.liftbot.views;

import com.slack.api.model.User;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.UsersSelectElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NavigationView {

    private List<LayoutBlock> blocks;
    private User user;

    public NavigationView(User user) {
        this.user = user;
        this.blocks = new ArrayList<>();
    }

    public List<LayoutBlock> getHomeLanding() {
        log.info("creating home landing layout");

        if(user.isAdmin()) {
            blocks.add(SectionBlock.builder()
                            .text(MarkdownTextObject.builder().text("Whose fate are we controlling today?").build())
                            .accessory(UsersSelectElement.builder().actionId("selected_user").build())
                            .build());
        } else {
            blocks.add(getActionChoiceLayout());
        }
        return blocks;
    }

    private ActionsBlock getActionChoiceLayout() {
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
        blocks.add(HeaderBlock.builder().blockId("selected_user_name")
                        .text(new PlainTextObject(user.getName(),true)).build());
        blocks.add(new DividerBlock());
        blocks.add(getActionChoiceLayout());
        return blocks;
    }
}
