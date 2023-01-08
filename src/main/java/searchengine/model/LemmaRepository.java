package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    List<LemmaEntity> findByLemmaAndSiteEntity(String lemma, SiteEntity se);
    List<LemmaEntity> findBySiteEntity(SiteEntity se);
    int countBySiteEntity(SiteEntity se);
}
