package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import searchengine.model.*;

@Getter
@Setter
public class IndexingSettings {
    SiteRepository siteRepository;
    PageRepository pageRepository;
    LemmaRepository lemmaRepository;
    IndexRepository indexRepository;
    SiteEntity siteEntity;
    Connection session;
    int delay;
    boolean isModeOnePage = false;
}
