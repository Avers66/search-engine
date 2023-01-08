package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    List<PageEntity> findByPath(String path);
    List<PageEntity> findBySiteEntity(SiteEntity se);
    int countBySiteEntity(SiteEntity se);

}
