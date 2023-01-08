package searchengine.dto.search;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchError {
    private boolean result = false;
    private String error = "Задан пустой поисковый запрос";

    public SearchError(){}

    public SearchError(String error) {
        this.error = error;
    }
}
