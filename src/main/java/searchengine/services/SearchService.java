package searchengine.services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchError;
import searchengine.dto.search.SearchPage;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    LemmaMaker lemmaMaker = new LemmaMaker();

    public SearchService(){}

    public ResponseEntity<?> searchResponse(String query, String site, int offset, int limit){
        if (query == "") return ResponseEntity.ok(new SearchError());
        Logger logger = LogManager.getRootLogger();
        logger.info("Запуск поискового запроса: " + query);
        SearchResponse searchResponse = new SearchResponse();
        List<SearchPage> listPage = new ArrayList<>();
        List<SiteEntity> listSite = new ArrayList<>();
        Set<String> searchLemmaList = lemmaMaker.makeLemmaList(query).keySet();
        if (site.matches("all")) listSite = siteRepository.findAll();
        else listSite = siteRepository.findByUrl(site.replaceAll("www.", ""));
        if (listSite.isEmpty()) {
            logger.error("Отсутствуют индексированные сайты");
            return ResponseEntity.ok(new SearchError("Отсутствуют индексированные сайты"));
        }
        for (SiteEntity se : listSite) {
            if (se.getStatus() == StatusType.INDEXING || se.getStatus() == StatusType.FAILED) {
                logger.error("Индексация " + se.getName() + " не закончена");
                return ResponseEntity.ok(new SearchError("Индексация " + se.getName() + " не закончена"));
            }
            int limitLemmaFrequency = pageRepository.countBySiteEntity(se)/5;
            List<LemmaEntity> listLE = new ArrayList<>();
            for (String lemma : searchLemmaList) {
                listLE.addAll(lemmaRepository.findByLemmaAndSiteEntity(lemma, se));
            }
            listLE = listLE.stream().filter(l -> (l.getFrequency() < limitLemmaFrequency))
                                    .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                                    .collect(Collectors.toList());
            addResponseListPage(listLE, listPage);
        }
        listPage = listPage.stream()
                           .sorted(Comparator.comparing(SearchPage::getRelevance).reversed())
                           .collect(Collectors.toList());
        searchResponse.setData(splittingIntoPages(listPage, offset, limit));
        searchResponse.setCount(listPage.size());
        return ResponseEntity.ok(searchResponse);
    }

    public List<SearchPage> splittingIntoPages(List<SearchPage> listPage, int offset, int limit){
        List<SearchPage> listPageable = new ArrayList<>();
        for (int i = offset; i < offset + limit; i++) {
            if (i == listPage.size()) break;
            listPageable.add(listPage.get(i));
        }
        return listPageable;
    }

    public void addResponseListPage(List<LemmaEntity> listLE, List<SearchPage> listPage){
        if (listLE.isEmpty()) return;
        List<IndexEntity> listIE1 = indexRepository.findByLemmaId(listLE.get(0).getId());
        for (int i = 1; i < listLE.size(); i++){
            Iterator<IndexEntity> iteratorIE1 = listIE1.iterator();
            List<IndexEntity> listI = indexRepository.findByLemmaId(listLE.get(i).getId()); //500 ms
            while (iteratorIE1.hasNext()) {
                IndexEntity ie1 = iteratorIE1.next();
                boolean pageExist = false;
                for (IndexEntity ieI : listI) {
                    if (ieI.getPageEntity().getId() == ie1.getPageEntity().getId()) {
                        pageExist = true;
                        ie1.setRank(ie1.getRank() + ieI.getRank());
                        break;
                    }
                }
                if (!pageExist) iteratorIE1.remove();
            }
        }
        calculateRelevance(listIE1);
        for (IndexEntity ie : listIE1) {
            SearchPage searchPage = createSearchPage(ie, listLE);
            listPage.add(searchPage);
        }
    }

    public List<IndexEntity> calculateRelevance(List<IndexEntity> listIE1) {
        float maxAbsoluteRelevance = 0;
        for (IndexEntity ie : listIE1) {
            if (ie.getRank() > maxAbsoluteRelevance) maxAbsoluteRelevance = ie.getRank();
        }
        for (IndexEntity ie : listIE1) {
            ie.setRank(ie.getRank()/maxAbsoluteRelevance);
        }
        return listIE1;
    }

    public SearchPage createSearchPage(IndexEntity ie, List<LemmaEntity> listLE) {
        SearchPage searchPage = new SearchPage();
        searchPage.setSite(ie.getPageEntity().getSiteEntity().getUrl());
        searchPage.setUri(ie.getPageEntity().getPath()
                .replaceAll(ie.getPageEntity().getSiteEntity().getUrl(), ""));
        String content = ie.getPageEntity().getContent();
        String title = content.substring(0,content.indexOf("title"));
        searchPage.setTitle(title);
        searchPage.setSiteName(ie.getPageEntity().getSiteEntity().getName());
        int lengthOnePartSnippet = 210/listLE.size();
        String snippet = "";
        for (LemmaEntity le : listLE) {
            List<Integer> position = lemmaMaker.getPositionWord(content, le.getLemma());
            if (position.isEmpty()) continue;
            int start1 = position.get(0) > lengthOnePartSnippet/2 ? position.get(0) - lengthOnePartSnippet/2 : 0;
            int end1 = position.get(0) - 1;
            int start2 = position.get(0);
            int end2 = position.get(1);
            int start3 = position.get(1) + 1;
            int end3 = end2 + lengthOnePartSnippet/2 > content.length() ? content.length() - 1 : end2 + lengthOnePartSnippet/2 -1;
            snippet = snippet + " " + content.substring(start1, end1) + " <b>" + content.substring(start2, end2) + "</b> "
                    + content.substring(start3, end3);
        }
        searchPage.setSnippet(snippet);
        searchPage.setRelevance(ie.getRank());
        return searchPage;
    }
}
