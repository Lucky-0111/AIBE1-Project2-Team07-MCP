package org.lucky0111.pettalkmcpserver.repository.trainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Trainer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, UUID>, CustomTrainerRepository {
    Optional<Trainer> findByUser_Nickname(String nickname);
}

interface CustomTrainerRepository {
    List<Trainer> findAllByTags(List<String> tags);
    List<Trainer> findAllByAreas(List<String> areas);
    List<Trainer> findAllByTagsAndAreas(List<String> tags, List<String> areas);
}

@Repository
class CustomTrainerRepositoryImpl implements CustomTrainerRepository {
    @PersistenceContext
    private EntityManager entityManager;

    // 행정구역 단위 리스트
    private static final List<String> ADMINISTRATIVE_UNITS = Arrays.asList(
            "시", "도", "군", "구", "읍", "면", "동", "리"
    );

    /**
     * 태그 검색을 위한 문자열 처리
     * 입력된 태그 문자열을 각 문자로 분리하여 검색 조건 생성
     */
    private String createTagCondition(String tag, int paramIndex) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }

        // 공백 제거 및 소문자 변환
        String normalizedTag = tag.trim();

        // 태그에 포함된 각 문자를 추출
        StringBuilder condition = new StringBuilder("(");
        for (int i = 0; i < normalizedTag.length(); i++) {
            char c = normalizedTag.charAt(i);
            // 공백이나 특수문자는 건너뜀
            if (Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) {
                continue;
            }

            if (i > 0) {
                condition.append(" AND ");
            }
            condition.append("tag.tag_name LIKE ?").append(paramIndex++);
        }
        condition.append(")");

        return condition.toString();
    }

    /**
     * 지역 검색을 위한 문자열 처리
     * 행정구역 단위(시, 도, 군, 구 등)를 제거하여 검색
     */
    private String processAreaName(String area) {
        if (area == null || area.isEmpty()) {
            return "";
        }

        String processedArea = area.trim();

        // 행정구역 단위 제거
        for (String unit : ADMINISTRATIVE_UNITS) {
            if (processedArea.endsWith(unit)) {
                processedArea = processedArea.substring(0, processedArea.length() - unit.length());
                break;  // 하나의 단위만 제거
            }
        }

        return processedArea;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Trainer> findAllByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return entityManager.createQuery("SELECT t FROM Trainer t").getResultList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.* FROM trainers t ");
        sql.append("JOIN trainer_tags tt ON tt.trainer_id = t.trainer_id ");
        sql.append("JOIN tags tag ON tag.tag_id = tt.tag_id ");
        sql.append("WHERE ");

        List<String> conditions = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        // 각 태그에 대한 조건과 파라미터 생성
        for (String tag : tags) {
            StringBuilder tagCondition = new StringBuilder("(");

            // 태그의 각 문자에 대해 LIKE 조건 추가 (AND로 연결)
            String normalized = tag.trim();
            List<String> charParams = new ArrayList<>();

            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                // 공백이나 특수문자는 건너뜀
                if (Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) {
                    continue;
                }
                charParams.add("%" + c + "%");
            }

            if (!charParams.isEmpty()) {
                for (int i = 0; i < charParams.size(); i++) {
                    if (i > 0) {
                        tagCondition.append(" AND ");
                    }
                    tagCondition.append("tag.tag_name LIKE ?");
                    parameters.add(charParams.get(i));
                }
                tagCondition.append(")");
                conditions.add(tagCondition.toString());
            }
        }

        // 조건이 없는 경우 처리
        if (conditions.isEmpty()) {
            return findAll();
        }

        // OR로 각 태그 조건 연결
        sql.append(String.join(" OR ", conditions));

        // 쿼리 준비 및 파라미터 바인딩
        var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Trainer> findAllByAreas(List<String> areas) {
        if (areas == null || areas.isEmpty()) {
            return entityManager.createQuery("SELECT t FROM Trainer t").getResultList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.* FROM trainers t WHERE ");

        List<String> conditions = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        // 각 지역에 대한 조건과 파라미터 생성 (행정구역 단위 제거)
        for (String area : areas) {
            String processedArea = processAreaName(area);
            if (!processedArea.isEmpty()) {
                conditions.add("t.visiting_areas LIKE ?");
                parameters.add("%" + processedArea + "%");
            }
        }

        // 조건이 없는 경우 처리
        if (conditions.isEmpty()) {
            return findAll();
        }

        // OR로 각 지역 조건 연결
        sql.append(String.join(" OR ", conditions));

        // 쿼리 준비 및 파라미터 바인딩
        var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Trainer> findAllByTagsAndAreas(List<String> tags, List<String> areas) {
        if (tags == null || tags.isEmpty()) {
            return findAllByAreas(areas);
        }

        if (areas == null || areas.isEmpty()) {
            return findAllByTags(tags);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.* FROM trainers t ");
        sql.append("JOIN trainer_tags tt ON tt.trainer_id = t.trainer_id ");
        sql.append("JOIN tags tag ON tag.tag_id = tt.tag_id ");
        sql.append("WHERE (");

        List<String> tagConditions = new ArrayList<>();
        List<String> areaConditions = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        // 각 태그에 대한 조건과 파라미터 생성
        for (String tag : tags) {
            StringBuilder tagCondition = new StringBuilder("(");

            // 태그의 각 문자에 대해 LIKE 조건 추가 (AND로 연결)
            String normalized = tag.trim();
            List<String> charParams = new ArrayList<>();

            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                // 공백이나 특수문자는 건너뜀
                if (Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) {
                    continue;
                }
                charParams.add("%" + c + "%");
            }

            if (!charParams.isEmpty()) {
                for (int i = 0; i < charParams.size(); i++) {
                    if (i > 0) {
                        tagCondition.append(" AND ");
                    }
                    tagCondition.append("tag.tag_name LIKE ?");
                    parameters.add(charParams.get(i));
                }
                tagCondition.append(")");
                tagConditions.add(tagCondition.toString());
            }
        }

        // 각 지역에 대한 조건과 파라미터 생성 (행정구역 단위 제거)
        for (String area : areas) {
            String processedArea = processAreaName(area);
            if (!processedArea.isEmpty()) {
                areaConditions.add("t.visiting_areas LIKE ?");
                parameters.add("%" + processedArea + "%");
            }
        }

        // 조건이 없는 경우 처리
        if (tagConditions.isEmpty() && areaConditions.isEmpty()) {
            return findAll();
        } else if (tagConditions.isEmpty()) {
            return findAllByAreas(areas);
        } else if (areaConditions.isEmpty()) {
            return findAllByTags(tags);
        }

        // OR로 각 태그 조건 연결
        sql.append(String.join(" OR ", tagConditions));
        sql.append(") AND (");

        // OR로 각 지역 조건 연결
        sql.append(String.join(" OR ", areaConditions));
        sql.append(")");

        // 쿼리 준비 및 파라미터 바인딩
        var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getResultList();
    }

    // 모든 훈련사 조회 헬퍼 메서드
    @SuppressWarnings("unchecked")
    private List<Trainer> findAll() {
        return entityManager.createQuery("SELECT t FROM Trainer t").getResultList();
    }
}