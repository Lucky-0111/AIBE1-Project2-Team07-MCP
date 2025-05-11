package org.lucky0111.pettalkmcpserver.service.trainer;


import org.lucky0111.pettalkmcpserver.domain.dto.trainer.CertificationRequestDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface TrainerService {

    TrainerDTO getTrainerDetails(String trainerNickname);

}
