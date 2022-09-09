package com.barbenders.liftbot.views;

import com.barbenders.liftbot.model.Exercise;
import com.barbenders.liftbot.repo.ExerciseRepository;
import com.barbenders.liftbot.util.LiftbotUtil;
import com.slack.api.model.User;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.block.element.StaticSelectElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RecordsView {

    private final List<LayoutBlock> blocks;
    private final User user;
    private List<String> equipment;

    public RecordsView(User user) {
        this.user = user;
        this.blocks = new ArrayList<>();
    }

    public List<LayoutBlock> getAllRecordsView(ExerciseRepository repo) {

        log.info("getting [All Records] view for [{}]", user.getProfile().getDisplayName());
        List<Exercise> allRecords = repo.getAllExercisesForUser(user.getId());

        blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                        .text("*"+user.getProfile().getDisplayName()+"*").build())
                .build());

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
        blocks.add(ActionsBlock.builder().elements(new ArrayList<BlockElement>(){
            {
                add(ButtonElement.builder()
                        .actionId("nav_home")
                        .text(new PlainTextObject("Back", true)).build());
            }
        }).build());
        return blocks;
    }

    public List<LayoutBlock> getAddRecordView() {

        log.info("getting [Add Record] view for [{}]", user.getProfile().getDisplayName());
        blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder().text("*"+user.getProfile().getDisplayName()+"*").build())
                .build());
        blocks.add(InputBlock.builder().blockId("exercise_name_input")
                .label(new PlainTextObject("Exercise Name",true))
                .element(new PlainTextInputElement()).build());
        blocks.add(getDropDown("equipment_needed_input","equipment.yml","Equipment Needed"));
        blocks.add(getDropDown("sets_input","sets.yml","Sets"));
        blocks.add(getDropDown("reps_input","reps.yml","Reps"));
        blocks.add(getDropDown("weight_input","weight.yml","Reps"));
        blocks.add(ActionsBlock.builder().elements(new ArrayList<BlockElement>(){
            {
                add(ButtonElement.builder()
                        .text(new PlainTextObject("Save",true))
                        .actionId("exercise_save").build());
                add(ButtonElement.builder()
                        .actionId("nav_home")
                        .text(new PlainTextObject("Back", true)).build());
            }
        }).build());
        return blocks;
    }

    private InputBlock getDropDown(String inputId, String fileName, String label) {
        return InputBlock.builder()
                .element(StaticSelectElement.builder()
                        .actionId(inputId)
                        .options(LiftbotUtil.createDropDownOptions(fileName)).build())
                .label(new PlainTextObject(label, true)).build();
    }

}
