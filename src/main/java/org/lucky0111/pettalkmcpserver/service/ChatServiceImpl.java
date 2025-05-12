package org.lucky0111.pettalkmcpserver.service;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lucky0111.pettalkmcpserver.domain.dto.trainer.TrainerDTO;
import org.lucky0111.pettalkmcpserver.domain.entity.common.Tag;
import org.lucky0111.pettalkmcpserver.domain.entity.community.Post;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Trainer;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.exception.CustomException;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.lucky0111.pettalkmcpserver.repository.community.PostRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerRepository;
import org.lucky0111.pettalkmcpserver.repository.user.PetUserRepository;
import org.lucky0111.pettalkmcpserver.service.trainer.TrainerService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// TrainerResponseDTO 클래스 추가
@Data
class ChatTrainerResponseDTO {
    private Map<TrainerDTO, String> trainerSearchTypes; // 각 TrainerDTO와 검색 유형을 직접 연결
    private boolean found;

    public ChatTrainerResponseDTO(Map<TrainerDTO, String> trainerSearchTypes) {
        this.trainerSearchTypes = trainerSearchTypes;
        this.found = !trainerSearchTypes.isEmpty();
    }

    // 필요한 경우 trainers 리스트를 반환하는 메서드 추가
    public List<TrainerDTO> getTrainers() {
        return new ArrayList<>(trainerSearchTypes.keySet());
    }
}

