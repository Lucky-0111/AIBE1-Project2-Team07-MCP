package org.lucky0111.pettalkmcpserver.repository.trainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.lucky0111.pettalkmcpserver.domain.entity.trainer.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, UUID>, CustomTrainerRepository {
    Optional<Trainer> findByUser_Nickname(String nickname);

    /**
     * 태그 목록만 충족하는 훈련사 목록 조회
     */
    @Query(nativeQuery = true,
            value = "SELECT DISTINCT t.* FROM trainers t " +
                    "JOIN trainer_tags tt ON tt.trainer_id = t.trainer_id " +
                    "JOIN tags tag ON tag.tag_id = tt.tag_id " +
                    "WHERE tag.tag_name IN (:tags)")
    List<Trainer> findAllByTags(@Param("tags") List<String> tags);
}

interface CustomTrainerRepository {
    List<Trainer> findAllByAreas(List<String> areas);
    List<Trainer> findAllByTagsAndAreas(List<String> tags, List<String> areas);
}

@Repository
class CustomTrainerRepositoryImpl implements CustomTrainerRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Trainer> findAllByAreas(List<String> areas) {
        if (areas == null || areas.isEmpty()) {
            return entityManager.createQuery("SELECT t FROM Trainer t").getResultList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.* FROM trainers t WHERE ");

        // 각 지역에 대해 LIKE 조건 추가
        for (int i = 0; i < areas.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("t.visiting_areas LIKE ?").append(i + 1);
        }

        var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

        // 파라미터 바인딩
        for (int i = 0; i < areas.size(); i++) {
            query.setParameter(i + 1, "%" + areas.get(i) + "%");
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
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT t.* FROM trainers t ");
            sql.append("JOIN trainer_tags tt ON tt.trainer_id = t.trainer_id ");
            sql.append("JOIN tags tag ON tag.tag_id = tt.tag_id ");
            sql.append("WHERE tag.tag_name IN (");

            // 태그 플레이스홀더 추가
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?").append(i + 1);
            }
            sql.append(")");

            var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

            // 태그 파라미터 바인딩
            for (int i = 0; i < tags.size(); i++) {
                query.setParameter(i + 1, tags.get(i));
            }

            return query.getResultList();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.* FROM trainers t ");
        sql.append("JOIN trainer_tags tt ON tt.trainer_id = t.trainer_id ");
        sql.append("JOIN tags tag ON tag.tag_id = tt.tag_id ");
        sql.append("WHERE tag.tag_name IN (");

        // 태그 플레이스홀더 추가
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?").append(i + 1);
        }
        sql.append(") AND (");

        // 지역 LIKE 조건 추가
        for (int i = 0; i < areas.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("t.visiting_areas LIKE ?").append(tags.size() + i + 1);
        }
        sql.append(")");

        var query = entityManager.createNativeQuery(sql.toString(), Trainer.class);

        // 태그 파라미터 바인딩
        for (int i = 0; i < tags.size(); i++) {
            query.setParameter(i + 1, tags.get(i));
        }

        // 지역 파라미터 바인딩
        for (int i = 0; i < areas.size(); i++) {
            query.setParameter(tags.size() + i + 1, "%" + areas.get(i) + "%");
        }

        return query.getResultList();
    }
}