package org.lucky0111.pettalkmcpserver.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lucky0111.pettalkmcpserver.domain.common.BaseTimeEntity;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.CertificationDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerPhotoDTO;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerServiceFeeDTO;
import org.lucky0111.pettalkmcpserver.domain.entity.common.Tag;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Trainer;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerTagRepository;
import org.lucky0111.pettalkmcpserver.service.trainer.TrainerService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final TagRepository tagRepository;
    private final TrainerRepository trainerRepository;

    private final String trainerCardTemplate = """
            ---
            
            ## 🐾 {{트레이너_이름}} 트레이너
            
            ![트레이너 프로필]({{프로필이미지_URL}})
            
            **전문 분야**: {{전문_분야}}
            **경력**: {{경력}}년
            **방문 가능 지역**: {{방문_가능_지역}}
            **평점**: ⭐{{평점}} ({{리뷰_수}}개의 리뷰)
            
            ### 소개
            {{소개_내용}}
            
            ### 대표 경력
            {{대표_경력_내용}}
            
            ### 자격증
            - {{자격증1}} ({{발급기관1}})
            - {{자격증2}} ({{발급기관2}})
            - {{자격증3}} ({{발급기관3}})
            
            ---
            """;

//    String allTagsText = tagRepository.findAll().stream()
//            .map(Tag::getTagName)
//            .collect(Collectors.joining(", "));
//
//    String allAreasText = trainerRepository.findAll().stream()
//            .map(Trainer::getVisitingAreas)
//            .collect(Collectors.joining(", "));

    @Tool(name = "getTrainerInfo", description = "훈련사, 트레이너를 찾아달라는 요청, 훈련사가 필요한 상황을 포함한 요청이 있을 경우 사용하세요. 요청에 맞는 훈련사를 찾아줍니다. 태그, 지역에 맞는 훈련사를 찾아줍니다. 요청에 태그, 지역 중 하나가 없는 경우 요청에 있는 것으로만 찾습니다. 태그, 지역의 범위는 가변적입니다. (예시: 태그는 강아지 훈련, 강아지, 고양이, 고양이 훈련 처럼 범위가 클 수도 있고 분리불안, 배변훈련 처럼 범위가 작을수도 있습니다. 지역은 서울, 경기도, 부산 처럼 범위가 클 수도 있고 강남구, 용산구, 기흥구, 수지구 처럼 범위가 작을수도 있습니다.). 큰 범위의 태그, 지역이 지정된 경우 포함되는 작은 범위도 포함합니다. (예시: 강아지를 요청한 경우 분리불안, 강아지, 배변훈련 등 모두 포함. 서울을 요청한 경우 강남구, 용산구 등 모두 포함). 범위에 대해 사용자에게 집요하게 요청하지 마세요. 이 메서드에서 return 된 TrainerDTO를 사용하여 훈련사 별 훈련사 프로필 카드로 출력하세요. 출력형태는 마크다운이며 훈련사 프로필 카드 템플릿을 따라 출력합니다. 훈련사 프로필 카드에 비어있는 값의 항목은 출력하지 않습니다. (예시: - 자격증1 (발급기관1)\\n- None (None) 일 경우 - None (None)는 출력하지 않고 - 자격증1 (발급기관1) 만 출력)\n훈련사 프로필 카드 템플릿: \n" + trainerCardTemplate + "\n")
    public List<TrainerDTO> getTrainerInfo(
            @ToolParam(description = "사용자 요청에서 추출한 태그 목록입니다. 태그, 지역의 범위는 가변적입니다. (예시: 태그는 강아지 훈련, 강아지, 고양이, 고양이 훈련 처럼 범위가 클 수도 있고 분리불안, 배변훈련 처럼 범위가 작을수도 있습니다. 지역은 서울, 경기도, 부산 처럼 범위가 클 수도 있고 강남구, 용산구, 기흥구, 수지구 처럼 범위가 작을수도 있습니다.). 큰 범위의 태그, 지역이 지정된 경우 포함되는 작은 범위도 포함합니다. (예시: 강아지를 요청한 경우 분리불안, 강아지, 배변훈련 등 모두 포함. 서울을 요청한 경우 강남구, 용산구 등 모두 포함). 범위에 대해 사용자에게 집요하게 요청하지 마세요.")
            List<String> tags,
            @ToolParam(description = "사용자 요청에서 추출한 지역 목록입니다. 태그, 지역의 범위는 가변적입니다. (예시: 태그는 강아지 훈련, 강아지, 고양이, 고양이 훈련 처럼 범위가 클 수도 있고 분리불안, 배변훈련 처럼 범위가 작을수도 있습니다. 지역은 서울, 경기도, 부산 처럼 범위가 클 수도 있고 강남구, 용산구, 기흥구, 수지구 처럼 범위가 작을수도 있습니다.). 큰 범위의 태그, 지역이 지정된 경우 포함되는 작은 범위도 포함합니다. (예시: 강아지를 요청한 경우 분리불안, 강아지, 배변훈련 등 모두 포함. 서울을 요청한 경우 강남구, 용산구 등 모두 포함). 범위에 대해 사용자에게 집요하게 요청하지 마세요.")
            List<String> areas
    ) {

        // 1. tags와 areas 모두 충족하는 훈련사가 있는 경우, 해당 훈련사 목록 리턴
        List<Trainer> trainers = trainerRepository.findAllByTagsAndAreas(tags, areas);
        if (!trainers.isEmpty()) {
            return trainers.stream()
                    .map(trainer -> new TrainerDTO(
                            trainer.getTrainerId(),
                            trainer.getUser().getNickname(),
                            trainer.getUser().getProfileImageUrl(),
                            trainer.getUser().getEmail(),
                            trainer.getTitle(),
                            trainer.getIntroduction(),
                            trainer.getRepresentativeCareer(),
                            trainer.getSpecializationText(),
                            trainer.getVisitingAreas(),
                            trainer.getExperienceYears(),
                            null, // photos
                            null, // serviceFees
                            null, // specializations
                            null, // certifications
                            0.0, // averageRating
                            0L // reviewCount
                    ))
                    .collect(Collectors.toList());
        }

        // 2. tags, areas를 각각 충족하는 훈련사 찾으면 tags, areas를 각각 충족하는 훈련사 리턴
        List<Trainer> tagTrainers = trainerRepository.findAllByTags(tags);
        List<Trainer> areaTrainers = trainerRepository.findAllByAreas(areas);
        if (!tagTrainers.isEmpty() || !areaTrainers.isEmpty()) {
            List<Trainer> combinedTrainers = new ArrayList<>();
            if (!tagTrainers.isEmpty()) {
                combinedTrainers.addAll(tagTrainers);
            }
            if (!areaTrainers.isEmpty()) {
                combinedTrainers.addAll(areaTrainers);
            }
            return combinedTrainers.stream()
                    .map(trainer -> new TrainerDTO(
                            trainer.getTrainerId(),
                            trainer.getUser().getNickname(),
                            trainer.getUser().getProfileImageUrl(),
                            trainer.getUser().getEmail(),
                            trainer.getTitle(),
                            trainer.getIntroduction(),
                            trainer.getRepresentativeCareer(),
                            trainer.getSpecializationText(),
                            trainer.getVisitingAreas(),
                            trainer.getExperienceYears(),
                            null, // photos
                            null, // serviceFees
                            null, // specializations
                            null, // certifications
                            0.0, // averageRating
                            0L // reviewCount
                    ))
                    .collect(Collectors.toList());
        }


        // 3. tags와 areas 모두 충족하는 훈련사, tags, areas를 각각 충족하는 훈련사 모두 없는 경우 비어있는 List<TrainerDTO> 리턴
        return new ArrayList<>(); // 비어있는 리스트 리턴
    }

}