@Data
class ChatPostDTO {
    private Long postId;
    private String title;
    private String content;
}

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final PostRepository postRepository;
    private final PetUserRepository petUserRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerService trainerService;
    private final int MAX_TRAINERS = 4; // 최대 반환할 훈련사 수를 상수로 정의
    private final int MAX_POSTS = 4; // 최대 반환할 게시글 수를 상수로 정의

    // 게시글 카드 템플릿
    private final String postCardTemplate = """
        ---
        ## 🐾 {{게시글_제목}}
        ### 3줄 요약\\n
        {{게시글_내용_3줄 요약}}\\n
        👉 [자세히 보기](https://mass-jandy-lucky0111-ed8f3811.koyeb.app/community/post/{{게시글_ID}})
        ---
        """;
    private final String trainerCardTemplate = """
            ---
            
            ## 🐾 {{트레이너_이름}} 트레이너
            
            ![트레이너 프로필]({{프로필이미지_URL}})
            
            ### **전문 분야**: {{전문_분야}}
            ### **경력**: {{경력}}년
            ### **방문 가능 지역**: {{방문_가능_지역}}
            ### **평점**: ⭐{{평점}} ({{리뷰_수}}개의 리뷰)
            
            ### 소개
            {{소개_내용}}
            
            ### 대표 경력
            {{대표_경력_내용}}
            
            ### 자격증
            - {{자격증1}} ({{발급기관1}})
            - {{자격증2}} ({{발급기관2}})
            - {{자격증3}} ({{발급기관3}})
            
            ### 👇 문의하기
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
    1. **DB에 저장된 태그 목록, 훈련사 지역 목록을 요청합니다.** 툴 이름: getTrainerAreas, getTagsInDB
    2. 사용자의 상황(반려동물 종류, 문제 행동 등)을 파악하여 적절한 태그, 지역 추출 (**태그 또는 지역은 사용자가 언급한 것만 사용, 언급하지 않은 경우 입력받지 않아도 됨, 사용자에게 집요하게 요청 금지**)
    3. 각 훈련사의 태그, 지역 기반으로 상황에 적합한 1-4명 추천 (항상 최대 4명만 반환, **태그 또는 지역이 없을 경우 해당 조건으로만 검색**)
    4. 훈련사가 없는 경우 일반적인 조언과 다른 검색어 제안
    
    태그와 지역 검색 범위:
    - 태그는 넓은 범위(강아지 훈련, 고양이 훈련)부터 좁은 범위(분리불안, 배변훈련)까지 다양
    - 지역은 넓은 범위(서울, 경기)부터 좁은 범위(강남, 용산, 용인, 수원, 성남)까지 다양
    - 범위에 대해 사용자에게 추가 질문은 지양
    
    커뮤니케이션 스타일:
    - **먼저 사용자의 상황을 요약하여 공감 표현**
    - **추천 훈련사와 함께 간단한 상황별 조언 제공**
    - 각 훈련사의 프로필 링크를 안내하여 상세 정보 확인 유도
    - 친근하고 전문적인 톤 유지
    - 반드시 한국어로만 응답합니다. 다른 언어의 단어를 절대 사용하지 마세요.
    - 먼저 사용자의 상황을 요약하여 공감 표현
    - 간단한 상황별 조언 제공
    
    응답 형식:
    - 훈련사 프로필 카드는 마크다운 형식으로 표시 (코드블록 금지, 마크다운으로만 출력)
    - 훈련사 프로필 카드 출력 이전에 후속 질문 제안
    - 훈련사 프로필 카드는 사용자 요청에 대한 답변 이후에 출력
    - TrainerResponseDTO에는 Map<TrainerDTO, String> 형태의 trainerSearchTypes가 있으며, 각 TrainerDTO와 해당 검색 유형("tag", "area", "both" 중 하나)이 연결되어 있습니다.
    - 각 TrainerDTO의 검색 유형을 확인하여 적절한 설명을 제공하십시오:
      - "both": 태그와 지역 모두 일치하는 훈련사
      - "tag": 태그만 일치하는 훈련사
      - "area": 지역만 일치하는 훈련사
    훈련사 검색 방식에 따라 분류하여 훈련사 프로필 카드를 출력합니다.
      - searchType이 "both"인 경우: "태그와 지역에 맞는 훈련사:"
      - searchType이 "tag"인 경우: "태그에 맞는 훈련사:"
      - searchType이 "area"인 경우: "지역에 맞는 훈련사:"
      - 예시: "
              # 태그와 지역에 맞는 훈련사:\\n
              ---
              {훈련사1 프로필 카드}\\n
              ---
              {훈련사2 프로필 카드}\\n
              ---
              # 태그에 맞는 훈련사:\\n
              ---
              {훈련사3 프로필 카드}\\n
              ---
              {훈련사4 프로필 카드}\\n
              ---
              # 지역에 맞는 훈련사:\\n
              ---
              {훈련사5 프로필 카드}\\n
              ---
              {훈련사6 프로필 카드}\\n
              ---
            "
      - 훈련사 프로필 카드 출력 시 태그와 지역에 맞는 훈련사, 태그에 맞는 훈련사, 지역에 맞는 훈련사 순서로 출력
    - 훈련사를 찾은 경우(TrainerResponseDTO의 found가 true인 경우) "훈련사를 찾았습니다."라는 문구로 시작
    - 훈련사를 찾지 못한 경우(TrainerResponseDTO의 found가 false인 경우) "훈련사를 찾지 못했습니다."라는 문구로 시작
    - **매우 중요: TrainerResponseDTO의 found가 false인 경우(trainers가 비어있는 경우) 절대로 훈련사 프로필 카드를 생성하지 마세요! 없는 데이터를 임의로 만들지 마세요! 대신 "죄송합니다. 요청하신 조건에 맞는 훈련사를 찾을 수 없습니다."라고 안내하세요.**
    - **매우 중요: 훈련사 정보는 오직 TrainerResponseDTO.trainers에 포함된 실제 데이터만 사용하세요. 임의로 정보를 생성하거나 바꾸지 마세요.**
    - TrainerResponseDTO의 trainers에 TrainerDTO가 존재하는 경우에만 해당 TrainerDTO의 정보로 훈련사 프로필 카드 출력
    - 항목이 비어있는 경우 해당 항목은 출력하지 않음 (예시: None 일 경우 출력하지 않음)
    - 매우 중요: 훈련사 정보는 오직 TrainerResponseDTO.trainers에 포함된 실제 데이터만 사용하세요. 임의로 정보를 생성하거나 바꾸지 마세요. TrainerResponseDTO의 trainers에 TrainerDTO가 존재하는 경우에만 해당 TrainerDTO의 정보로 훈련사 프로필 카드를 출력하세요. TrainerResponseDTO의 trainers가 비어있는 경우 훈련사 프로필 카드를 생성하지 마세요. 대신 "죄송합니다. 요청하신 조건에 맞는 훈련사를 찾을 수 없습니다."라고 안내하세요.
    이 지침을 정확히 따라 사용자 요청 조건과 검색 결과를 명확하게 설명하세요.
    
    훈련사 프로필 카드 템플릿:
    """ + trainerCardTemplate)
    public ChatTrainerResponseDTO getTrainerInfo(
            @ToolParam(description = """
           사용자 요청에서 추출한 태그 목록입니다. 다음과 같은 내용을 태그로 추출하세요:
           
           태그 추출 순서:
           1. **DB에 저장된 태그 목록을 요청합니다.** 툴 이름: getTagsInDB
           2. DB에 있는 태그 목록을 기반으로 사용자가 제공한 정보에 맞는 태그를 추출합니다.
           
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
           
           지역 추출 순서:
           1. **DB에 저장된 훈련사 지역 목록을 요청합니다.** 툴 이름: getTrainerAreas
           2. DB에 있는 훈련사 지역 목록을 기반으로 사용자가 제공한 정보에 맞는 지역를 추출합니다.
           
           지역은 다양한 범위로 추출할 수 있습니다:
           - 넓은 범위: 서울, 경기, 부산 (광역시, 특별시, 도)
           - 좁은 범위: 강남, 용산, 용인, 수원, 성남 (시, 구)
           
           매우 중요: 사용자가 상위 지역을 언급하면, 해당 지역과 모든 하위 지역을 areas 배열에 함께 포함해야 합니다.
           예시:
           - 사용자가 "서울"만 언급했다면: areas = ["서울", "강남", "강동", "강북", "강서", "관악", "광진", "구로", "금천", "노원", "도봉", "동대문", "동작", "마포", "서대문", "서초", "성동", "성북", "송파", "양천", "영등포", "용산", "은평", "종로", "중구", "중랑"]
           - 사용자가 "경기"만 언급했다면: areas = ["경기", "수원", "성남", "용인", "부천", "안산", "안양", "평택", "시흥", "김포", "광주", "광명", "군포", "하남", "오산", "이천", "안성", "의왕", "양평", "여주", "과천", "고양", "의정부", "동두천", "구리", "남양주", "파주", "양주", "포천", "연천", "가평"]
           - 사용자가 "수도권"만 언급했다면: areas = ["수도권", "서울", "경기", "인천", "강남", "강동", ... DB에 있는 모든 하위 지역 포함]

           프로그램에서 따로 지역 확장을 하지 않으므로, 여기서 상위 지역에 대한 DB 내 모든 하위 지역을 직접 포함시켜야 합니다.
           
           여기서 DB에 저장된 지역만 추출합니다. (예시: DB(["경기", "동탄", "광진, 성동, 잠실, 강남", "서울", "남양주, 동두천, 의정부", "성남", "해운대", "부산", "분당"] 이고 요청이 "경기 지역 훈련사 찾아주세요"일 경우 ["경기", "동탄", "남양주", "동두천", "의정부", "성남", "분당"] 만 포함))
           
           지역 계층 구조는 큰 틀의 예시이며 모든 하위 지역을 포함하지 않는 참고용 예시입니다. 그러므로 동탄, 분당 같은 변수가 있을 수 있습니다.
           
           지역 계층 구조:
           """ + regionHierarchyToString)
            List<String> areas
    ) {
        // 입력값 로깅
        log.info("Received tags: {}", tags);
        log.info("Received areas: {}", areas);

        // 훈련사 검색 및 검색 유형 정보 가져오기
        Map<TrainerDTO, String> trainerSearchTypes = getTrainersByTagsAndAreas(tags, areas);

        log.info("Trainer search types: {}", trainerSearchTypes);

        // 최대 MAX_TRAINERS(4)개로 제한
        if (trainerSearchTypes.size() > MAX_TRAINERS) {
            Map<TrainerDTO, String> limitedTrainerSearchTypes = new HashMap<>();
            int count = 0;

            for (Map.Entry<TrainerDTO, String> entry : trainerSearchTypes.entrySet()) {
                limitedTrainerSearchTypes.put(entry.getKey(), entry.getValue());
                count++;

                if (count >= MAX_TRAINERS) {
                    break;
                }
            }

            trainerSearchTypes = limitedTrainerSearchTypes;
        }

        // 명시적인 결과 객체 반환
        return new ChatTrainerResponseDTO(trainerSearchTypes);
    }

    private Map<TrainerDTO, String> getTrainersByTagsAndAreas(List<String> tags, List<String> areas) {
        Map<TrainerDTO, String> trainerSearchTypes = new HashMap<>(); // 각 TrainerDTO와 검색 유형 연결

        // 중복 방지를 위한 Set
        Set<String> addedTrainerNicknames = new HashSet<>();

        // 1. tags와 areas 모두 충족하는 훈련사가 있는 경우
        if(!tags.isEmpty() && !areas.isEmpty()) {
            List<Trainer> trainers = trainerRepository.findAllByTagsAndAreas(tags, areas);
            if (!trainers.isEmpty()) {
                for (Trainer trainer : trainers) {
                    String nickname = trainer.getUser().getNickname();
                    if (addedTrainerNicknames.add(nickname)) {
                        TrainerDTO trainerDTO = trainerService.getTrainerDetails(nickname);
                        trainerSearchTypes.put(trainerDTO, "both"); // 각 TrainerDTO와 검색 유형 연결

                        log.info("Found both trainer: {}", trainerDTO);
                        if (trainerSearchTypes.size() >= MAX_TRAINERS) {
                            break;
                        }
                    }
                }
            }
        }

        // 2. 태그 기반 훈련사 처리 (아직 MAX_TRAINERS에 도달하지 않은 경우)
        int tagCount = 0;
        if (!tags.isEmpty() && trainerSearchTypes.size() < MAX_TRAINERS) {
            List<Trainer> tagTrainers = trainerRepository.findAllByTags(tags);
            for (Trainer trainer : tagTrainers) {
                String nickname = trainer.getUser().getNickname();
                // 중복 체크
                if (addedTrainerNicknames.add(nickname)) {
                    TrainerDTO trainerDTO = trainerService.getTrainerDetails(nickname);
                    trainerSearchTypes.put(trainerDTO, "tag"); // 각 TrainerDTO와 검색 유형 연결

                    log.info("Found tag trainer: {}", trainerDTO);
                    tagCount++;
                    if (trainerSearchTypes.size() >= MAX_TRAINERS / 2) {
                        break;
                    }
                }
            }
        }

        // 3. 지역 기반 훈련사 처리
        int areaCount = 0;
        if (tagCount <= 1) {
            tagCount = MAX_TRAINERS - trainerSearchTypes.size();
        }
        if (!areas.isEmpty()) {
            List<Trainer> areaTrainers = trainerRepository.findAllByAreas(areas);
            for (Trainer trainer : areaTrainers) {
                String nickname = trainer.getUser().getNickname();
                // 중복 체크
                if (addedTrainerNicknames.add(nickname)) {
                    TrainerDTO trainerDTO = trainerService.getTrainerDetails(nickname);
                    trainerSearchTypes.put(trainerDTO, "area"); // 각 TrainerDTO와 검색 유형 연결

                    log.info("Found area trainer: {}", trainerDTO);
                    areaCount++;
                    if (areaCount >= tagCount) {
                        break;
                    }
                }
            }
        }

        return trainerSearchTypes;
    }


    @Tool(name = "getTrainerDetailsByName", description = """
    훈련사 이름으로 훈련사 정보를 조회할 경우 이 도구를 사용하세요.
    사용자의 요청에 맞는 훈련사 정보를 조회하는 과정:
    1. 훈련사 이름을 기반으로 훈련사 정보를 조회
    2. 훈련사 정보가 존재하는 경우 해당 정보를 반환
    3. 훈련사 정보가 존재하지 않는 경우 "훈련사 정보를 찾을 수 없습니다."라는 메시지 반환
    4. 훈련사 정보가 존재하는 경우 훈련사 프로필 카드 출력
    
    훈련사 프로필 카드 템플릿:
    """ + trainerCardTemplate)
    public TrainerDTO getTrainerDetailsByName(
            @ToolParam (description = """
            훈련사 이름을 입력하세요.
            훈련사 이름은 사용자가 요청한 훈련사 이름입니다.
            훈련사 이름만 출력합니다.
            (예시: '홍길동 훈련사'일 경우 '홍길동' 만 입력, '강형욱 훈련사'일 경우 '강형욱' 만 입력)
            """)
            String name) {
        // 입력값 로깅
        log.info("Received trainer nickname: {}", name);

        // 훈련사 정보 조회
        String nickname = petUserRepository.findByName(name).getNickname();
        if (nickname == null) {
            log.warn("Trainer not found for nickname: {}", name);
            return null; // 훈련사 정보가 없는 경우 null 반환
        }

        return trainerService.getTrainerDetails(nickname);
    }

    @Tool(name = "getTrainerAreas", description = """
    이 도구는 DB에 저장된 모든 훈련사 지역 정보 목록을 가져옵니다.
    **DB에 저장된 훈련사 지역 목록을 요청할 경우 이 도구를 사용하세요.**
    
    ### 사용 예시
    - "사용자 요청에서 지역 정보를 추출하기 위해 DB에 있는 훈련사 지역 목록을 가져와주세요"
    - "사용자가 언급한 지역이 훈련사 데이터베이스에 있는지 확인하고 싶습니다"
    
    ### 용도
    - 사용자 쿼리에서 지역 정보 추출 시 참조 목록으로 활용
    - 지역명 표준화 및 일관성 유지
    - 지역 기반 훈련사 검색 시 사용될 올바른 지역명 확인
    
    ### 반환 데이터
    - List<String> 형태의 모든 훈련사 지역 정보 목록을 반환합니다
    - 각 문자열은 DB에 저장된 정확한 지역 정보입니다
    - 각 훈련사마다 방문 가능 지역 정보가 포함됩니다
    
    ### 사용 방법
    1. 사용자의 질문이나 요청에서 지역 관련 정보를 파악하세요
    2. 이 도구를 호출하여 DB에 있는 훈련사 지역 목록을 확인하세요
    3. 사용자 요청의 지역 정보와 DB의 지역 정보를 비교하여 일치하는 지역을 선택하세요
    4. 선택된 지역 정보를 기반으로 훈련사 검색 또는 게시글 검색 또는 다른 작업을 수행하세요
    
    ### 지역 선택 규칙
    - 사용자가 언급한 지역과 가장 일치하는 지역을 선택하세요
    - 대도시나 행정구역 단위(서울, 경기 등)와 하위 지역(강남구, 분당구 등)을 모두 고려하세요
    - 사용자가 언급한 지역이 DB에 없는 경우, 가장 가까운 상위 지역을 선택하세요
    
    ### 응답 활용
    - 이 도구의 결과는 훈련사 검색 시 지역 기반 필터링에 활용할 수 있습니다
    - 사용자에게 특정 지역의 훈련사 정보를 제공할 때 정확한 지역명 참조에 사용하세요
    """)
    public List<String> getTrainerAreas() {
        // DB에 저장된 훈련사 지역 정보 목록을 가져오는 로직
        List<Trainer> trainers = trainerRepository.findAll();

        List<String> trainerAreas = trainers.stream()
                .map(Trainer::getVisitingAreas)
                .collect(Collectors.toList());

        log.info("Trainer areas: {}", trainerAreas);

        return trainerAreas;
    }


    @Tool(name = "getPostInfo", description = """
    이 도구는 사용자의 요청에 따라 반려동물 관련 게시글을 검색합니다.
    게시글 검색 요청이 있을 경우 이 도구를 사용하세요.
    
    ### 사용 예시
    - "분리불안에 대한 게시글 찾아줘"
    - "배변훈련 관련 사례 찾아줘"
    - "강아지 훈련 경험담 보여줘"
    
    ### 검색 과정
    1. **DB에 저장된 태그 목록을 요청합니다.** 툴 이름: getTagsInDB
    2. 사용자의 질문이나 요청에서 키워드를 추출하세요.
    3. 추출한 키워드를 기반으로 관련 게시글을 검색합니다.
    4. 검색 결과를 사용자에게 제공합니다.
    
    ### 검색 범위
    - 태그: 반려동물 종류, 문제 행동, 훈련 방법 등
    - 키워드: 게시글 제목이나 내용에 포함된 관련 단어
    
    ### 반환 데이터
    - List<PostDTO> 형태의 게시글 목록을 반환합니다.
    - 각 PostDTO에는 다음 정보가 포함됩니다:
     - postId: 게시글 ID
     - title: 게시글 제목
     - content: 게시글 내용
    
    ### 커뮤니케이션 스타일
    - 게시글 정보를 요약하여 명확하게 전달
    - 핵심 내용을 강조하여 사용자가 쉽게 이해할 수 있도록 함
    - 원본 게시글 링크 제공으로 상세 내용 확인 유도
    
    ### 응답 형식
    - 게시글 요약은 마크다운 형식으로 표시 (코드블록 금지, 마크다운으로만 출력)
    - 게시글이 있는 경우 "키워드에 관한 게시글을 찾았습니다."라는 문구로 시작
    - 게시글이 없는 경우 "키워드에 관한 게시글을 찾을 수 없습니다."라는 문구로 시작
    - 게시글 목록이 비어있는 경우 게시글 정보를 임의로 생성하지 마세요
    - 각 게시글은 게시글 템플릿에 따라 표시됩니다
    - 검색된 모든 게시글을 마크다운 형식으로 출력합니다
    - 게시글 사이는 "---"로 구분합니다.
    - 예시: "
              # {태그} 게시글:\\n
              ---
              {게시글1}\\n
              ---
              {게시글2}\\n
              ---
              {게시글3}\\n
              ---
            "
    - 출력되는 형태는 게시글 템플릿을 따르고 게시글 내용은 **3줄 요약**합니다.
    
    이 지침을 정확히 따라 사용자 요청 조건과 검색 결과를 명확하게 설명하세요.
    
    게시글 템플릿:
    """ + postCardTemplate)
    public List<ChatPostDTO> getPostInfo(
            @ToolParam(description = """
           사용자 요청에서 추출한 태그 목록입니다. 다음과 같은 내용을 태그로 추출하세요:
           
           태그 추출 순서:
           1. **DB에 저장된 태그 목록을 요청합니다.**
           2. DB에 있는 태그 목록을 기반으로 사용자가 제공한 정보에 맞는 태그를 추출합니다.
           
           태그 예시:
           1. 반려동물 종류: 강아지, 고양이 등
           2. 문제 행동: 분리불안, 짖음, 공격성, 배변문제 등
           3. 훈련 방법: 기본훈련, 행동교정, 클리커 등
           4. 경험담: 성공사례, 실패사례, 경험, 후기 등
           
           중요한 태그만 추출하고, 불필요한 단어는 포함하지 마세요.
           """)
            List<String> tags
    ) {
        // 입력값 로깅
        log.info("Received tags: {}", tags);

        // 게시글 검색 로직
        List<Post> posts = postRepository.findByTags(tags);

        // posts가 비어있는 경우 처리
        if (posts.isEmpty()) {
            log.warn("No posts found for tags: {}", tags);
            return Collections.emptyList(); // 게시글이 없는 경우 빈 리스트 반환
        }

        // MAX_POSTS 개로 제한
        if (posts.size() > MAX_POSTS) {
            posts = posts.subList(0, MAX_POSTS);
        }

        // 검색 결과를 DTO로 변환
        List<ChatPostDTO> postDTOs = posts.stream()
                .map(this::convertToPostDTO)
                .collect(Collectors.toList());

        log.info("Found {} posts", postDTOs);

        return postDTOs;
    }

    // Post 엔티티를 PostDTO로 변환하는 메서드
    private ChatPostDTO convertToPostDTO(Post post) {
        ChatPostDTO dto = new ChatPostDTO();
        dto.setPostId(post.getPostId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());

        return dto;
    }
}