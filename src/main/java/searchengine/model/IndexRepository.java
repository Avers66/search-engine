package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    List<IndexEntity> findByPageEntity(PageEntity pe);
    List<IndexEntity> findByLemmaId(long id);
    //void deleteByPageEntity(PageEntity pageEntity);
}
