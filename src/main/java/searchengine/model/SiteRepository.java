package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    List<SiteEntity> findByName(String name);
    List<SiteEntity> findByUrl(String url);
    SiteEntity getByName(String name);
}
