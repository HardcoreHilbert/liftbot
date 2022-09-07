package com.barbenders.liftbot.controller;

import com.slack.api.bolt.servlet.SlackAppServlet;
import com.slack.api.bolt.App;
import javax.servlet.annotation.WebServlet;

@WebServlet("/slack/events")
public class LiftbotController extends SlackAppServlet {

    public LiftbotController(App app) {
        super(app);
    }
}

