package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchPage {
    private String site = "site";
    private String siteName = "siteName";
    private String uri = "uri";
    private String title = "title";
    private String snippet = "snippet";
    private float relevance = 0;
}
