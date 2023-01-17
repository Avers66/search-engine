package searchengine.services;
import net.bytebuddy.matcher.StringMatcher;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LemmaMaker {
    LuceneMorphology luceneMorph;
    String regex = "[А-Яа-яЁё]{2,20}";
    Pattern pattern = Pattern.compile(regex);
    List<String> wordBuffer;
    public HashMap<String, Integer> makeLemmaList(String text) {
        HashMap<String, Integer> lemmaList = new HashMap<>();
        try {
            luceneMorph = new RussianLuceneMorphology();
            Matcher matcher = pattern.matcher(text);
            String word;
            while (matcher.find()) {
                word = matcher.group();
                word = (word.toLowerCase());
                word = word.replaceAll("[ё]", "е");
                //if (!luceneMorph.checkString(word)) continue;
                wordBuffer = luceneMorph.getMorphInfo(word);
                if (wordBuffer.get(0).matches(".+(СОЮЗ|ПРЕДЛ|ЧАСТ|МЕЖД)")) continue;
                wordBuffer = luceneMorph.getNormalForms(word);
                for (String s : wordBuffer) {
                    lemmaList.put(s, lemmaList.getOrDefault(s, 0) + 1);
                }
            }
        } catch (IOException ex) {
            ex.getMessage();
        }
        //System.out.println(lemmaList);
        return lemmaList;
    }

    public List<Integer> getPositionWord(String content, String wordNormal) {
        List<Integer> position = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        try {
            luceneMorph = new RussianLuceneMorphology();
            String word;
            while (matcher.find()) {
                word = content.substring(matcher.start(), matcher.end());
                word = (word.toLowerCase());
                word = word.replaceAll("[ё]", "е");
                wordBuffer = luceneMorph.getNormalForms(word);
                for (String s : wordBuffer) {
                    if (s.matches(wordNormal)) {
                        position.add(matcher.start());
                        position.add(matcher.end());
                        return position;
                    };
                }
            }
        } catch (IOException ex) {ex.getMessage();}
        return position;
    }
}
