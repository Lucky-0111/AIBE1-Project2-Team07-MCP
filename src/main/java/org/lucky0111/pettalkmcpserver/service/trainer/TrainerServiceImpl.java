package org.lucky0111.pettalkmcpserver.service.trainer;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.lucky0111.pettalkmcpserver.domain.dto.review.ReviewStatsDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.CertificationDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerPhotoDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerServiceFeeDTO;
import org.lucky0111.pettalkmcpserver.domain.entity.common.Tag;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.*;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.exception.CustomException;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.lucky0111.pettalkmcpserver.repository.review.ReviewRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.*;
import org.lucky0111.pettalkmcpserver.repository.user.PetUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional

public class TrainerServiceImpl implements TrainerService {

    private final TrainerRepository trainerRepository;
    private final PetUserRepository petUserRepository;
    private final CertificationRepository certificationRepository;
    private final TrainerTagRepository trainerTagRepository;
    private final TagRepository tagRepository;
    private final ReviewRepository reviewRepository;
//    private final FileUploaderService fileUploaderService;
    private final TrainerPhotoRepository trainerPhotoRepository;
    private final TrainerServiceFeeRepository trainerServiceFeeRepository;


    @Override
    @Transactional(readOnly = true)
    public TrainerDTO getTrainerDetails(String trainerNickname) {
        // 1. Trainer 엔티티 조회 (ID는 UUID)
        Trainer trainer = trainerRepository.findByUser_Nickname(trainerNickname)
                .orElseThrow(() -> new CustomException("훈련사 정보를 찾을 수 없습니다 ID: %s".formatted(trainerNickname), HttpStatus.NOT_FOUND));

        PetUser user = trainer.getUser();

        Set<TrainerPhoto> photos = trainer.getPhotos();
        Set<TrainerServiceFee> serviceFees = trainer.getServiceFees();

        List<String> specializationNames = getSpecializationNames(trainer.getTrainerId());
        List<CertificationDTO> certificationDtoList = getCertificationDTOList(trainer.getTrainerId());

        List<TrainerPhotoDTO> photoDTOs = getPhotosDTO(photos);
        List<TrainerServiceFeeDTO> serviceFeeDTOs = getServiceFeesDTO(serviceFees);


        ReviewStatsDTO reviewStatsDTO = getReviewStatsDTO(trainer.getTrainerId());


        return new TrainerDTO(
                trainer.getTrainerId(), // UUID 타입
                user != null ? user.getName() : null,
                user != null ? user.getNickname() : null,
                user != null ? user.getProfileImageUrl() : null,
                user != null ? user.getEmail() : null, // email 필드 추가 (PetUser에 있다고 가정)

                trainer.getTitle(),
                trainer.getIntroduction(),
                trainer.getRepresentativeCareer(),
                trainer.getSpecializationText(),
                trainer.getVisitingAreas(),
                trainer.getExperienceYears() != null ? trainer.getExperienceYears() : 0,

                photoDTOs,
                serviceFeeDTOs,

                specializationNames, // 태그 이름 목록 (리스트 형태)
                certificationDtoList, // 자격증 DTO 목록
                reviewStatsDTO.averageRating(),
                reviewStatsDTO.reviewCount()

        );
    }

    private Map<UUID, List<CertificationDTO>> getCertificationMapForTrainers(List<UUID> trainerIds) {
        // 트레이너 ID 목록으로 자격증 조회
        return certificationRepository.findAllByTrainer_TrainerIdIn(trainerIds).stream()
                .collect(Collectors.groupingBy(
                        certification -> certification.getTrainer().getTrainerId(),
                        Collectors.mapping(CertificationDTO::fromEntity, Collectors.toList())
                ));
    }

    private Map<UUID, List<String>> getSpecializationMapForTrainers(List<UUID> trainerIds) {
        return trainerTagRepository.findAllByTrainer_TrainerIdIn(trainerIds).stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getTrainer().getTrainerId(),
                        Collectors.mapping(
                                relation -> relation.getTag().getTagName(),
                                Collectors.toList()
                        )
                ));
    }

//    // 여러 트레이너의 리뷰 통계를 한 번에 조회
//    private Map<UUID, ReviewStatsDTO> getReviewStatsMapForTrainers(List<UUID> trainerIds) {
//        // 트레이너 ID 목록으로 평균 평점 조회
//        Map<UUID, Double> avgRatings = reviewRepository.findAverageRatingsByTrainerIds(trainerIds);
//
//        // 트레이너 ID 목록으로 리뷰 개수 조회
//        Map<UUID, Long> reviewCounts = reviewRepository.countReviewsByTrainerIds(trainerIds);
//
//        // 결과 맵 생성
//        Map<UUID, ReviewStatsDTO> result = new HashMap<>();
//        for (UUID trainerId : trainerIds) {
//            Double avgRating = avgRatings.getOrDefault(trainerId, 0.0);
//            Long reviewCount = reviewCounts.getOrDefault(trainerId, 0L);
//            result.put(trainerId, new ReviewStatsDTO(avgRating, reviewCount));
//        }
//
//        return result;
//    }

//    // TrainerDTO 변환 메서드 분리
//    private TrainerDTO convertToTrainerDTO(
//            Trainer trainer,
//            List<TrainerPhotoDTO> photoDTOs,
//            List<TrainerServiceFeeDTO> serviceFeeDTOs,
//            List<String> specializationNames,
//            List<CertificationDTO> certificationDtoList,
//            ReviewStatsDTO reviewStatsDTO) {
//
//        PetUser user = trainer.getUser();
//
//        return new TrainerDTO(
//                trainer.getTrainerId(),
//                user != null ? user.getName() : null,
//                user != null ? user.getNickname() : null,
//                user != null ? user.getProfileImageUrl() : null,
//                user != null ? user.getEmail() : null,
//                trainer.getTitle(),
//                trainer.getIntroduction(),
//                trainer.getRepresentativeCareer(),
//                trainer.getSpecializationText(),
//                trainer.getVisitingAreas(),
//                trainer.getExperienceYears() != null ? trainer.getExperienceYears() : 0,
//                photoDTOs,
//                serviceFeeDTOs,
//                specializationNames,
//                certificationDtoList,
//                reviewStatsDTO.averageRating(),
//                reviewStatsDTO.reviewCount()
//        );
//    }
    private List<CertificationDTO> getCertificationDTOList(UUID trainerId){
        List<Certification> certifications = certificationRepository.findByTrainer_TrainerId(trainerId);

        return certifications.stream()
                .map(CertificationDTO::fromEntity)
                .toList();
    }

    // 4. 전문 분야(태그) 목록 조회 (Trainer ID는 UUID)
    private List<String> getSpecializationNames(UUID trainerId){
        List<TrainerTagRelation> trainerTags = trainerTagRepository.findByTrainer_TrainerId(trainerId);
        return trainerTags.stream()
                .map(TrainerTagRelation::getTag) // TrainerTag 엔티티에서 Tag 엔티티를 가져옴 (관계 매핑 필요)
                .map(Tag::getTagName) // Tag 엔티티에서 태그 이름을 가져옴
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

    private List<TrainerPhotoDTO> getPhotosDTO(Set<TrainerPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }

        return photos.stream()
                .map(photo -> new TrainerPhotoDTO(
                        photo.getFileUrl(),
                        photo.getPhotoOrder()
                ))
                .collect(Collectors.toList());
    }

    private List<TrainerServiceFeeDTO> getServiceFeesDTO(Set<TrainerServiceFee> serviceFees) {
        if (serviceFees == null || serviceFees.isEmpty()) {
            return Collections.emptyList();
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
