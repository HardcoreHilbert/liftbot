package com.barbenders.liftbot.util;

import com.barbenders.liftbot.model.Exercise;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.model.User;
import com.slack.api.model.view.ViewState;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;

@UtilityClass
@Slf4j
public class LiftbotUtil {

    public User getUserWithId(Context context, String userId) {
        try {
            return context.client().usersInfo(UsersInfoRequest.builder()
                    .user(userId)
                    .token(context.getBotToken()).build()).getUser();
        } catch (SlackApiException | IOException ex) {
            log.error("Exception while retrieving user info for id: {}",userId, ex);
        }
        return null;
    }

    public User getSelectedUserFromRequest(Context context, BlockActionRequest request) throws NoSuchElementException {
        String userId = request.getPayload().getView().getState()
                .getValues().values().stream()
                .filter(value -> value.containsKey("selected_user")).findFirst().get()
                .get("selected_user").getSelectedUser();
        return getUserWithId(context,userId);
    }

    public Exercise getRecordFromPayload(BlockActionPayload payload) {
        Map<String, Map<String, ViewState.Value>> vsValues = payload.getView().getState().getValues();
        Exercise record = new Exercise();

        record.setUserid(payload.getView().getPrivateMetadata());
        record.setName(new ArrayList<>(vsValues.get("exercise_name_input").values()).get(0).getValue());
        record.setEquipment(new ArrayList<>(vsValues.get("equipment_needed_input").values()).get(0).getValue());
        record.setSets(new ArrayList<>(vsValues.get("sets_input").values()).get(0).getValue());
        record.setReps(new ArrayList<>(vsValues.get("reps_input").values()).get(0).getValue());
        record.setWeight(new ArrayList<>(vsValues.get("weight_input").values()).get(0).getValue());

        return record;
    }
}
