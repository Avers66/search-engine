package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "lemma")
public class LemmaEntity  {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @OnDelete(action = OnDeleteAction.CASCADE)
        @JoinColumn(nullable = false, name = "site_id")
        private SiteEntity siteEntity;

        //@Column(nullable = false)
        @Column(columnDefinition = "varchar(255) NOT NULL, UNIQUE KEY (lemma(20), site_id)")
        private String lemma;

        @Column(nullable = false)
        private int frequency;

        public LemmaEntity() {}
}
