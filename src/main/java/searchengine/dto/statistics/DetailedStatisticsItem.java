package searchengine.dto.statistics;

import lombok.Data;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status = "нд";
    private long statusTime = 0;
    private String error = null;
    private int pages = 0;
    private int lemmas = 0;
}
