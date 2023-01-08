package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Data
@Entity
@Table(name = "`index`")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false, name = "page_id")
    private PageEntity pageEntity;


    //@Column(nullable = false, name = "lemma_id")
    @Column(columnDefinition = "bigint NOT NULL, INDEX idx_lemma (lemma_id)", name = "lemma_id")
    private  long lemmaId;

    @Column(nullable = false, name = "`rank`")
    private float rank;

    public IndexEntity() {}

}
