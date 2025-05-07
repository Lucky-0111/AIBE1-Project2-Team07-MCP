package org.lucky0111.pettalkmcpserver.repository.trainer;

import org.lucky0111.pettalkmcpserver.domain.entity.trainer.TrainerTagRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TrainerTagRepository extends JpaRepository<TrainerTagRelation, Long> {
    List<TrainerTagRelation> findByTrainer_TrainerId(UUID trainerId);

    @Query("SELECT tt.trainer.trainerId " +
            "FROM TrainerTagRelation tt " +
            "WHERE tt.tag.tagName IN :tags")
    List<UUID> findTrainerIdsByTagNames(List<String> tags);
}
