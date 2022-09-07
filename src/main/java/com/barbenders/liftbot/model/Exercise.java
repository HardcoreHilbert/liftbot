package com.barbenders.liftbot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "exercise")
public class Exercise {

    @Id
    private String id;
    private String userid;
    private String name;
    private String equipment;
    private String sets;
    private String reps;
    private String weight;

}
