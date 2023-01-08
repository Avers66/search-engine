package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingError;
import searchengine.dto.indexing.IndexingOk;
import searchengine.dto.indexing.IndexingSettings;
import searchengine.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
//@RequiredArgsConstructor
public class IndexingService implements Runnable {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    private final SitesList sites;
    private List<Crawler> listCrawler;
    public static ForkJoinPool pool = new ForkJoinPool();

    private boolean isIndexing = false;
    private final int level = 0;
    private final int limit;
    private final int delay;
    private final String agent;
    private final String referrer;
    private Connection session;
    public static boolean isLimitPage = false;
    public static boolean isCancelled = false;
    private boolean isCrawl = false;




    public IndexingService(SitesList sites) {
        this.sites = sites;
        limit = sites.getPageNumberLimit();
        delay = sites.getDelayCrawler();
        agent = sites.getUserAgent();
        referrer =  sites.getReferrer();
        session = Jsoup.newSession().timeout(3000).userAgent(agent).referrer(referrer);
    }
    public ResponseEntity<?> indexingStart()  {
        if (isIndexing){
            return ResponseEntity.ok(new IndexingError(false, "Индексация уже запущена"));
        }
        listCrawler = new ArrayList<>();
        isCancelled = false;
        isLimitPage = false;
        isIndexing = true;
        //siteRepository.deleteAll();
        for (Site site : sites.getSites()){
            deleteFromTableSite(site);
            SiteEntity se = initTableSite(site);
            IndexingSettings config = indexingConfig(se);
            String rootSite = site.getUrl();
            Crawler crawler = new Crawler(rootSite, level, config);
            listCrawler.add(crawler);
            pool.execute(crawler);
        }
        return ResponseEntity.ok(new IndexingOk(true));
    }

    public ResponseEntity<?> indexingStop() {
        if (!isIndexing){
            return ResponseEntity.ok(new IndexingError(false, "Индексация не запущена"));
        }
        isCancelled = true;  //pool.shutdown();
        statusMonitor();
        return ResponseEntity.ok(new IndexingOk(true));
    }

    @Override
    public void run() {
        Long start = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            statusMonitor();
        } while  (isIndexing);
        System.out.println("Время выполнения ******* " + (System.currentTimeMillis() - start));

    }

    public void deleteFromTableSite(Site site) {
        List<SiteEntity> listSE = siteRepository.findByName(site.getName());
        for (SiteEntity se : listSE) {
            siteRepository.delete(se);
        }

    }
    public SiteEntity initTableSite(Site site) {
        SiteEntity se = new SiteEntity();
        se.setName(site.getName());
        se.setUrl(site.getUrl().trim().toLowerCase().replaceAll("www.", ""));
        se.setLastError(null);
        se.setStatus(StatusType.INDEXING);
        se.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(se);
        return se;
    }

    public IndexingSettings indexingConfig(SiteEntity se) {
        IndexingSettings config = new IndexingSettings();
        config.setDelay(delay);
        config.setIndexRepository(indexRepository);
        config.setLemmaRepository(lemmaRepository);
        config.setPageRepository(pageRepository);
        config.setSiteEntity(se);
        config.setSession(session);
        return config;
    }

    public void setStatus(SiteEntity se, StatusType type)  {
        se.setStatus(type);
        if (type == StatusType.FAILED) se.setLastError("«Индексация остановлена пользователем»");
        siteRepository.saveAndFlush(se);
    }

    public void setTimeStamp(SiteEntity se) {
        se.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(se);
    }

    public void statusMonitor() {
        boolean isDone = true;
        //isLimitPage = (listPath.size() > limit);
        isLimitPage = (pageRepository.count() > limit);
        for (Crawler crawler : listCrawler) {
            isDone = isDone && crawler.isDone();
            if (crawler.isDone() && !isCancelled) setStatus(crawler.getSe(),StatusType.INDEXED);
            else setTimeStamp(crawler.getSe());
            if (isCancelled && !crawler.isDone()) setStatus(crawler.getSe(),StatusType.FAILED);
        }
        isIndexing = !isDone;
    }

    public ResponseEntity<?> indexPage(String page) {
        String error = "Данная страница находится за пределами сайтов," +
                       " указанных в конфигурационном файле";
        Site siteFromConfig = null;
        page = page.toLowerCase().replaceAll("www.", "").trim();
        for (Site site : sites.getSites()){
            String siteURL = site.getUrl().toLowerCase().replaceAll("www.", "");
            if (page.indexOf(siteURL) == 0) siteFromConfig = site;
        }
        if (siteFromConfig == null) return ResponseEntity.ok(new IndexingError(false, error));
        SiteEntity se;
        List<SiteEntity> listSE = siteRepository.findByName(siteFromConfig.getName());
        if (listSE.isEmpty()) se = initTableSite(siteFromConfig);
        else {
            se = listSE.get(0);
            List<PageEntity> listPE = pageRepository.findByPath(page);
            if (listPE.size() > 0) deletePage(listPE.get(0));
        }
        IndexingSettings config = indexingConfig(se);
        config.setModeOnePage(true);
        Crawler crawler = new Crawler(page, level, config);
        pool.execute(crawler);
        setStatus(se, StatusType.INDEXED);
        return ResponseEntity.ok(new IndexingOk(true));
    }

    public void deletePage(PageEntity pe) {
        List<IndexEntity> listIE = indexRepository.findByPageEntity(pe);
        for (IndexEntity ie : listIE) {
            LemmaEntity le = lemmaRepository.getReferenceById(ie.getLemmaId());
            if (le.getFrequency() > 1) {
                le.setFrequency(le.getFrequency()-1);
                lemmaRepository.save(le);
            }else lemmaRepository.delete(le);
            //indexRepository.delete(ie);
        }
        Iterator<IndexEntity> ieIterator = listIE.iterator();
        while (ieIterator.hasNext()) {
            indexRepository.delete(ieIterator.next());
        }
        //indexRepository.deleteByPageEntity(pe);
        pageRepository.delete(pe);
    }
}


