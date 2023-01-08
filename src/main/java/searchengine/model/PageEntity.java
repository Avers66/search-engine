package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "page")
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;
    //private int siteId;

    //@Column(columnDefinition = "TEXT NOT NULL, FULLTEXT KEY idx_path (path)")
    //@Column(columnDefinition = "TEXT NOT NULL, INDEX idx_path (path(1000))")
    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY (path(200))")
    private String path;
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    public PageEntity() {}
}
