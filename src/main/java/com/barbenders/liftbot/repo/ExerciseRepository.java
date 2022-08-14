package com.barbenders.liftbot.repo;

import com.barbenders.liftbot.model.Exercise;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends MongoRepository<Exercise, String> {

    @Query("{userid: ?0,name: ?1}")
    Optional<Exercise> getExerciseForUser(String userid, String name);

    @Query("{userid: ?0}")
    List<Exercise> getAllExercisesForUser(String userid);

    @Query("{name: ?0}")
    List<Exercise> getAllExercisesByName(String name);

    @Query("{equipment: ?0}")
    List<Exercise> getAllExercisesByEquipment(String equipment);
}
