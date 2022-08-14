package com.barbenders.liftbot.repo;

import com.barbenders.liftbot.model.Exercise;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExerciseRepository extends MongoRepository<Exercise, String> {
    public Exercise getExerciseForUser(String userid, String name);
    public Exercise getAllExercisesForUser(String userid);
    public Exercise getAllExercises();
    public Exercise getAllExercisesByName(String name);
    public Exercise getAllExercisesByEquipment(String equipment);
}
