package com.barbenders.liftbot.views;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.slack.api.model.User;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RecordsView {

    @Autowired
    ExerciseRepository repo;

    private final List<LayoutBlock> blocks;
    private final User user;

    public RecordsView(User user) {
        this.user = user;
        this.blocks = new ArrayList<>();
    }

    public List<LayoutBlock> getAllRecordsView() {

        log.info("creating All Records view for {}", user.getName());
        List<Exercise> allRecords = repo.getAllExercisesForUser(user.getId());

        //build title
        HeaderBlock title = HeaderBlock.builder()
                .text(new PlainTextObject(user.getName(),true)).build();
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

    public List<LayoutBlock> getAddRecordView() {

        blocks.add(HeaderBlock.builder().blockId("selected_user_name")
                .text(new PlainTextObject(user.getName(),true)).build());
        blocks.add(InputBlock.builder().blockId("exercise_name_input")
                .label(new PlainTextObject("Exercise Name",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(InputBlock.builder().blockId("equipment_needed_input")
                .label(new PlainTextObject("Equipment Needed",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(InputBlock.builder().blockId("sets_input")
                .label(new PlainTextObject("Sets",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(InputBlock.builder().blockId("reps_input")
                .label(new PlainTextObject("Reps",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(InputBlock.builder().blockId("weight_input")
                .label(new PlainTextObject("Weight",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(ActionsBlock.builder().elements(new ArrayList<BlockElement>(){
            {
                add(ButtonElement.builder()
                        .text(new PlainTextObject("Save",true))
                        .actionId("exercise_save").build());
            }
        }).build());
        return blocks;
    }
}
