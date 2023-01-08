package searchengine.dto.search;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponse {
    private boolean result = true;
    private int count = 0;
    private List<SearchPage> data = new ArrayList<>();



}
