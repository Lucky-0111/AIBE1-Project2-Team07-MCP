package org.lucky0111.pettalkmcpserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lucky0111.pettalkmcpserver.domain.entity.common.Tag;
import org.lucky0111.pettalkmcpserver.repository.common.TagRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Tool(
    name = "getTagsInDB",
    description = """
    이 도구는 DB에 저장된 모든 태그 목록을 가져옵니다.
    **DB에 저장된 태그 목록을 요청할 때 사용하세요.**
    
    ### 사용 예시
    - "태그를 생성하기 위해 DB에 있는 태그 목록을 가져와주세요"
    
    ### 용도
    - 태그 선택 시 DB에 있는 태그를 우선적으로 사용하기 위한 참조 목록
    - 중복 태그 생성 방지 및 태그 일관성 유지
    
    ### 반환 데이터
    - List<String> 형태의 모든 태그 이름 목록을 반환합니다
    - 각 문자열은 DB에 저장된 정확한 태그 이름입니다
    
    ### 사용 방법
    1. 사용자의 질문이나 요청에서 키워드로 태그를 파악하세요
    2. 이 도구를 호출하여 DB에 있는 태그 목록을 확인하세요
    3. 사용자 요청의 태그와 DB의 태그를 비교하여 일치하는 지역을 선택하세요
    4. 선택된 태그를 기반으로 훈련사 검색 또는 게시글 검색 또는 다른 작업을 수행하세요
    
    ### 응답 형식
    - 쉼표로 구분된 태그 목록만 반환하세요 (예: 태그1,태그2,태그3)
    - 코드 블록, JSON 형식, 마크다운 구문 등은 사용하지 마세요

    """
    )
    public List<String> getTagsInDB(
    ) {
        List<String> tagList = tagRepository.findAll()
                .stream()
                .map(Tag::getTagName)
                .collect(Collectors.toList());

        log.info("Tag List: {}", tagList);

        return tagList;
    }

}