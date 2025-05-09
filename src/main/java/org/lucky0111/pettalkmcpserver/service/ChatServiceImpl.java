package org.lucky0111.pettalkmcpserver.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerDTO;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Trainer;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerRepository;
import org.lucky0111.pettalkmcpserver.service.trainer.TrainerService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// TrainerResponseDTO 클래스 추가
@Data
class TrainerResponseDTO {
    private List<TrainerDTO> trainers;
    private boolean found;

    public TrainerResponseDTO(List<TrainerDTO> trainers) {
        this.trainers = trainers;
        this.found = !trainers.isEmpty();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final TrainerRepository trainerRepository;
    private final TrainerService trainerService;
    private final int MAX_TRAINERS = 3; // 최대 반환할 훈련사 수를 상수로 정의

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
            
            ### 훈련사 프로필 페이지
            [{{트레이너_이름}} 프로필 페이지](https://mass-jandy-lucky0111-ed8f3811.koyeb.app/trainers/profile/{{트레이너_닉네임}})
            ---
            """;

    private final String regionHierarchyToString = """
    - 수도권 → 서울, 경기, 인천
       - 서울 → 강남, 강동, 강북, 강서, 관악, 광진, 구로, 금천, 노원, 도봉, 동대문, 동작, 마포, 서대문, 서초, 성동, 성북, 송파, 양천, 영등포, 용산, 은평, 종로, 중구, 중랑
       - 경기 → 수원, 성남, 용인, 부천, 안산, 안양, 평택, 시흥, 김포, 광주, 광명, 군포, 하남, 오산, 이천, 안성, 의왕, 양평, 여주, 과천, 고양, 의정부, 동두천, 구리, 남양주, 파주, 양주, 포천, 연천, 가평
       - 인천 → 중구, 동구, 미추홀구, 연수구, 남동구, 부평구, 계양구, 서구, 강화군, 옹진군
       - 부산 → 중구, 서구, 동구, 영도구, 부산진구, 동래구, 남구, 북구, 해운대구, 사하구, 금정구, 강서구, 연제구, 수영구, 사상구, 기장군
       - 대구 → 중구, 동구, 서구, 남구, 북구, 수성구, 달서구, 달성군
       - 광주 → 동구, 서구, 남구, 북구, 광산구
       - 대전 → 동구, 중구, 서구, 유성구, 대덕구
       - 울산 → 중구, 남구, 동구, 북구, 울주군
       - 세종 → 세종
       - 강원 → 춘천, 원주, 강릉, 동해, 태백, 속초, 삼척, 홍천, 횡성, 영월, 평창, 정선, 철원, 화천, 양구, 인제, 고성, 양양
       - 충북 → 청주, 충주, 제천, 보은, 옥천, 영동, 증평, 진천, 괴산, 음성, 단양
       - 충남 → 천안, 공주, 보령, 아산, 서산, 논산, 계룡, 당진, 금산, 부여, 서천, 청양, 홍성, 예산, 태안
       - 전북 → 전주, 군산, 익산, 정읍, 남원, 김제, 완주, 진안, 무주, 장수, 임실, 순창, 고창, 부안
       - 전남 → 목포, 여수, 순천, 나주, 광양, 담양, 곡성, 구례, 고흥, 보성, 화순, 장흥, 강진, 해남, 영암, 무안, 함평, 영광, 장성, 완도, 진도, 신안
       - 경북 → 포항, 경주, 김천, 안동, 구미, 영주, 영천, 상주, 문경, 경산, 군위, 의성, 청송, 영양, 영덕, 청도, 고령, 성주, 칠곡, 예천, 봉화, 울진, 울릉
       - 경남 → 창원, 진주, 통영, 사천, 김해, 밀양, 거제, 양산, 의령, 함안, 창녕, 고성, 남해, 하동, 산청, 함양, 거창, 합천
       - 제주 → 제주시, 서귀포시
    """;

    @Tool(name = "getTrainerInfo", description = """
    훈련사, 트레이너를 찾아달라는 요청이 있을 경우 이 도구를 사용하세요.
    
    사용자의 요청에 맞는 훈련사를 찾아주는 과정:
    1. 사용자의 상황(반려동물 종류, 문제 행동 등)을 파악하여 적절한 태그 추출
    2. 사용자의 지역을 고려하여 가까운 훈련사 추천 (상위 지역이 언급된 경우 모든 하위 지역을 자동으로 포함)
    3. 각 훈련사의 전문성, 경력, 자격증을 기반으로 상황에 적합한 1-3명 추천 (항상 최대 3명만 반환)
    4. 훈련사가 없는 경우 일반적인 조언과 다른 검색어 제안
    
    태그와 지역 검색 범위:
    - 태그는 넓은 범위(강아지 훈련, 고양이 훈련)부터 좁은 범위(분리불안, 배변훈련)까지 다양
    - 지역은 넓은 범위(서울, 경기)부터 좁은 범위(강남, 용산, 용인, 수원, 성남)까지 다양
    - 범위에 대해 사용자에게 추가 질문은 지양
    
    커뮤니케이션 스타일:
    - 먼저 사용자의 상황을 요약하여 공감 표현
    - 추천 훈련사와 함께 간단한 상황별 조언 제공
    - 각 훈련사의 프로필 링크를 안내하여 상세 정보 확인 유도
    - 친근하고 전문적인 톤 유지
    - 반드시 한국어로만 응답합니다. 다른 언어의 단어를 절대 사용하지 마세요.
    - 먼저 사용자의 상황을 요약하여 공감 표현
    - 간단한 상황별 조언 제공
    
    응답 형식:
    - 훈련사 프로필 카드는 마크다운 형식으로 표시
    - 훈련사 프로필 카드 출력 이전에 후속 질문 제안
    - 훈련사 프로필 카드는 사용자 요청에 대한 답변 이후에 출력
    - 훈련사를 찾은 경우(TrainerResponseDTO의 found가 true인 경우) "요청하신 훈련사 프로필 카드입니다."라는 문구로 시작
    - 훈련사를 찾지 못한 경우(TrainerResponseDTO의 found가 false인 경우) "훈련사를 찾지 못했습니다."라는 문구로 시작 
    - **매우 중요: TrainerResponseDTO의 found가 false인 경우(trainers가 비어있는 경우) 절대로 훈련사 프로필 카드를 생성하지 마세요! 없는 데이터를 임의로 만들지 마세요! 대신 "죄송합니다. 요청하신 조건에 맞는 훈련사를 찾을 수 없습니다."라고 안내하세요.**
    - **매우 중요: 훈련사 정보는 오직 TrainerResponseDTO.trainers에 포함된 실제 데이터만 사용하세요. 임의로 정보를 생성하거나 바꾸지 마세요.**
    - TrainerResponseDTO의 trainers에 TrainerDTO가 존재하는 경우에만 해당 TrainerDTO의 정보로 훈련사 프로필 카드 출력
    - 항목이 비어있는 경우 해당 항목은 출력하지 않음 (예시: - 자격증1 (발급기관1)\\n- None (None) 일 경우 - None (None)는 출력하지 않고 - 자격증1 (발급기관1) 만 출력, )
    - 요청 지역과 실제 훈련사 활동 지역이 다를 경우, 이를 명확히 안내. 예: "요청하신 제주 지역에는 훈련사가 없지만, 서울에서 활동하는 훈련사를 찾았습니다."
    - 매우 중요: 훈련사 정보는 오직 TrainerResponseDTO.trainers에 포함된 실제 데이터만 사용하세요. 임의로 정보를 생성하거나 바꾸지 마세요. TrainerResponseDTO의 trainers에 TrainerDTO가 존재하는 경우에만 해당 TrainerDTO의 정보로 훈련사 프로필 카드를 출력하세요. TrainerResponseDTO의 trainers가 비어있는 경우 훈련사 프로필 카드를 생성하지 마세요. 대신 "죄송합니다. 요청하신 조건에 맞는 훈련사를 찾을 수 없습니다."라고 안내하세요.
    이 지침을 정확히 따라 사용자 요청 조건과 검색 결과를 명확하게 설명하세요.
    
    훈련사 프로필 카드 템플릿:
    """ + trainerCardTemplate)
    public TrainerResponseDTO getTrainerInfo(
            @ToolParam(description = """
           사용자 요청에서 추출한 태그 목록입니다. 다음과 같은 내용을 태그로 추출하세요:
           
           1. 반려동물 종류: 강아지, 고양이 등
           2. 문제 행동: 분리불안, 짖음, 공격성, 배변문제 등
           3. 훈련 목표: 기본훈련, 행동교정, 복종훈련 등
           
           태그는 다양한 범위로 추출할 수 있습니다:
           - 넓은 범위: 강아지, 고양이
           - 좁은 범위: 분리불안, 배변
           
           태그에 중요한 키워드만 포함합니다. (예시: '강이지 훈련'일 경우 '강아지' 만 입력, '공격성 제어' 일 경우 '공격성' 만 입력, '배변 훈련' 일 경우 '배변' 만 입력)

           범위에 대해 사용자에게 추가 질문은 지양
           """)
            List<String> tags,
            @ToolParam(description = """
           사용자 요청에서 추출한 지역 목록입니다. 다음과 같은 지역 정보를 추출하세요:
           
           지역은 다양한 범위로 추출할 수 있습니다:
           - 넓은 범위: 서울, 경기, 부산 (광역시, 특별시, 도)
           - 좁은 범위: 강남, 용산, 용인, 수원, 성남 (시, 구)
           
           매우 중요: 사용자가 상위 지역을 언급하면, 해당 지역과 모든 하위 지역을 areas 배열에 함께 포함해야 합니다.
           예시:
           - 사용자가 "서울"만 언급했다면: areas = ["서울", "강남", "강동", "강북", "강서", "관악", "광진", "구로", "금천", "노원", "도봉", "동대문", "동작", "마포", "서대문", "서초", "성동", "성북", "송파", "양천", "영등포", "용산", "은평", "종로", "중구", "중랑"]
           - 사용자가 "경기"만 언급했다면: areas = ["경기", "수원", "성남", "용인", "부천", "안산", "안양", "평택", "시흥", "김포", "광주", "광명", "군포", "하남", "오산", "이천", "안성", "의왕", "양평", "여주", "과천", "고양", "의정부", "동두천", "구리", "남양주", "파주", "양주", "포천", "연천", "가평"]
           - 사용자가 "수도권"만 언급했다면: areas = ["수도권", "서울", "경기", "인천", "강남", "강동", ... 모든 하위 지역 포함]

           프로그램에서 따로 지역 확장을 하지 않으므로, 여기서 상위 지역에 대한 모든 하위 지역을 직접 포함시켜야 합니다.
           
           지역 계층 구조:
           """ + regionHierarchyToString)
            List<String> areas
    ) {
        // 입력값 로깅
        log.info("Received tags: {}", tags);
        log.info("Received areas: {}", areas);

        List<TrainerDTO> trainerDTOs = getTrainersByTagsAndAreas(tags, areas);

        // 최대 MAX_TRAINERS(3)개만 반환
        if (trainerDTOs.size() > MAX_TRAINERS) {
            trainerDTOs = trainerDTOs.subList(0, MAX_TRAINERS);
        }

        // 명시적인 결과 객체 반환
        return new TrainerResponseDTO(trainerDTOs);
    }

    private List<TrainerDTO> getTrainersByTagsAndAreas(List<String> tags, List<String> areas) {
        // 1. tags와 areas 모두 충족하는 훈련사가 있는 경우, 해당 훈련사 목록 리턴
        List<Trainer> trainers = trainerRepository.findAllByTagsAndAreas(tags, areas);
        if (!trainers.isEmpty()) {
            return trainers.stream()
                    .map(trainer -> trainerService.getTrainerDetails(trainer.getUser().getNickname()))
                    .collect(Collectors.toList());
        }

        // 2. tags, areas를 각각 충족하는 훈련사 찾으면 tags, areas를 각각 충족하는 훈련사 리턴
        List<Trainer> tagTrainers = trainerRepository.findAllByTags(tags);
        List<Trainer> areaTrainers = trainerRepository.findAllByAreas(areas);
        if (!tagTrainers.isEmpty() || !areaTrainers.isEmpty()) {
            Set<String> trainerNicknames = new HashSet<>();
            List<TrainerDTO> combinedTrainers = new ArrayList<>();

            // 태그 기반 훈련사 처리 (최대 MAX_TRAINERS/2개)
            if (!tagTrainers.isEmpty()) {
                for (Trainer trainer : tagTrainers) {
                    if (trainerNicknames.add(trainer.getUser().getNickname())) {
                        combinedTrainers.add(trainerService.getTrainerDetails(trainer.getUser().getNickname()));
                        // 태그 기반 훈련사는 최대 MAX_TRAINERS/2개로 제한
                        if (combinedTrainers.size() >= MAX_TRAINERS / 2 && !areaTrainers.isEmpty()) {
                            break;
                        }
                        // 태그 기반 훈련사만 있는 경우 MAX_TRAINERS개 까지 추가
                        if (combinedTrainers.size() >= MAX_TRAINERS && areaTrainers.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // 지역 기반 훈련사 처리 (남은 슬롯 채우기)
            if (!areaTrainers.isEmpty()) {
                for (Trainer trainer : areaTrainers) {
                    if (trainerNicknames.add(trainer.getUser().getNickname())) {
                        combinedTrainers.add(trainerService.getTrainerDetails(trainer.getUser().getNickname()));
                        // 전체 MAX_TRAINERS개 제한에 도달하면 중단
                        if (combinedTrainers.size() >= MAX_TRAINERS) {
                            break;
                        }
                    }
                }
            }

            return combinedTrainers;
        }

        // 3. tags와 areas 모두 충족하는 훈련사, tags, areas를 각각 충족하는 훈련사 모두 없는 경우 비어있는 List<TrainerDTO> 리턴
        return new ArrayList<>(); // 비어있는 리스트 리턴
    }

    // 필요에 따라 특정 훈련사의 상세 정보를 가져오는 메서드 추가
    public TrainerDTO getTrainerDetailsByNickname(String nickname) {
        return trainerService.getTrainerDetails(nickname);
    }
}