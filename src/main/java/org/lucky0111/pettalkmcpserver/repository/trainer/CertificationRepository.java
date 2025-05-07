package org.lucky0111.pettalkmcpserver.repository.trainer;

import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByTrainer_TrainerId(UUID trainerId);
}
