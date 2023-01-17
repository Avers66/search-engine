package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.introspector.Property;
import searchengine.dto.indexing.IndexingSettings;
import searchengine.model.*;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class Crawler extends RecursiveAction {

    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private IndexingSettings config;
    private String hyperLink;
    private int level;
    private Connection session;
    private SiteEntity se;
    private int delayCrawler;
    private boolean isModeOnePage;
    private Connection connection = null;
    private String responseBody = "";
    private String responseMessage = "";
    private int responseCode = 0;
    private Logger logger;

    public Crawler(String hyperLink, int level, IndexingSettings config) {
        this.hyperLink = hyperLink.trim().toLowerCase().replaceAll("www.","");
        this.level = level;
        this.session = config.getSession();
        this.se = config.getSiteEntity();
        this.delayCrawler = config.getDelay();
        this.pageRepository = config.getPageRepository();
        this.indexRepository = config.getIndexRepository();
        this.lemmaRepository = config.getLemmaRepository();
        this.isModeOnePage = config.isModeOnePage();
        this.config = config;
    }

    @Override
    protected void compute() {
        logger = LogManager.getRootLogger();
        int recursionLevelLimit = 199;
        String domain = se.getUrl();
        List<Crawler> tasksList = new ArrayList<>();
        if (level >= recursionLevelLimit || IndexingService.isCancelled) return;
        if (IndexingService.isLimitPage) return;
        Document doc = parseHyperLink(); // parsing time up to 200000000 ns (70 - 200 ms)
        if ( doc == null) return;
        printDebug(hyperLink); //debug info
        if (!pageRepository.findByPath(hyperLink).isEmpty()) return; // find time up to 4000000 ns (4 ms)
        savePageToRepository(); // save time up to 13000000 ns (13 ms).....new...up to 1000 ms
        if (isModeOnePage) return;
        Elements links = doc.select("a");
        for (Element e : links) {
            String hyperLinkChild = e.attr("href");
            boolean isShortLink = hyperLinkChild.matches("/.+[^#](/|.html)");
            if (isShortLink) hyperLinkChild = domain + hyperLinkChild;
            boolean term = hyperLinkChild.endsWith("/") || hyperLinkChild.endsWith(".html");
            if (hyperLinkChild.indexOf(domain) >= 0 && term) {
                if (!pageRepository.findByPath(hyperLinkChild).isEmpty()) continue;
                try {
                    Thread.sleep(delayCrawler);
                } catch (InterruptedException ex) {System.out.println(ex.getMessage());}
                Crawler task = new Crawler(hyperLinkChild, level + 1, config);
                task.fork();
                tasksList.add(task);
            }
        }
        for (Crawler item : tasksList) {
            item.join();
        }
    }

    public Document  parseHyperLink() {
        connection = session.newRequest().url(hyperLink);
        Connection.Method get = Connection.Method.GET;
        try {
            connection.method(get).execute();
        } catch (Exception ex) {
            logger.error(ex.getMessage() + " Страница недоступна " + hyperLink);
            System.out.println(ex.getMessage() + " Страница недоступна " + hyperLink);
            return null;
        }
        responseBody = connection.response().body();
        responseMessage = connection.response().statusMessage();
        responseCode = connection.response().statusCode();
        Document doc = Jsoup.parse(responseBody);
        String title = doc.title();
        responseBody = responseBody.replaceAll("[^а-яА-ЯёЁ\\s\\-]","");
        responseBody = title + "title " + responseBody;
        return doc;
    }

    public void savePageToRepository() {
        PageEntity pe = new PageEntity();
        pe.setSiteEntity(se);
        pe.setPath(hyperLink);
        pe.setCode(responseCode);
        pe.setContent(responseBody);
        pageRepository.save(pe);
        HashMap<String, Integer> lemmaList = new HashMap<>();
        LemmaMaker lemmaMaker = new LemmaMaker();
        lemmaList = lemmaMaker.makeLemmaList(responseBody); // 150-200 ms
        List<IndexEntity> listIndexEntity = new ArrayList<>();
        for (String lKey : lemmaList.keySet()) {
            List<LemmaEntity> reply;
            LemmaEntity newLemma;
            reply = (lemmaRepository.findByLemmaAndSiteEntity(lKey, se));
            if (reply.size() > 0) {
                newLemma = reply.get(0);
                newLemma.setFrequency(newLemma.getFrequency() + 1);
                lemmaRepository.save(newLemma);
            }else {
                newLemma = new LemmaEntity();
                newLemma.setSiteEntity(se);
                newLemma.setLemma(lKey);
                newLemma.setFrequency(1);
                lemmaRepository.save(newLemma);
            }
            IndexEntity newIndex = new IndexEntity();
            newIndex.setPageEntity(pe);
            newIndex.setLemmaId(newLemma.getId());
            newIndex.setRank(lemmaList.get(lKey));
            listIndexEntity.add(newIndex);

        }
        indexRepository.saveAll(listIndexEntity);

    }

    public void printDebug(String hyperLink) {
        logger.info(pageRepository.count() + " " + hyperLink + " " + responseCode);
        System.out.println(pageRepository.count() + " "
                            + hyperLink + " "
                            + responseCode + " "
                            + responseMessage + " уровень_"
                            + level + " "
                            + responseBody.length() + "_байт"); //debugging
    }
    public SiteEntity getSe() {return se;}
}
