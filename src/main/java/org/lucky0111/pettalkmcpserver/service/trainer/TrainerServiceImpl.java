package org.lucky0111.pettalkmcpserver.service.trainer;

import lombok.RequiredArgsConstructor;
import org.lucky0111.pettalkmcpserver.domain.dto.review.ReviewStatsDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.*;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.*;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.exception.CustomException;
import org.lucky0111.pettalkmcpserver.repository.review.ReviewRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.CertificationRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerTagRepository;
import org.lucky0111.pettalkmcpserver.repository.user.PetUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerServiceImpl implements TrainerService {

    private final TrainerRepository trainerRepository;
    private final PetUserRepository petUserRepository;
    private final CertificationRepository certificationRepository;
    private final TrainerTagRepository trainerTagRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public TrainerDTO getTrainerDetails(String trainerNickname) {
        // 1. Trainer 엔티티 조회 (ID는 UUID)
        Trainer trainer = trainerRepository.findByUser_Nickname(trainerNickname)
                .orElseThrow(() -> new CustomException("훈련사 정보를 찾을 수 없습니다 ID: %s".formatted(trainerNickname), HttpStatus.NOT_FOUND));

        return buildTrainerDTO(trainer);
    }

    /**
     * Trainer 엔티티를 TrainerDTO로 변환합니다.
     */
    private TrainerDTO buildTrainerDTO(Trainer trainer) {
        PetUser user = trainer.getUser();

        List<TrainerPhoto> photos = trainer.getPhotos();
        List<TrainerServiceFee> serviceFees = trainer.getServiceFees();

        List<String> specializationNames = getSpecializationNames(trainer.getTrainerId());
        List<CertificationDTO> certificationDtoList = getCertificationDTOList(trainer.getTrainerId());

        List<TrainerPhotoDTO> photoDTOs = getPhotosDTO(photos);
        List<TrainerServiceFeeDTO> serviceFeeDTOs = getServiceFeesDTO(serviceFees);

        ReviewStatsDTO reviewStatsDTO = getReviewStatsDTO(trainer.getTrainerId());

        return new TrainerDTO(
                trainer.getTrainerId(), // UUID 타입
                user != null ? user.getNickname() : null,
                user != null ? user.getProfileImageUrl() : null,
                user != null ? user.getEmail() : null, // email 필드 추가 (PetUser에 있다고 가정)

                trainer.getTitle(),
                trainer.getIntroduction(),
                trainer.getRepresentativeCareer(),
                trainer.getSpecializationText(),
                trainer.getVisitingAreas(),
                trainer.getExperienceYears(),

                photoDTOs,
                serviceFeeDTOs,

                specializationNames, // 태그 이름 목록 (리스트 형태)
                certificationDtoList, // 자격증 DTO 목록
                reviewStatsDTO.averageRating(),
                reviewStatsDTO.reviewCount()
        );
    }

    private List<CertificationDTO> getCertificationDTOList(UUID trainerId){
        List<Certification> certifications = certificationRepository.findByTrainer_TrainerId(trainerId);

        return certifications.stream()
                .map(CertificationDTO::fromEntity) // Certification 엔티티 -> CertificationDto Record 변환 (CertificationDto에 fromEntity 메소드 구현 필요)
                .toList();
    }

    // 4. 전문 분야(태그) 목록 조회 (Trainer ID는 UUID)
    private List<String> getSpecializationNames(UUID trainerId){
        List<TrainerTagRelation> trainerTags = trainerTagRepository.findByTrainer_TrainerId(trainerId);
        return trainerTags.stream()
                .map(TrainerTagRelation::getTag) // TrainerTag 엔티티에서 Tag 엔티티를 가져옴 (관계 매핑 필요)
                .map(tag -> tag.getTagName()) // Tag 엔티티에서 태그 이름을 가져옴
                .toList();
    }
    // 5. 평점 및 후기 개수 조회 (ReviewRepository 사용, 인자 타입 UUID)
    private ReviewStatsDTO getReviewStatsDTO(UUID trainerId){
        Double averageRating = reviewRepository.findAverageRatingByTrainerId(trainerId);
        Long reviewCount = reviewRepository.countByReviewedTrainerId(trainerId);

        return new ReviewStatsDTO(
                averageRating != null ? averageRating : 0.0, // 평균 평점 (NULL일 경우 0.0)
                reviewCount != null ? reviewCount : 0L  // 후기 개수 (NULL일 경우 0));
        );
    }

    private List<TrainerPhotoDTO> getPhotosDTO(List<TrainerPhoto> photos){
        if(photos == null || photos.isEmpty()){
            return null;
        }
        return photos.stream()
                .map(photo -> new TrainerPhotoDTO(
                        photo.getFileUrl(),
                        photo.getPhotoOrder()
                ))
                .collect(Collectors.toList());
    }

    private List<TrainerServiceFeeDTO> getServiceFeesDTO(List<TrainerServiceFee> serviceFees){
        if(serviceFees == null || serviceFees.isEmpty()){
            return null;
        }
        return serviceFees.stream()
                .map(fee -> new TrainerServiceFeeDTO(
                        fee.getServiceType().name(),
                        fee.getDurationMinutes(),
                        fee.getFeeAmount()
                ))
                .collect(Collectors.toList());
    }
}