package com.barbenders.liftbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

    public Exercise () {}

    public Exercise (String id, String userid, String name, String equipment, String sets, String reps, String weight){
        this.id = id;
        this.userid = userid;
        this.name = name;
        this.equipment = equipment;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getSets() {
        return sets;
    }

    public void setSets(String sets) {
        this.sets = sets;
    }

    public String getReps() {
        return reps;
    }

    public void setReps(String reps) {
        this.reps = reps;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}
