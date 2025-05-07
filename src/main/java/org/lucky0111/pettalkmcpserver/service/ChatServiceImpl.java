package org.lucky0111.pettalkmcpserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lucky0111.pettalkmcpserver.domain.entity.community.Post;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.TrainerTagRelation;
import org.lucky0111.pettalkmcpserver.domain.entity.user.PetUser;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.lucky0111.pettalkmcpserver.repository.community.PostRepository;
import org.lucky0111.pettalkmcpserver.repository.community.PostTagRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerRepository;
import org.lucky0111.pettalkmcpserver.repository.trainer.TrainerTagRepository;
import org.lucky0111.pettalkmcpserver.repository.user.PetUserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final PostRepository postRepository;
    private final TrainerTagRepository trainerTagRepository;
    private final TrainerRepository trainerRepository;

    //  예시
//    @Tool(name = "removeUserById", description = "사용자 ID를 기준으로 사용자를 삭제합니다. 특정 ID의 사용자를 시스템에서 완전히 제거해야 할 때 사용하세요. 이 작업은 되돌릴 수 없으니 신중하게 사용해야 합니다. 예: '아이디가 abc123인 사용자 삭제해줘', '특정 ID를 가진 계정을 제거해줘', '시스템에서 이 사용자를 완전히 제거해줘'")
//    @Transactional
//    public void deleteUserById(
//            @ToolParam(description = "삭제할 사용자의 고유 ID. 시스템이 자동으로 생성한 UUID 형식의 문자열입니다. 예: '550e8400-e29b-41d4-a716-446655440000'")
//            String id) {
//        userRepository.deleteById(id);
//    }
//    @Tool(name = "removeUserByUsername", description = "사용자 이름을 기준으로 사용자를 삭제합니다. 사용자 ID를 모르지만 이름은 알고 있을 때 계정을 제거하려는 경우 사용하세요. 이 작업은 되돌릴 수 없으니 신중하게 사용해야 합니다. 예: '홍길동 사용자 계정 삭제해줘', '특정 사용자 이름으로 등록된 계정 제거해줘', '이 사용자 이름을 가진 계정을 시스템에서 지워줘'")
//    @Transactional
//    public void deleteUserByUsername(
//            @ToolParam(description = "삭제할 사용자의 이름. 사용자가 회원가입 시 입력한 고유한 아이디입니다. 대소문자를 구분합니다. 예: 'hong123', 'admin_user'")
//            String username) {
//        userRepository.deleteByUsername(username);
//    }


    // 사용자의 질문과 관련된 내용이 태그에 있을 경우 해당 태그를 포함한 트레이너와 게시글을 검색합니다.
    @Tool(name = "getRelatedPosts", description = "사용자의 질문과 관련된 게시글을 검색합니다. 사용자가 질문한 내용과 관련된 태그를 기반으로 게시글을 찾습니다. 예: '강아지 훈련 방법에 대한 질문이 있어요', '고양이 행동 문제에 대한 조언을 받고 싶어요', '강아지 배변 훈련에 대한 팁을 알고 싶어요', '고양이 사료 추천해줘', '분리불안 해결 방법 알려줘', '공격성 제어 방법은?', '강아지 훈련사 추천해줘', '고양이 행동 문제 해결 방법 알려줘', '강아지 훈련 팁과 요령 공유해줘', '고양이 사료 브랜드 추천해줘'")
    public List<Long> getRelatedPosts(
            @ToolParam(description = "사용자가 질문한 내용에 대한 태그 리스트. 범위가 클 경우 여러 개의 태그 입력. 예: '강아지 훈련', '고양이 행동 문제', '강아지 배변 훈련', '고양이 사료', '분리불안', '공격성 제어', '강아지 훈련사 추천', '고양이 행동 문제 해결', '강아지 훈련 팁과 요령', '고양이 사료 브랜드'")
            List<String> tags) {
        log.info("getRelatedTrainers 호출됨: tags={}", tags);
        // 쿼리 실행
        List<Long> results = null;
        try {
            results = postTagRepository.findPostIdsByTagNames(tags);
            log.debug("쿼리 실행 완료, 결과 처리 전: {}", results);
        } catch (Exception e) {
            log.error("쿼리 실행 중 오류: ", e);
            return List.of();
        }

        if (results == null || results.isEmpty()) {
            log.warn("검색 결과 없음: tags={}", tags);
            return List.of(); // 빈 리스트 반환하여 NPE 방지
        }

        return results;
    }

    @Tool(name = "getRelatedTrainers", description = "사용자의 질문과 관련된 트레이너를 검색합니다. 사용자가 질문한 내용과 관련된 태그를 기반으로 트레이너를 찾습니다.  예: '강아지 훈련 방법에 대한 질문이 있어요', '고양이 행동 문제에 대한 조언을 받고 싶어요', '강아지 배변 훈련에 대한 팁을 알고 싶어요', '고양이 사료 추천해줘', '분리불안 해결 방법 알려줘', '공격성 제어 방법은?', '강아지 훈련사 추천해줘', '고양이 행동 문제 해결 방법 알려줘', '강아지 훈련 팁과 요령 공유해줘', '고양이 사료 브랜드 추천해줘'")
    public List<UUID> getRelatedTrainers(
            @ToolParam(description = "사용자가 질문한 내용에 대한 태그 리스트. 범위가 클 경우 여러 개의 태그 입력. 예: '강아지 훈련', '고양이 행동 문제', '강아지 배변 훈련', '고양이 사료', '분리불안', '공격성 제어', '강아지 훈련사 추천', '고양이 행동 문제 해결', '강아지 훈련 팁과 요령', '고양이 사료 브랜드'")
            List<String> tags) {
        log.info("getRelatedTrainers 호출됨: tags={}", tags);

        // 쿼리 실행
        List<UUID> results = null;
        try {
            results = trainerTagRepository.findTrainerIdsByTagNames(tags);
            log.debug("쿼리 실행 완료, 결과 처리 전: {}", results);
        } catch (Exception e) {
            log.error("쿼리 실행 중 오류: ", e);
            return List.of();
        }

        if (results == null || results.isEmpty()) {
            log.warn("검색 결과 없음: tags={}", tags);
            return List.of(); // 빈 리스트 반환하여 NPE 방지
        }

        return results;
    }


    
}